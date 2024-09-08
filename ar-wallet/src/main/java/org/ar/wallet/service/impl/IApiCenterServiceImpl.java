package org.ar.wallet.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSONObject;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.result.ApiResponse;
import org.ar.common.core.result.ApiResponseEnum;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.UserAgentUtil;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.MemberInfoMapper;
import org.ar.wallet.mapper.MerchantCollectOrdersMapper;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.rabbitmq.RabbitMQService;
import org.ar.wallet.req.*;
import org.ar.wallet.service.*;
import org.ar.wallet.util.*;
import org.ar.wallet.vo.*;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IApiCenterServiceImpl implements IApiCenterService {

    private final IMerchantInfoService merchantInfoService;
    private final IMemberInfoService memberInfoService;
    private final ITradeConfigService tradeConfigService;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;
    private final ArProperty arProperty;
    private final RedisTemplate redisTemplate;
    private final MemberInfoMapper memberInfoMapper;
    private final RabbitMQService rabbitMQService;
    private final IMerchantCollectOrdersService merchantCollectOrdersService;
    private final IMerchantPaymentOrdersService merchantPaymentOrdersService;
    private final IMemberAccountChangeService memberAccountChangeService;
    private final MerchantCollectOrdersMapper merchantCollectOrdersMapper;
    private final RedissonUtil redissonUtil;
    private final AmountChangeUtil amountChangeUtil;
    private final OrderNumberGeneratorUtil orderNumberGenerator;
    private final ICashBackOrderService cashBackOrderService;


    /**
     * 商户 获取钱包会员信息
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public ApiResponse getWalletMemberInfo(ApiRequest apiRequest, HttpServletRequest request) {

        //获取请求IP
        String requestIp = IpUtil.getRealIP(request);
        log.info("获取钱包会员信息接口: 商户号: {}, 请求IP: {}", apiRequest.getMerchantCode(), requestIp);

        //获取商户信息
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

        String merchantPublicKeyStr = null;
        //获取商户公钥
        if (merchantInfo != null) {
            merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();
        }

        //校验请求
        ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "获取钱包会员信息接口");
        if (apiResponse != null) {
            return apiResponse;
        }

        try {

            //商户公钥
            PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

            //平台私钥
            PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

            //使用平台私钥解密数据
            WalletMemberInfoReq walletMemberInfoReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, WalletMemberInfoReq.class);

            if (walletMemberInfoReq == null) {
                log.error("获取钱包会员信息接口失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
            }
            //手动调用验证明文参数
            Set<ConstraintViolation<WalletMemberInfoReq>> violations = validator.validate(walletMemberInfoReq);
            if (!violations.isEmpty()) {
                // 处理验证错误
                for (ConstraintViolation<WalletMemberInfoReq> violation : violations) {
                    log.error("获取钱包会员信息接口失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, walletMemberInfoReq);
                    System.out.println(violation.getMessage());
                    return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                }
            }

            //使用商户公钥验证签名
            if (!RsaUtil.verifySignature(walletMemberInfoReq, walletMemberInfoReq.getSign(), merchantPublicKey)) {
                log.error("获取钱包会员信息接口失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, walletMemberInfoReq);
                return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
            }

            //处理业务
            //查询会员信息
            MemberInfo memberInfo = memberInfoService.getMemberInfoByMemberId(walletMemberInfoReq.getMerchantCode() + walletMemberInfoReq.getMemberId());

            //组装返回数据
            WalletMemberInfoVo walletMemberInfoVo = new WalletMemberInfoVo();

            if (memberInfo == null) {
                //钱包未激活

                //商户号
                walletMemberInfoVo.setMerchantCode(merchantInfo.getCode());

            } else {

                //钱包已激活

                //获取配置信息
                TradeConfig tradeConfig = tradeConfigService.getById(1);

                //将详细信息返回给商户
                BeanUtils.copyProperties(memberInfo, walletMemberInfoVo);
                //商户号
                walletMemberInfoVo.setMerchantCode(merchantInfo.getCode());
                //钱包激活状态
                walletMemberInfoVo.setWalletActivationStatus("1");
                //提现奖励比例
                walletMemberInfoVo.setWithdrawalRewardRatio(tradeConfig.getMerchantSalesBonus());
                //最小提现金额 写死100
                walletMemberInfoVo.setMinimumWithdrawalAmount(new BigDecimal(100));
                //最大提现金额
                walletMemberInfoVo.setMaximumWithdrawalAmount(tradeConfig.getMemberMaxSellAmount());

                //会员id特殊处理 (将拼接的商户号去掉)
                walletMemberInfoVo.setMemberId(memberInfo.getMemberId().replace(merchantInfo.getCode(), ""));
            }

            //签名并加密数据
            EncryptedData encryptedData = RsaUtil.signAndEncryptData(walletMemberInfoVo, platformPrivateKey, merchantPublicKey);
            ApiResponseVo apiResponseVo = new ApiResponseVo();
            BeanUtils.copyProperties(encryptedData, apiResponseVo);
            apiResponseVo.setMerchantCode(merchantInfo.getCode());

            log.info("获取商户会员信息成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, walletMemberInfoReq, walletMemberInfoVo);

            return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);

        } catch (BadPaddingException e) {
            log.error("获取商户会员信息失败, 解密失败，无效的密文或密钥错误 e: {}", e.getMessage());
            return ApiResponse.of(ApiResponseEnum.DECRYPTION_ERROR, null);
        } catch (Exception e) {
            log.error("获取钱包会员信息失败 req: {}, e: {}", apiRequest, e);
        }

        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }

    /**
     * 获取激活钱包地址接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public ApiResponse activateWallet(ApiRequest apiRequest, HttpServletRequest request) {

        //加上分布式锁
        //分布式锁key ar-wallet-activateWallet+商户号
        String key = "ar-wallet-activateWallet" + apiRequest.getMerchantCode();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {
                //获取请求IP
                String requestIp = IpUtil.getRealIP(request);
                log.info("获取激活钱包页面, 商户号: {}, 请求IP: {}", apiRequest.getMerchantCode(), requestIp);

                //获取商户信息
                MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

                String merchantPublicKeyStr = null;
                //获取商户公钥
                if (merchantInfo != null) {
                    merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();
                }

                //校验请求
                ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "获取激活钱包页面");
                if (apiResponse != null) {
                    return apiResponse;
                }

                //语言值
                String lang = request.getHeader("lang");

                if (StringUtils.isEmpty(lang)) {
                    //默认值 9 = hd 印地语
                    lang = "9";
                }

                try {
                    //商户公钥
                    PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

                    //平台私钥
                    PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

                    //使用平台私钥解密数据
                    ActivateWalletReq activateWalletReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, ActivateWalletReq.class);

                    if (activateWalletReq == null) {
                        log.error("获取激活钱包页面失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                        return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
                    }

                    //手动调用验证明文参数
                    Set<ConstraintViolation<ActivateWalletReq>> violations = validator.validate(activateWalletReq);
                    if (!violations.isEmpty()) {
                        // 处理验证错误
                        for (ConstraintViolation<ActivateWalletReq> violation : violations) {
                            log.error("获取激活钱包页面失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, activateWalletReq);
                            System.out.println(violation.getMessage());
                            return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                        }
                    }

                    //使用商户公钥验证签名
                    if (!RsaUtil.verifySignature(activateWalletReq, activateWalletReq.getSign(), merchantPublicKey)) {
                        log.error("获取激活钱包页面失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, activateWalletReq);
                        return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
                    }

                    //处理业务

                    //判断如果商户代收状态和代付状态都是关闭的话 无法激活钱包
                    if (merchantInfo.getRechargeStatus().equals("0") && merchantInfo.getWithdrawalStatus().equals("0")) {
                        log.error("获取激活钱包页面失败, 商户代收状态和代付状态都处于关闭状态: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, activateWalletReq);
                        return ApiResponse.of(ApiResponseEnum.MERCHANT_STATUS_DISABLED, null);
                    }


                    //判断该商户是否是内部商户, 只有内部商户才能激活会员
                    if (MerchantTypeEnum.EXTERNAL_MERCHANT.getCode().equals(merchantInfo.getMerchantType())) {
                        log.error("获取激活钱包页面失败, 该商户不是内部商户: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, activateWalletReq);
                        return ApiResponse.of(ApiResponseEnum.INVALID_REQUEST, null);
                    }

                    //查看该会员是否被注册 (会员id)
                    MemberInfo checkMemberRegistered = memberInfoService.checkMemberRegistered(activateWalletReq.getMerchantCode() + activateWalletReq.getMemberId());

                    if (checkMemberRegistered != null) {
                        log.error("获取激活钱包页面失败, 该会员已被注册: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                        return ApiResponse.of(ApiResponseEnum.MEMBER_ALREADY_REGISTERED, null);
                    }

                    //生成注册页面的token
                    ActivateWallet activateWallet = new ActivateWallet();

                    //商户号
                    activateWallet.setMerchantCode(activateWalletReq.getMerchantCode());

                    //会员id
                    activateWallet.setMemberId(activateWalletReq.getMemberId());

                    //返回地址
                    activateWallet.setReturnUrl(activateWalletReq.getReturnUrl());

                    String activateWalletToken = createActivateWalletToken(activateWallet, TimeUnit.MINUTES.toMillis(arProperty.getWalletActivationPageExpiryTime()));


                    //返回数据
                    ActivateWalletVo activateWalletVo = new ActivateWalletVo();

                    //商户号
                    activateWalletVo.setMerchantCode(merchantInfo.getCode());

                    //会员id
                    activateWalletVo.setMemberId(activateWalletReq.getMemberId());

                    //激活钱包页面地址
                    activateWalletVo.setWalletActivationPageUrl(arProperty.getWalletActivationPageUrl() + "?token=" + activateWalletToken + "&returnUrl=" + activateWalletReq.getReturnUrl() + "&lang=" + lang);

                    //签名并加密数据
                    EncryptedData encryptedData = RsaUtil.signAndEncryptData(activateWalletVo, platformPrivateKey, merchantPublicKey);
                    ApiResponseVo apiResponseVo = new ApiResponseVo();
                    BeanUtils.copyProperties(encryptedData, apiResponseVo);
                    apiResponseVo.setMerchantCode(merchantInfo.getCode());

                    log.info("获取激活钱包页面成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, activateWalletReq, activateWalletVo);

                    return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);
                } catch (Exception e) {
                    log.error("获取激活钱包页面失败 req: {}, e: {}", apiRequest, e);
                    return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
                }

            }
        } catch (Exception e) {
            log.error("获取激活钱包页面失败 商户号: {}, req: {} e: {}", apiRequest.getMerchantCode(), apiRequest, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }


    /**
     * 商户 充值接口 转出
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    @Transactional
    public ApiResponse depositApply(ApiRequest apiRequest, HttpServletRequest request) {

        //获取请求IP
        String requestIp = IpUtil.getRealIP(request);
        log.info("API充值接口, 商户号: {}, 请求IP: {}", apiRequest.getMerchantCode(), requestIp);

        //获取商户信息
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

        String merchantPublicKeyStr = null;
        //获取商户公钥
        if (merchantInfo != null) {
            merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();
        }

        //校验请求
        ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "API充值接口");
        if (apiResponse != null) {
            return apiResponse;
        }

        try {
            //商户公钥
            PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

            //平台私钥
            PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

            //使用平台私钥解密数据
            DepositApplyReq depositApplyReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, DepositApplyReq.class);

            if (depositApplyReq == null) {
                log.error("API充值接口处理失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
            }

            //手动调用验证明文参数
            Set<ConstraintViolation<DepositApplyReq>> violations = validator.validate(depositApplyReq);
            if (!violations.isEmpty()) {
                // 处理验证错误
                for (ConstraintViolation<DepositApplyReq> violation : violations) {
                    log.error("API充值接口处理失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositApplyReq);
                    System.out.println(violation.getMessage());
                    return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                }
            }

            //使用商户公钥验证签名
            if (!RsaUtil.verifySignature(depositApplyReq, depositApplyReq.getSign(), merchantPublicKey)) {
                log.error("API充值接口处理失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositApplyReq);
                return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
            }

            //处理业务

            //订单金额
            BigDecimal amount = new BigDecimal(depositApplyReq.getAmount());

            //校验订单 金额是否小于1
            if (amount.compareTo(BigDecimal.ONE) < 0) {
                log.error("API充值接口处理失败, 金额小于1: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}, 订单金额: {}", requestIp, apiRequest, merchantInfo, depositApplyReq, amount);
                return ApiResponse.of(ApiResponseEnum.AMOUNT_EXCEEDS_LIMIT, null);
            }

            //判断商户代收状态
            if (merchantInfo.getRechargeStatus().equals("0")) {
                //当前商户代收状态未开启
                log.error("API充值接口处理失败, 当前商户代收状态未开启: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositApplyReq);
                return ApiResponse.of(ApiResponseEnum.MERCHANT_COLLECTION_STATUS_DISABLED, null);
            }

            //是否配置最小金额
            boolean isMinCostConfigured = merchantInfo.getMinCost() != null && merchantInfo.getMinCost().compareTo(BigDecimal.ZERO) > 0;

            //是否配置最大金额
            boolean isMaxCostConfigured = merchantInfo.getMaxCost() != null && merchantInfo.getMaxCost().compareTo(BigDecimal.ZERO) > 0;

            boolean isAmountGreaterThanMin = isMinCostConfigured ? amount.compareTo(merchantInfo.getMinCost()) >= 0 : true;
            boolean isAmountLessThanMax = isMaxCostConfigured ? amount.compareTo(merchantInfo.getMaxCost()) <= 0 : true;

            boolean isBetween = isAmountGreaterThanMin && isAmountLessThanMax;

            if (!isBetween) {
                //订单金额不在最小金额和最大金额之间
                log.error("API充值接口处理失败, 金额超过限制: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositApplyReq);
                return ApiResponse.of(ApiResponseEnum.AMOUNT_EXCEEDS_LIMIT, null);
            }

            String memberId = depositApplyReq.getMerchantCode() + depositApplyReq.getMemberId();

            //获取会员信息 需要拼接上 商户号 + 会员id
            MemberInfo memberInfo = memberInfoService.getMemberInfoByMemberId(memberId);

            if (memberInfo == null) {
                log.error("API充值接口处理失败, 获取会员信息失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositApplyReq);
                return ApiResponse.of(ApiResponseEnum.MEMBER_NOT_FOUND, null);
            }

            //校验会员状态
            if (MemberStatusEnum.DISABLE.getCode().equals(memberInfo.getStatus())) {
                //会员已被禁用
                log.error("API充值接口处理失败, 该会员已被禁用: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositApplyReq);
                return ApiResponse.of(ApiResponseEnum.NO_PERMISSION, null);
            }

            //校验余额是否足够
            if (memberInfo.getBalance().compareTo(amount) < 0) {
                log.error("API充值接口处理失败, 余额不足: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositApplyReq);
                return ApiResponse.of(ApiResponseEnum.INSUFFICIENT_BALANCE, null);
            }

            //语言值
            String lang = request.getHeader("lang");

            if (StringUtils.isEmpty(lang)) {
                //默认值 9 = hd 印地语
                lang = "9";
            }

            //生成 商户代收订单
            MerchantCollectOrders merchantCollectOrders = new MerchantCollectOrders();

            //生成平台订单号
            String platformOrder = orderNumberGenerator.generateOrderNo("P");
            merchantCollectOrders.setPlatformOrder(platformOrder);

            //商户订单号
            merchantCollectOrders.setMerchantOrder(depositApplyReq.getMerchantTradeNo());

            //设置支付类型
            merchantCollectOrders.setPayType(depositApplyReq.getChannel());

            //商户号
            merchantCollectOrders.setMerchantCode(depositApplyReq.getMerchantCode());

            //订单金额
            merchantCollectOrders.setAmount(amount);

            //设置会员ID
            merchantCollectOrders.setMemberId(String.valueOf(memberInfo.getId()));

            //设置商户会员ID
            merchantCollectOrders.setExternalMemberId(memberId);

            //交易回调地址
            merchantCollectOrders.setTradeNotifyUrl(depositApplyReq.getNotifyUrl());

            //设置时间戳
            merchantCollectOrders.setTimestamp(depositApplyReq.getTimestamp());

            //设置代收订单费率
            BigDecimal payRate = merchantInfo.getPayRate();
            merchantCollectOrders.setOrderRate(payRate);

            //订单费用 默认为0
            BigDecimal cost = BigDecimal.ZERO;

            //代收费率大于0才计算费用
            if (payRate != null && payRate.compareTo(BigDecimal.ZERO) > 0) {
                //订单费用
                cost = merchantCollectOrders.getAmount().multiply((payRate.divide(BigDecimal.valueOf(100))));
            }


            //设置费用 订单金额 * 费率)
            merchantCollectOrders.setCost(cost);

            //客户端ip
            merchantCollectOrders.setClientIp(requestIp);

            //商户名称
            merchantCollectOrders.setMerchantName(merchantInfo.getUsername());

            //商户类型
            merchantCollectOrders.setMerchantType(merchantInfo.getMerchantType());


            if (merchantCollectOrdersService.save(merchantCollectOrders)) {
                //提交成功

                //订单页面信息
                PaymentInfo paymentInfo = new PaymentInfo();

                //商户号
                paymentInfo.setMerchantCode(merchantInfo.getCode());

                //商户名称
                paymentInfo.setMerchantName(merchantInfo.getUsername());

                //支付剩余时间 秒
                paymentInfo.setPaymentExpireTime(-1L);

                //订单金额
                paymentInfo.setAmount(amount);

                //商户订单号
                paymentInfo.setMerchantOrder(merchantCollectOrders.getMerchantOrder());

                //平台订单号
                paymentInfo.setPlatformOrder(merchantCollectOrders.getPlatformOrder());

                //订单时间
                paymentInfo.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                //支付密码
                paymentInfo.setPaymentPasswordHint(memberInfo.getPaymentPasswordHint());

                //会员id
                paymentInfo.setMemberId(String.valueOf(memberInfo.getId()));

                //商户会员id
                paymentInfo.setExternalMemberId(memberId);

                //返回地址
                paymentInfo.setReturnUrl(depositApplyReq.getReturnUrl());

                //生成订单token
                String paymentToken = createPaymentToken(paymentInfo, TimeUnit.MINUTES.toMillis(arProperty.getPaymentPageExpirationTime()));

                //返回数据
                DepositApplyVo depositApplyVo = new DepositApplyVo();

                //支付地址
                depositApplyVo.setPayUrl(arProperty.getPayUrl() + "?token=" + paymentToken + "&lang=" + lang + "&returnUrl=" + depositApplyReq.getReturnUrl());

                //订单token
                depositApplyVo.setToken(paymentToken);

                //商户号
                depositApplyVo.setMerchantCode(merchantInfo.getCode());

                //会员id
                depositApplyVo.setMemberId(depositApplyReq.getMemberId());

                //平台订单号
                depositApplyVo.setTradeNo(platformOrder);

                //商户订单号
                depositApplyVo.setMerchantTradeNo(depositApplyReq.getMerchantTradeNo());

                //订单有效期
                depositApplyVo.setOrderValidityDuration(arProperty.getPaymentPageExpirationTime() * 60);

                //签名并加密数据
                EncryptedData encryptedData = RsaUtil.signAndEncryptData(depositApplyVo, platformPrivateKey, merchantPublicKey);

                ApiResponseVo apiResponseVo = new ApiResponseVo();
                BeanUtils.copyProperties(encryptedData, apiResponseVo);
                apiResponseVo.setMerchantCode(merchantInfo.getCode());

                log.info("API充值接口订单提交成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, depositApplyReq, depositApplyVo);


                //注册事务同步回调, 事务提交成功后, 发送延时MQ 改变订单为超时状态
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        //发送使 支付订单 超时的MQ
                        Long lastUpdateTimestamp = System.currentTimeMillis();
                        TaskInfo taskInfo = new TaskInfo(platformOrder, TaskTypeEnum.MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_QUEUE.getCode(), lastUpdateTimestamp);
                        //防止并发竞争关系 MQ延迟5秒后再将订单改为超时状态
                        long paymentPageExpirationTime = arProperty.getPaymentPageExpirationTime();
                        long millis = TimeUnit.MINUTES.toMillis(paymentPageExpirationTime);
                        rabbitMQService.sendTimeoutTask(taskInfo, millis + 5000);
                    }
                });


                return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);
            } else {
                //提交失败
                log.error("API充值接口订单提交失败, 请求ip: {}, 请求明文: {}, 请求密文: {}", requestIp, depositApplyReq, apiRequest);
            }
        } catch (DataIntegrityViolationException e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("API充值接口订单提交失败, 数据重复 e: {}", e.getMessage());
            return ApiResponse.of(ApiResponseEnum.DATA_DUPLICATE_SUBMISSION, null);
        } catch (BadPaddingException e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("API充值接口订单提交失败, 解密失败，无效的密文或密钥错误 e: {}", e.getMessage());
            return ApiResponse.of(ApiResponseEnum.DECRYPTION_ERROR, null);
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("API充值接口订单提交失败 req: {}, e: {}", apiRequest, e);
        }
        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }


    /**
     * 获取支付页面(收银台)信息接口
     *
     * @param token
     * @return {@link RestResult}<{@link PaymentInfo}>
     */
    @Override
    public RestResult<PaymentInfo> retrievePaymentDetails(String token) {

        if (redisTemplate.hasKey(token)) {
            //获取支付页面信息
            PaymentInfo paymentInfo = (PaymentInfo) redisTemplate.opsForValue().get(token);

            //设置支付剩余时间
            paymentInfo.setPaymentExpireTime(redisTemplate.getExpire(token, TimeUnit.SECONDS));

            if (paymentInfo != null) {
                log.info("获取支付页面(收银台)信息成功, token: {}, 返回数据: {}", token, paymentInfo);
                return RestResult.ok(paymentInfo);
            }
        }

        log.error("获取支付页面(收银台)信息失败, 该订单不存在或该订单已失效, token: {}", token);
        return RestResult.failure(ResultCode.ORDER_EXPIRED);
    }

    /**
     * 收银台 确认支付 接口
     *
     * @param confirmPaymentReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult confirmPayment(ConfirmPaymentReq confirmPaymentReq) {


        //分布式锁key ar-wallet-confirmPayment+订单token
        String key = "ar-wallet-confirmPayment" + confirmPaymentReq.getToken();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                if (redisTemplate.hasKey(confirmPaymentReq.getToken())) {
                    //获取支付页面信息
                    PaymentInfo paymentInfo = (PaymentInfo) redisTemplate.opsForValue().get(confirmPaymentReq.getToken());

                    //获取订单信息 加排他行锁
                    MerchantCollectOrders merchantCollectOrders = null;
                    if (paymentInfo != null) {
                        merchantCollectOrders = merchantCollectOrdersMapper.selectMerchantCollectOrdersForUpdate(paymentInfo.getPlatformOrder());
                    }

                    if (merchantCollectOrders == null) {
                        log.error("收银台 确认支付 接口处理失败: 订单不存在, 订单信息: {}", paymentInfo);
                        return RestResult.failure(ResultCode.ORDER_EXPIRED);
                    }
                    log.info("收银台 确认支付 : 获取到支付订单锁, 订单状态: {}, 订单号: {}", merchantCollectOrders.getOrderStatus(), merchantCollectOrders.getPlatformOrder());

                    //判断订单是待支付状态才进行处理
                    if (!merchantCollectOrders.getOrderStatus().equals(CollectionOrderStatusEnum.BE_PAID.getCode())) {
                        log.error("收银台 确认支付 接口处理失败: 非法的订单状态: {}, 订单号: {}", merchantCollectOrders.getOrderStatus(), merchantCollectOrders.getPlatformOrder());
                        return RestResult.failure(ResultCode.DATA_DUPLICATE_SUBMISSION);
                    }

                    //分布式锁key ar-wallet-sell+会员id
                    String key2 = "ar-wallet-sell" + paymentInfo.getMemberId();
                    RLock lock2 = redissonUtil.getLock(key2);

                    boolean req2 = false;

                    try {
                        req2 = lock2.tryLock(10, TimeUnit.SECONDS);

                        if (req2) {

                            //获取当前会员信息 加上排他行锁
                            MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(paymentInfo.getMemberId()));

                            //校验支付密码
                            if (!passwordEncoder.matches(confirmPaymentReq.getPaymentPassword(), memberInfo.getPaymentPassword())) {
                                log.error("收银台 确认支付 接口处理失败: 支付密码错误: 会员信息: {}, req: {}", memberInfo, confirmPaymentReq);
                                return RestResult.failure(ResultCode.PASSWORD_VERIFICATION_FAILED);
                            }

                            //校验余额是否足够
                            if (memberInfo.getBalance().compareTo(paymentInfo.getAmount()) < 0) {
                                log.error("收银台 确认支付 接口处理失败: 会员余额不足: 会员信息: {}, req: {}", memberInfo, confirmPaymentReq);
                                return RestResult.failure(ResultCode.INSUFFICIENT_BALANCE);
                            }

                            //更新订单信息: 订单状态 (已完成)
                            merchantCollectOrders.setOrderStatus(CollectionOrderStatusEnum.PAID.getCode());
                            //支付时间
                            merchantCollectOrders.setPaymentTime(LocalDateTime.now());
                            boolean updateCollectOrderInfo = merchantCollectOrdersService.updateById(merchantCollectOrders);

                            if (!updateCollectOrderInfo) {
                                log.error("收银台 确认支付 接口处理失败: 更新订单信息失败，触发事务回滚。 订单信息: {}", merchantCollectOrders);
                                // 抛出运行时异常
                                throw new RuntimeException("收银台 确认支付 接口处理失败: 更新订单信息失败，触发事务回滚。");
                            }

                            //账变前余额
                            BigDecimal previousBalance = memberInfo.getBalance();

                            if (memberInfo == null) {
                                log.error("收银台 确认支付 接口处理失败: 获取会员信息失败");
                                return RestResult.failure(ResultCode.RELOGIN);
                            }

                            //更新会员信息: 扣除会员余额
                            memberInfo.setBalance(memberInfo.getBalance().subtract(paymentInfo.getAmount()));

                            //更新会员信息 累计充值金额
                            memberInfo.setRechargeTotalAmount(memberInfo.getRechargeTotalAmount().add(paymentInfo.getAmount()));

                            //更新会员信息 累计充值次数
                            memberInfo.setRechargeNum(memberInfo.getRechargeNum() + 1);

                            boolean updateMemberInfo = memberInfoService.updateById(memberInfo);

                            if (!updateMemberInfo) {
                                log.error("收银台 确认支付 接口处理失败: 更新会员信息失败，触发事务回滚。 订单信息: {}", merchantCollectOrders);
                                // 抛出运行时异常
                                throw new RuntimeException("收银台 确认支付 接口处理失败: 更新会员信息失败，触发事务回滚。");
                            }

                            //更新商户信息 并记录商户账变
                            Boolean updatemerchantInfo = amountChangeUtil.insertChangeAmountRecord(
                                    paymentInfo.getMerchantCode(),//商户号
                                    paymentInfo.getAmount(),//账变金额 (订单金额)
                                    ChangeModeEnum.ADD,//账变类型 收入
                                    "ARB",//币种
                                    paymentInfo.getPlatformOrder(),//平台订单号
                                    AccountChangeEnum.COLLECTION,//账变类型 代收
                                    DateUtil.format(merchantCollectOrders.getCreateTime(), "yyyy-MM-dd"),//订单时间 精确到日
                                    "API代收",//备注
                                    paymentInfo.getMerchantOrder()
                            );

                            if (!updatemerchantInfo) {
                                log.error("收银台 确认支付 接口处理失败: 记录商户账变失败: req: {}, 订单信息: {}", confirmPaymentReq, merchantCollectOrders);
                                // 抛出运行时异常
                                throw new RuntimeException("收银台 确认支付 接口处理失败: 更新商户信息失败，触发事务回滚。");
                            }

                            //订单费用大于0 才记录订单费用账变
                            if (merchantCollectOrders.getCost().compareTo(BigDecimal.ZERO) > 0) {
                                //记录商户账变 (订单费用)
                                Boolean updatemerchantInfoFee = amountChangeUtil.insertChangeAmountRecord(
                                        paymentInfo.getMerchantCode(),//商户号
                                        merchantCollectOrders.getCost(),//账变金额 (订单费用)
                                        ChangeModeEnum.SUB,//账变类型 支出
                                        "ARB",//币种
                                        merchantCollectOrders.getPlatformOrder(),//平台订单号
                                        AccountChangeEnum.COLLECTION_FEE,//账变类型 代收费用
                                        DateUtil.format(merchantCollectOrders.getCreateTime(), "yyyy-MM-dd"),//订单时间 精确到日
                                        "API代收费用",//备注
                                        merchantCollectOrders.getMerchantOrder()
                                );

                                if (!updatemerchantInfoFee) {
                                    log.error("收银台 确认支付 接口处理失败: 记录商户账变失败: req: {}, 订单信息: {}", confirmPaymentReq, merchantCollectOrders);
                                    // 抛出运行时异常
                                    throw new RuntimeException("收银台 确认支付 接口处理失败: 更新商户信息失败，触发事务回滚。");
                                }
                            }

                            //添加会员账变 (交易记录)
                            Boolean b = memberAccountChangeService.recordMemberTransaction(String.valueOf(memberInfo.getId()), paymentInfo.getAmount(), MemberAccountChangeEnum.WITHDRAWAL.getCode(), merchantCollectOrders.getPlatformOrder(), previousBalance, memberInfo.getBalance(), merchantCollectOrders.getMerchantOrder());

                            if (!b) {
                                log.error("收银台 确认支付 接口处理失败: 记录会员账变失败: req: {}, 订单信息: {}", confirmPaymentReq, merchantCollectOrders);
                                // 抛出运行时异常
                                throw new RuntimeException("收银台 确认支付 接口处理失败: 记录会员账变失败，触发事务回滚。");
                            }


                            //注册事务同步回调 事务提交成功后才执行以下操作
                            final MerchantCollectOrders finalMerchantCollectOrders = merchantCollectOrders;
                            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {

                                    //将收银台数据(订单状态)改为已完成
                                    updatePaymentInfo(confirmPaymentReq.getToken());

                                    //异步通知
                                    TaskInfo taskInfo = new TaskInfo(finalMerchantCollectOrders.getPlatformOrder(), TaskTypeEnum.DEPOSIT_NOTIFICATION.getCode(), System.currentTimeMillis());
                                    rabbitMQService.sendRechargeSuccessCallbackNotification(taskInfo);

                                    log.info("收银台 确认支付 接口处理成功, 执行同步事务回调 会员信息: {}, paymentInfo: {}, paymentOrder:{}, req: {}", memberInfo, paymentInfo, finalMerchantCollectOrders, confirmPaymentReq);

                                }
                            });

                            log.info("收银台 确认支付 接口处理成功, 会员信息: {}, paymentInfo: {}, paymentOrder:{}, req: {}", memberInfo, paymentInfo, merchantCollectOrders, confirmPaymentReq);

                            //返回数据
                            return RestResult.ok();
                        }
                    } catch (Exception e) {
                        //手动回滚
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        log.error("收银台 确认支付 接口处理失败: req: {}, e: {}", confirmPaymentReq, e);
                    } finally {
                        //释放锁
                        if (req && lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }

                        if (req2 && lock2.isHeldByCurrentThread()) {
                            lock2.unlock();
                        }
                    }
                } else {
                    log.error("收银台 确认支付 接口处理失败: 订单已失效: req: {}", confirmPaymentReq);
                    return RestResult.failure(ResultCode.ORDER_EXPIRED);
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("收银台 确认支付 接口处理失败: req: {}, e: {}", confirmPaymentReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 更新收银台页面数据 (支付状态)
     *
     * @param token
     */
    private void updatePaymentInfo(String token) {

        long duration = redisTemplate.getExpire(token, TimeUnit.MILLISECONDS);

        if (duration > 0) {
            // 从 Redis 获取 paymentInfo 对象
            PaymentInfo paymentInfo = (PaymentInfo) redisTemplate.opsForValue().get(token);

            if (paymentInfo != null) {
                // 更新 paymentInfo 对象的字段
                paymentInfo.setOrderStatus(CollectionOrderStatusEnum.PAID.getCode());

                // 将更新后的 paymentInfo 对象重新写入 Redis，并设置之前的剩余过期时间
                redisTemplate.opsForValue().set(token, paymentInfo, duration, TimeUnit.MILLISECONDS);
            } else {
                // 处理 paymentInfo 不存在的情况
            }
        } else {
            // 处理键不存在或已过期的情况
        }
    }


    /**
     * 提现接口 (转入)
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    @Transactional
    public ApiResponse withdrawalApply(ApiRequest apiRequest, HttpServletRequest request) {


        //分布式锁key ar-wallet-withdrawalApply+商户号


        try {


            //获取请求IP
            String requestIp = IpUtil.getRealIP(request);
            log.info("API提现接口, 商户号: {}, 请求IP: {}", apiRequest.getMerchantCode(), requestIp);

            //获取商户信息
            MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

            String merchantPublicKeyStr = null;
            //获取商户公钥
            if (merchantInfo != null) {
                merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();
            }

            //校验请求
            ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "API提现接口");
            if (apiResponse != null) {
                return apiResponse;
            }

            try {
                //商户公钥
                PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

                //平台私钥
                PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

                //使用平台私钥解密数据
                WithdrawalApplyReq withdrawalApplyReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, WithdrawalApplyReq.class);

                if (withdrawalApplyReq == null) {
                    log.error("API提现接口处理失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                    return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
                }

                //手动调用验证明文参数
                Set<ConstraintViolation<WithdrawalApplyReq>> violations = validator.validate(withdrawalApplyReq);
                if (!violations.isEmpty()) {
                    // 处理验证错误
                    for (ConstraintViolation<WithdrawalApplyReq> violation : violations) {
                        log.error("API提现接口处理失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalApplyReq);
                        System.out.println(violation.getMessage());
                        return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                    }
                }

                //使用商户公钥验证签名
                if (!RsaUtil.verifySignature(withdrawalApplyReq, withdrawalApplyReq.getSign(), merchantPublicKey)) {
                    log.error("API提现接口处理失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalApplyReq);
                    return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
                }

                //处理业务

                BigDecimal amount = new BigDecimal(withdrawalApplyReq.getAmount());

                //校验订单 金额是否小于1
                if (amount.compareTo(BigDecimal.ONE) < 0) {
                    log.error("API提现接口处理失败, 金额小于1: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}, 订单金额: {}", requestIp, apiRequest, merchantInfo, withdrawalApplyReq, amount);
                    return ApiResponse.of(ApiResponseEnum.AMOUNT_EXCEEDS_LIMIT, null);
                }

                //判断商户代付状态
                if (merchantInfo.getWithdrawalStatus().equals("0")) {
                    //当前商户代付状态未开启
                    log.error("API提现接口处理失败, 当前商户代付状态未开启: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalApplyReq);
                    return ApiResponse.of(ApiResponseEnum.MERCHANT_PAYMENT_STATUS_DISABLED, null);
                }


                //是否配置最小金额
                boolean isMinCostConfigured = merchantInfo.getMinCost() != null && merchantInfo.getMinCost().compareTo(BigDecimal.ZERO) > 0;

                //是否配置最大金额
                boolean isMaxCostConfigured = merchantInfo.getMaxCost() != null && merchantInfo.getMaxCost().compareTo(BigDecimal.ZERO) > 0;

                boolean isAmountGreaterThanMin = isMinCostConfigured ? amount.compareTo(merchantInfo.getMinCost()) >= 0 : true;
                boolean isAmountLessThanMax = isMaxCostConfigured ? amount.compareTo(merchantInfo.getMaxCost()) <= 0 : true;

                boolean isBetween = isAmountGreaterThanMin && isAmountLessThanMax;

                if (!isBetween) {
                    //订单金额不在最小金额和最大金额之间
                    log.error("API提现接口处理失败, 金额超过限制: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalApplyReq);
                    return ApiResponse.of(ApiResponseEnum.AMOUNT_EXCEEDS_LIMIT, null);
                }

                //获取会员信息 加上排他行锁
                MemberInfo memberInfo = memberInfoMapper.selectMemberInfoByMemberIdForUpdate(withdrawalApplyReq.getMerchantCode() + withdrawalApplyReq.getMemberId());

                if (memberInfo == null) {
                    log.error("API提现接口处理失败, 获取会员信息失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalApplyReq);
                    return ApiResponse.of(ApiResponseEnum.MEMBER_NOT_FOUND, null);
                }


                //分布式锁key ar-wallet-sell+会员id
                String key2 = "ar-wallet-sell" + memberInfo.getId();
                RLock lock2 = redissonUtil.getLock(key2);

                boolean req2 = false;

                try {
                    req2 = lock2.tryLock(10, TimeUnit.SECONDS);

                    if (req2) {

                        //校验会员状态
                        if (MemberStatusEnum.DISABLE.getCode().equals(memberInfo.getStatus())) {
                            //会员已被禁用
                            log.error("API提现接口处理失败, 该会员已被禁用: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalApplyReq);
                            return ApiResponse.of(ApiResponseEnum.NO_PERMISSION, null);
                        }

                        //订单费用 默认为0
                        BigDecimal cost = BigDecimal.ZERO;

                        //卖出订单费率
                        BigDecimal transferRate = merchantInfo.getTransferRate();

                        //判断如果代付费率 大于0才计算费率
                        if (transferRate != null && transferRate.compareTo(BigDecimal.ZERO) > 0) {
                            //订单费用
                            cost = amount.multiply((transferRate.divide(BigDecimal.valueOf(100))));
                        }

                        //订单金额 + 订单费用
                        BigDecimal amountCost = amount.add(cost);

                        //校验商户余额是否足够
                        if (merchantInfo.getBalance().compareTo(amountCost) < 0) {
                            log.error("API提现接口处理失败, 商户余额不足: 订单金额(包括费率): {}, 商户余额: {}, 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", amountCost, merchantInfo.getBalance(), requestIp, apiRequest, merchantInfo, withdrawalApplyReq);
                            return ApiResponse.of(ApiResponseEnum.INSUFFICIENT_MERCHANT_BALANCE, null);
                        }


                        //账变前余额
                        BigDecimal previousBalance = memberInfo.getBalance();


                        //生成商户代付订单
                        MerchantPaymentOrders merchantPaymentOrders = new MerchantPaymentOrders();

                        //订单状态
                        merchantPaymentOrders.setOrderStatus(PaymentOrderStatusEnum.SUCCESS.getCode());

                        //设置商户号
                        merchantPaymentOrders.setMerchantCode(withdrawalApplyReq.getMerchantCode());

                        //设置会员id
                        merchantPaymentOrders.setMemberId(String.valueOf(memberInfo.getId()));

                        //设置商户会员id
                        merchantPaymentOrders.setExternalMemberId(withdrawalApplyReq.getMerchantCode() + withdrawalApplyReq.getMemberId());

                        //设置平台订单号
                        String platformOrder = orderNumberGenerator.generateOrderNo("W");
                        merchantPaymentOrders.setPlatformOrder(platformOrder);

                        //设置商户订单号
                        merchantPaymentOrders.setMerchantOrder(withdrawalApplyReq.getMerchantTradeNo());

                        //设置订单金额
                        merchantPaymentOrders.setAmount(amount);

                        //设置渠道编码 (支付方式)
                        merchantPaymentOrders.setPayType(withdrawalApplyReq.getChannel());

                        //设置时间戳
                        merchantPaymentOrders.setTimestamp(withdrawalApplyReq.getTimestamp());

                        //设置交易回调地址
                        merchantPaymentOrders.setTradeNotifyUrl(withdrawalApplyReq.getNotifyUrl());

                        //设置订单费率 (代付费率)
                        merchantPaymentOrders.setOrderRate(transferRate);

                        //设置费用 订单金额 * 费率)
                        merchantPaymentOrders.setCost(cost);

                        //客户端ip
                        merchantPaymentOrders.setClientIp(requestIp);

                        //商户名称
                        merchantPaymentOrders.setMerchantName(merchantInfo.getUsername());

                        //商户类型
                        merchantPaymentOrders.setMerchantType(merchantInfo.getMerchantType());

                        boolean save = merchantPaymentOrdersService.save(merchantPaymentOrders);

                        if (!save) {
                            log.error("API提现接口处理失败: 生成商户代付订单失败，触发事务回滚。 订单信息: {}, req: {}", merchantPaymentOrders, withdrawalApplyReq);
                            // 抛出运行时异常
                            throw new RuntimeException("API提现接口处理失败: 生成商户代付订单失败，触发事务回滚。");
                        }

                        boolean updateMemberInfo = false;
                        if (save) {

                            //增加会员余额
                            memberInfo.setBalance(memberInfo.getBalance().add(amount));

                            //更新会员信息 累计提现金额
                            memberInfo.setWithdrawTotalAmount(memberInfo.getWithdrawTotalAmount().add(amount));

                            //更新会员信息 累计提现次数
                            memberInfo.setWithdrawNum(memberInfo.getWithdrawNum() + 1);

                            //更新会员信息
                            updateMemberInfo = memberInfoService.updateById(memberInfo);

                            if (!updateMemberInfo) {
                                log.error("API提现接口处理失败: 更新会员信息失败，触发事务回滚。 订单信息: {}, req: {}", merchantPaymentOrders, withdrawalApplyReq);
                                // 抛出运行时异常
                                throw new RuntimeException("API提现接口处理失败: 更新会员信息失败，触发事务回滚。");
                            }

                            //更新商户余额并记录商户账变
                            //记录商户账变 (订单金额)
                            Boolean updatemerchantInfo = amountChangeUtil.insertChangeAmountRecord(
                                    merchantInfo.getCode(),//商户号
                                    merchantPaymentOrders.getAmount(),//账变金额 (订单金额)
                                    ChangeModeEnum.SUB,//账变类型 支出
                                    "ARB",//币种
                                    merchantPaymentOrders.getPlatformOrder(),//平台订单号
                                    AccountChangeEnum.PAYMENT,//账变类型 代付
                                    DateUtil.format(merchantPaymentOrders.getCreateTime(), "yyyy-MM-dd"),//订单时间 精确到日
                                    "API代付",//备注
                                    merchantPaymentOrders.getMerchantOrder()
                            );

                            if (!updatemerchantInfo) {
                                log.error("API提现接口处理失败: 更新商户信息失败，触发事务回滚。 订单信息: {}, req: {}", merchantPaymentOrders, withdrawalApplyReq);
                                // 抛出运行时异常
                                throw new RuntimeException("API提现接口处理失败: 更新商户信息失败，触发事务回滚。");
                            }

                            // 订单费用大于0 才记录 订单费用的账变
                            if (merchantPaymentOrders.getCost().compareTo(BigDecimal.ZERO) > 0) {
                                //记录商户账变 (订单费用)
                                Boolean updatemerchantInfoFee = amountChangeUtil.insertChangeAmountRecord(
                                        merchantInfo.getCode(),//商户号
                                        merchantPaymentOrders.getCost(),//账变金额 (订单费用)
                                        ChangeModeEnum.SUB,//账变类型 支出
                                        "ARB",//币种
                                        merchantPaymentOrders.getPlatformOrder(),//平台订单号
                                        AccountChangeEnum.PAYMENT_FEE,//账变类型 代付费用
                                        DateUtil.format(merchantPaymentOrders.getCreateTime(), "yyyy-MM-dd"),//订单时间 精确到日
                                        "API代付费用",//备注
                                        merchantPaymentOrders.getMerchantOrder()
                                );

                                if (!updatemerchantInfoFee) {
                                    log.error("API提现接口处理失败: 更新商户信息失败，触发事务回滚。 订单信息: {}, req: {}", merchantPaymentOrders, withdrawalApplyReq);
                                    // 抛出运行时异常
                                    throw new RuntimeException("API提现接口处理失败: 更新商户信息失败，触发事务回滚。");
                                }
                            }

                            //添加会员账变 (交易记录)
                            Boolean b = memberAccountChangeService.recordMemberTransaction(String.valueOf(memberInfo.getId()), amount, MemberAccountChangeEnum.DEPOSIT.getCode(), merchantPaymentOrders.getPlatformOrder(), previousBalance, memberInfo.getBalance(), merchantPaymentOrders.getMerchantOrder());

                            if (!b) {
                                log.error("API提现接口处理失败: 记录会员账变失败，触发事务回滚。 订单信息: {}, req: {}", merchantPaymentOrders, withdrawalApplyReq);
                                // 抛出运行时异常
                                throw new RuntimeException("API提现接口处理失败: 记录会员账变失败，触发事务回滚。");
                            }
                        }

                        if (save && updateMemberInfo) {
                            //提交成功

                            //返回数据
                            WithdrawalApplyVo withdrawalApplyVo = new WithdrawalApplyVo();

                            //商户号
                            withdrawalApplyVo.setMerchantCode(merchantInfo.getCode());

                            //会员id
                            withdrawalApplyVo.setMemberId(withdrawalApplyReq.getMemberId());

                            //平台订单号
                            withdrawalApplyVo.setTradeNo(platformOrder);

                            //商户订单号
                            withdrawalApplyVo.setMerchantTradeNo(withdrawalApplyReq.getMerchantTradeNo());

                            //订单金额
                            withdrawalApplyVo.setAmount(amount);

                            //交易状态 (默认代付成功)
                            withdrawalApplyVo.setTradeStatus(merchantPaymentOrders.getOrderStatus());

                            //时间戳
                            withdrawalApplyVo.setTimestamp(String.valueOf(System.currentTimeMillis() / 1000));

                            //签名并加密数据
                            EncryptedData encryptedData = RsaUtil.signAndEncryptData(withdrawalApplyVo, platformPrivateKey, merchantPublicKey);

                            ApiResponseVo apiResponseVo = new ApiResponseVo();
                            BeanUtils.copyProperties(encryptedData, apiResponseVo);
                            apiResponseVo.setMerchantCode(merchantInfo.getCode());

                            log.info("API提现接口订单提交成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, withdrawalApplyReq, withdrawalApplyVo);

                            //注册事务同步回调 事务提交成功了才执行以下操作
                            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {

                                    log.info("API提现接口订单提交成功, 执行事务同步回调 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, withdrawalApplyReq, withdrawalApplyVo);

                                    //发送提现成功 异步延时回调通知
                                    long millis = 3000L;
                                    //发送提现延时回调的MQ消息
                                    TaskInfo taskInfo = new TaskInfo(merchantPaymentOrders.getPlatformOrder(), TaskTypeEnum.WITHDRAW_NOTIFICATION_TIMEOUT.getCode(), System.currentTimeMillis());
                                    rabbitMQService.sendTimeoutTask(taskInfo, millis);

                                }
                            });

                            return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);
                        } else {
                            //提交失败
                            log.error("API提现接口订单提交失败, 请求ip: {}, 请求明文: {}, 请求密文: {}", requestIp, withdrawalApplyReq, apiRequest);
                            //手动回滚
                            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                            return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
                        }

                    }
                } catch (Exception e) {
                    //手动回滚
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    log.error("API提现接口订单提交失败 req: {}, e: {}", apiRequest, e);
                } finally {
                    //释放锁


                    if (req2 && lock2.isHeldByCurrentThread()) {
                        lock2.unlock();
                    }
                }
            } catch (DuplicateKeyException e) {
                //手动回滚
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                log.error("API提现接口订单提交失败, 数据重复 e: {}", e.getMessage());
                return ApiResponse.of(ApiResponseEnum.DATA_DUPLICATE_SUBMISSION, null);
            } catch (BadPaddingException e) {
                //手动回滚
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                log.error("API提现接口订单提交失败, 解密失败，无效的密文或密钥错误 e: {}", e.getMessage());
                return ApiResponse.of(ApiResponseEnum.DECRYPTION_ERROR, null);
            } catch (Exception e) {
                //手动回滚
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                log.error("API提现接口订单提交失败 req: {}, e: {}", apiRequest, e);
            }
            return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);

        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("API提现接口订单提交失败 req: {}, e: {}", apiRequest, e);
        } finally {
            //释放锁

        }
        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }

    /**
     * 商户 进入钱包
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public ApiResponse accessWallet(ApiRequest apiRequest, HttpServletRequest request) {

        // 获取请求头中的clientIp值 (用户真实ip)
        String clientIp = request.getHeader("clientIp");

        if (StringUtils.isEmpty(clientIp)) {
            clientIp = "unknown";
        }

        //语言值
        String lang = request.getHeader("lang");

        if (StringUtils.isEmpty(lang)) {
            //默认值 9 = hd 印地语
            lang = "9";
        }

        //获取请求IP (商户服务器IP)
        String requestIp = IpUtil.getRealIP(request);
        log.info("进入钱包接口: 商户号: {}, 商户服务器IP: {}, 用户ip: {}", apiRequest.getMerchantCode(), requestIp, clientIp);

        //获取商户信息
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

        String merchantPublicKeyStr = null;
        //获取商户公钥
        if (merchantInfo != null) {
            merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();
        }

        //校验请求
        ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "API提现接口");
        if (apiResponse != null) {
            return apiResponse;
        }


        try {
            //商户公钥
            PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

            //平台私钥
            PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

            //使用平台私钥解密数据
            AccessWalletReq accessWalletReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, AccessWalletReq.class);

            if (accessWalletReq == null) {
                log.error("进入钱包失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
            }

            //手动调用验证明文参数
            Set<ConstraintViolation<AccessWalletReq>> violations = validator.validate(accessWalletReq);
            if (!violations.isEmpty()) {
                // 处理验证错误
                for (ConstraintViolation<AccessWalletReq> violation : violations) {
                    log.error("进入钱包失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, accessWalletReq);
                    System.out.println(violation.getMessage());
                    return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                }
            }

            //使用商户公钥验证签名
            if (!RsaUtil.verifySignature(accessWalletReq, accessWalletReq.getSign(), merchantPublicKey)) {
                log.error("进入钱包失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, accessWalletReq);
                return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
            }


            //获取会员信息
            String memberId = accessWalletReq.getMerchantCode() + accessWalletReq.getMemberId();

            //获取会员信息 需要拼接上 商户号 + 会员id
            MemberInfo memberInfo = memberInfoService.getMemberInfoByMemberId(memberId);

            //判断如果商户代收状态和代付状态都是关闭的话 无法激活钱包
            if (merchantInfo.getRechargeStatus().equals("0") && merchantInfo.getWithdrawalStatus().equals("0")) {
                log.error("进入钱包失败, 商户代收状态和代付状态都处于关闭状态: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, accessWalletReq);
                return ApiResponse.of(ApiResponseEnum.MERCHANT_STATUS_DISABLED, null);
            }

            if (memberInfo == null) {
                log.error("进入钱包失败, 获取会员信息失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.MEMBER_NOT_FOUND, null);
            }

            //判断该商户是否是内部商户, 只有内部商户才能进入钱包
            if (MerchantTypeEnum.EXTERNAL_MERCHANT.getCode().equals(merchantInfo.getMerchantType())) {
                log.error("进入钱包失败, 该商户不是内部商户: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, accessWalletReq);
                return ApiResponse.of(ApiResponseEnum.INVALID_REQUEST, null);
            }

            //判断会员类型如果是钱包会员就拒掉
            if (MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberInfo.getMemberType())) {
                log.error("进入钱包失败, 获取到的会员信息是钱包会员: 请求ip: {}, req: {}, 商户信息: {}, 会员信息: {}", requestIp, apiRequest, merchantInfo, memberInfo);
                return ApiResponse.of(ApiResponseEnum.MEMBER_NOT_FOUND, null);
            }

            //校验会员状态
            if (MemberStatusEnum.DISABLE.getCode().equals(memberInfo.getStatus())) {
                //2.已激活会员 状态被禁用 返回禁用页面地址

                //返回数据
                AccessWalletVo accessWalletVo = new AccessWalletVo();

                //商户号
                accessWalletVo.setMerchantCode(merchantInfo.getCode());

                //钱包地址 会员被禁用页面
                accessWalletVo.setWalletAccessUrl(arProperty.getMemberDisabledPageUrl() + "?returnUrl=" + accessWalletReq.getReturnUrl() + "&lang=" + lang + "&merchantName=" + merchantInfo.getUsername());

                //会员id
                accessWalletVo.setMemberId(accessWalletReq.getMemberId());

                //时间戳
                accessWalletVo.setTimestamp(String.valueOf(System.currentTimeMillis() / 1000));

                //签名并加密数据
                EncryptedData encryptedData = RsaUtil.signAndEncryptData(accessWalletVo, platformPrivateKey, merchantPublicKey);

                ApiResponseVo apiResponseVo = new ApiResponseVo();
                BeanUtils.copyProperties(encryptedData, apiResponseVo);
                apiResponseVo.setMerchantCode(merchantInfo.getCode());

                log.info("进入钱包接口处理成功, 进入会员禁用页面: 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, accessWalletReq, accessWalletVo);

                return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);
            }

            //加密用户名
            byte[] decodedKey = Base64.getDecoder().decode("wz+glqDb2YceJ3piABkWig==");
            SecretKeySpec reqKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            HashMap<String, String> req = new HashMap<>();
            req.put("data", RsaUtil.encryptData(memberInfo.getMemberAccount(), reqKey));

            String res = RequestUtil.get("http://127.0.0.1:20001/oauth/generateToken", req, clientIp);

            if (res == null) {
                log.error("进入钱包失败, 获取AES密钥和token失败: res=null, 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
            }

            //解密数据
            //从redis里面获取 AES密钥 和token
            JSONObject tokenAndKey = retrieveTokenAndKey(memberInfo.getMemberAccount());

            if (tokenAndKey == null) {
                log.error("进入钱包失败, 获取AES密钥和token失败: tokenAndKey=null, 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
            }

            // 2. 使用AES密钥解密数据
            String token = RsaUtil.decryptData((String) tokenAndKey.get("encryptedData"), convertStringToAESKey((String) tokenAndKey.get("aesKey")));
            String refreshToken = RsaUtil.decryptData((String) tokenAndKey.get("encryptedData2"), convertStringToAESKey((String) tokenAndKey.get("aesKey")));

            if (StringUtils.isEmpty(token)) {
                log.error("进入钱包失败, token为null: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
            }

            if (StringUtils.isEmpty(refreshToken)) {
                log.error("进入钱包失败, refreshToken为null: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
            }

            //返回数据
            AccessWalletVo accessWalletVo = new AccessWalletVo();

            //商户号
            accessWalletVo.setMerchantCode(merchantInfo.getCode());

            //钱包地址
            accessWalletVo.setWalletAccessUrl(arProperty.getWalletAccessUrl() + "?memberId=" + memberInfo.getId() + "&returnUrl=" + accessWalletReq.getReturnUrl() + "&token=" + token + "&refreshToken=" + refreshToken + "&lang=" + lang + "&merchantName=" + merchantInfo.getUsername());

            //会员id
            accessWalletVo.setMemberId(accessWalletReq.getMemberId());

            //时间戳
            accessWalletVo.setTimestamp(String.valueOf(System.currentTimeMillis() / 1000));

            //签名并加密数据
            EncryptedData encryptedData = RsaUtil.signAndEncryptData(accessWalletVo, platformPrivateKey, merchantPublicKey);

            ApiResponseVo apiResponseVo = new ApiResponseVo();
            BeanUtils.copyProperties(encryptedData, apiResponseVo);
            apiResponseVo.setMerchantCode(merchantInfo.getCode());

            log.info("进入钱包接口处理成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, accessWalletReq, accessWalletVo);

            return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);
        } catch (Exception e) {
            log.error("API充值接口订单提交失败 req: {}, e: {}", apiRequest, e);
        }
        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }


    /**
     * 将字符串转为 AES密钥
     *
     * @param strKey
     * @return {@link SecretKey}
     */
    public SecretKey convertStringToAESKey(String strKey) {
        byte[] decodedKey = Base64.getDecoder().decode(strKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }


    /**
     * 从Redis获取 aes密钥和token
     *
     * @param username
     * @return {@link JSONObject}
     */
    public JSONObject retrieveTokenAndKey(String username) {
        // 从Redis获取数据
        String key = "GENERATETOKEN:" + username;
        return (JSONObject) redisTemplate.opsForValue().get(key);
    }


    /**
     * 查询充值订单
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public ApiResponse depositQuery(ApiRequest apiRequest, HttpServletRequest request) {


        //获取请求IP
        String requestIp = IpUtil.getRealIP(request);
        log.info("查询充值订单: {}, 商户号: {}, 请求IP: {}", apiRequest.getMerchantCode(), requestIp);

        //获取商户信息
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

        String merchantPublicKeyStr = null;
        //获取商户公钥
        if (merchantInfo != null) {
            merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();
        }

        //校验请求
        ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "查询充值订单");
        if (apiResponse != null) {
            return apiResponse;
        }

        try {

            //商户公钥
            PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

            //平台私钥
            PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

            //使用平台私钥解密数据
            DepositQueryReq depositQueryReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, DepositQueryReq.class);

            if (depositQueryReq == null) {
                log.error("查询充值订单失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
            }
            //手动调用验证明文参数
            Set<ConstraintViolation<DepositQueryReq>> violations = validator.validate(depositQueryReq);
            if (!violations.isEmpty()) {
                // 处理验证错误
                for (ConstraintViolation<DepositQueryReq> violation : violations) {
                    log.error("查询充值订单失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositQueryReq);
                    System.out.println(violation.getMessage());
                    return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                }
            }

            //使用商户公钥验证签名
            if (!RsaUtil.verifySignature(depositQueryReq, depositQueryReq.getSign(), merchantPublicKey)) {
                log.error("查询充值订单失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositQueryReq);
                return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
            }

            //处理业务

            //查询充值订单
            MerchantCollectOrders orderInfoByOrderNumber = merchantCollectOrdersService.getOrderInfoByOrderNumber(depositQueryReq.getMerchantTradeNo());

            if (orderInfoByOrderNumber == null) {
                log.error("查询充值订单失败, 订单不存在: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositQueryReq);
                return ApiResponse.of(ApiResponseEnum.ORDER_NOT_FOUND, null);
            }

            //查看该订单是否属于该商户
            if (!orderInfoByOrderNumber.getMerchantCode().equals(merchantInfo.getCode())) {
                log.error("查询充值订单失败, 该笔订单不属于该商户: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, depositQueryReq);
                return ApiResponse.of(ApiResponseEnum.ORDER_NOT_FOUND, null);
            }

            //组装返回数据
            DepositQueryVo depositQueryVo = new DepositQueryVo();

            //商户号
            depositQueryVo.setMerchantCode(orderInfoByOrderNumber.getMerchantCode());

            //商户订单号
            depositQueryVo.setMerchantTradeNo(orderInfoByOrderNumber.getMerchantOrder());

            //平台订单号
            depositQueryVo.setTradeNo(orderInfoByOrderNumber.getPlatformOrder());

            //充值金额
            depositQueryVo.setAmount(orderInfoByOrderNumber.getAmount());

            //订单时间
            depositQueryVo.setOrderDateTime(orderInfoByOrderNumber.getCreateTime());

            //交易状态
            depositQueryVo.setTradeStatus(orderInfoByOrderNumber.getOrderStatus());

            //签名并加密数据
            EncryptedData encryptedData = RsaUtil.signAndEncryptData(depositQueryVo, platformPrivateKey, merchantPublicKey);
            ApiResponseVo apiResponseVo = new ApiResponseVo();
            BeanUtils.copyProperties(encryptedData, apiResponseVo);
            apiResponseVo.setMerchantCode(merchantInfo.getCode());

            log.info("查询充值订单成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, depositQueryReq, depositQueryVo);

            return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);

        } catch (BadPaddingException e) {
            log.error("查询充值订单失败, 解密失败，无效的密文或密钥错误 e: {}", e.getMessage());
            return ApiResponse.of(ApiResponseEnum.DECRYPTION_ERROR, null);
        } catch (Exception e) {
            log.error("查询充值订单失败 req: {}, e: {}", apiRequest, e);
        }

        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }


    /**
     * 查询提现订单
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public ApiResponse withdrawalQuery(ApiRequest apiRequest, HttpServletRequest request) {


        //获取请求IP
        String requestIp = IpUtil.getRealIP(request);
        log.info("查询提现订单: {}, 商户号: {}, 请求IP: {}", apiRequest.getMerchantCode(), requestIp);

        //获取商户信息
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

        String merchantPublicKeyStr = null;
        //获取商户公钥
        if (merchantInfo != null) {
            merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();
        }

        //校验请求
        ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "查询提现订单");
        if (apiResponse != null) {
            return apiResponse;
        }

        try {

            //商户公钥
            PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

            //平台私钥
            PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

            //使用平台私钥解密数据
            WithdrawalQueryReq withdrawalQueryReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, WithdrawalQueryReq.class);

            if (withdrawalQueryReq == null) {
                log.error("查询提现订单失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
            }
            //手动调用验证明文参数
            Set<ConstraintViolation<WithdrawalQueryReq>> violations = validator.validate(withdrawalQueryReq);
            if (!violations.isEmpty()) {
                // 处理验证错误
                for (ConstraintViolation<WithdrawalQueryReq> violation : violations) {
                    log.error("查询提现订单失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalQueryReq);
                    System.out.println(violation.getMessage());
                    return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                }
            }

            //使用商户公钥验证签名
            if (!RsaUtil.verifySignature(withdrawalQueryReq, withdrawalQueryReq.getSign(), merchantPublicKey)) {
                log.error("查询提现订单失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalQueryReq);
                return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
            }

            //处理业务

            //查询提现订单
            MerchantPaymentOrders orderInfoByOrderNumber = merchantPaymentOrdersService.getOrderInfoByOrderNumber(withdrawalQueryReq.getMerchantTradeNo());

            if (orderInfoByOrderNumber == null) {
                log.error("查询提现订单失败, 订单不存在: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalQueryReq);
                return ApiResponse.of(ApiResponseEnum.ORDER_NOT_FOUND, null);
            }

            //查看该订单是否属于该商户
            if (!orderInfoByOrderNumber.getMerchantCode().equals(merchantInfo.getCode())) {
                log.error("查询提现订单失败, 该笔订单不属于该商户: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, withdrawalQueryReq);
                return ApiResponse.of(ApiResponseEnum.ORDER_NOT_FOUND, null);
            }

            //组装返回数据
            WithdrawalQueryVo withdrawalQueryVo = new WithdrawalQueryVo();

            //商户号
            withdrawalQueryVo.setMerchantCode(orderInfoByOrderNumber.getMerchantCode());

            //商户订单号
            withdrawalQueryVo.setMerchantTradeNo(orderInfoByOrderNumber.getMerchantOrder());

            //平台订单号
            withdrawalQueryVo.setTradeNo(orderInfoByOrderNumber.getPlatformOrder());

            //充值金额
            withdrawalQueryVo.setAmount(orderInfoByOrderNumber.getAmount());

            //订单时间
            withdrawalQueryVo.setOrderDateTime(orderInfoByOrderNumber.getCreateTime());

            //交易状态
            withdrawalQueryVo.setTradeStatus(orderInfoByOrderNumber.getOrderStatus());

            //签名并加密数据
            EncryptedData encryptedData = RsaUtil.signAndEncryptData(withdrawalQueryVo, platformPrivateKey, merchantPublicKey);
            ApiResponseVo apiResponseVo = new ApiResponseVo();
            BeanUtils.copyProperties(encryptedData, apiResponseVo);
            apiResponseVo.setMerchantCode(merchantInfo.getCode());

            log.info("查询提现订单成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, withdrawalQueryReq, withdrawalQueryVo);

            return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);

        } catch (BadPaddingException e) {
            log.error("查询提现订单失败, 解密失败，无效的密文或密钥错误 e: {}", e.getMessage());
            return ApiResponse.of(ApiResponseEnum.DECRYPTION_ERROR, null);
        } catch (Exception e) {
            log.error("查询提现订单失败 req: {}, e: {}", apiRequest, e);
        }

        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }


    /**
     * 获取激活钱包页面信息接口
     *
     * @param token
     * @return {@link RestResult}<{@link PaymentInfo}>
     */
    @Override
    public RestResult<ActivateWallet> getWalletActivationPageInfo(String token) {

        if (redisTemplate.hasKey(token)) {

            //获取激活钱包页面信息
            ActivateWallet activateWallet = (ActivateWallet) redisTemplate.opsForValue().get(token);

            //设置支付剩余时间
            activateWallet.setWalletActivationPageExpiryTime(redisTemplate.getExpire(token, TimeUnit.SECONDS));

            if (activateWallet != null) {
                log.info("获取激活钱包页面信息成功, token: {}, 返回数据: {}", token, activateWallet);
                return RestResult.ok(activateWallet);
            }
        }

        log.error("获取激活钱包页面信息失败, 该订单不存在或该订单已失效, token: {}", token);
        return RestResult.failure(ResultCode.ORDER_EXPIRED);
    }

    /**
     * 激活钱包
     *
     * @param initiateWalletActivationReq
     * @param request
     * @return {@link RestResult}<{@link InitiateWalletActivationVo}>
     */
    @Override
    public RestResult<InitiateWalletActivationVo> initiateWalletActivation(InitiateWalletActivationReq initiateWalletActivationReq, HttpServletRequest request) {


        //分布式锁key ar-wallet-initiateWalletActivation+订单token
        String key = "ar-wallet-initiateWalletActivation" + initiateWalletActivationReq.getToken();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        String realIP = IpUtil.getRealIP(request);

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                if (redisTemplate.hasKey(initiateWalletActivationReq.getToken())) {


                    //获取激活钱包页面信息
                    ActivateWallet activateWallet = (ActivateWallet) redisTemplate.opsForValue().get(initiateWalletActivationReq.getToken());

                    //查看该会员是否被注册 (会员id 手机号)
                    MemberInfo checkMemberRegistered = memberInfoService.checkMemberRegistered(activateWallet.getMerchantCode() + activateWallet.getMemberId(), initiateWalletActivationReq.getMobileNumber());

                    if (checkMemberRegistered != null) {
                        log.error("激活钱包失败, 该会员已被注册: 请求ip: {}, req: {}, 激活钱包页面信息: {}", realIP, initiateWalletActivationReq, activateWallet);
                        return RestResult.failure(ResultCode.MOBILE_ALREADY_REGISTERED);
                    }

                    //获取商户信息
                    MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(activateWallet.getMerchantCode());

                    if (merchantInfo == null) {
                        log.error("激活钱包失败, 获取商户信息失败: 请求ip: {}, req: {}, 激活钱包页面信息: {}", realIP, initiateWalletActivationReq, activateWallet);
                        return RestResult.failure(ResultCode.INVALID_REQUEST);
                    }

                    //校验手机验证码
                    ValidateSmsCodeReq validateSmsCodeReq = new ValidateSmsCodeReq();
                    validateSmsCodeReq.setMobileNumber(initiateWalletActivationReq.getMobileNumber());
                    validateSmsCodeReq.setVerificationCode(initiateWalletActivationReq.getVerificationCode());

                    if (!memberInfoService.signUpValidateSmsCode(validateSmsCodeReq)) {
                        log.error("激活钱包失败, 手机验证码校验失败, 手机: 请求ip: {}, req: {}, 激活钱包页面信息: {}", realIP, initiateWalletActivationReq, activateWallet);
                        return RestResult.failure(ResultCode.VERIFICATION_CODE_ERROR);
                    }


                    //注册会员
                    MemberInfo memberInfo = new MemberInfo();
                    //设置会员id 商户号 + 商户会员id
                    memberInfo.setMemberId(activateWallet.getMerchantCode() + activateWallet.getMemberId());
                    //设置会员账号 (手机号)
                    memberInfo.setMemberAccount(initiateWalletActivationReq.getMobileNumber());
                    //设置手机号
                    memberInfo.setMobileNumber(initiateWalletActivationReq.getMobileNumber());
                    //设置密码 商户那边注册的会员 不设置登录密码  避免被登录  设置一个复杂的随机密码
                    memberInfo.setPassword(passwordEncoder.encode(SignAPI.calculate(SignUtil.generateMd5Key())));

                    //设置支付密码提示语
                    memberInfo.setPaymentPasswordHint(initiateWalletActivationReq.getPaymentPasswordHint());

                    //设置商户号
                    memberInfo.setMerchantCode(merchantInfo.getCode());

                    //设置商户名称
                    memberInfo.setMerchantName(merchantInfo.getUsername());

                    //设置会员类型 (内部商户会员)
                    memberInfo.setMemberType(MemberTypeEnum.INTERNAL_MERCHANT_MEMBER.getCode());

                    //设置注册设备
                    memberInfo.setRegisterDevice(UserAgentUtil.getDeviceType(request.getHeader("user-agent")));
                    //设置注册ip
                    memberInfo.setRegisterIp(realIP);
                    //设置钱包地址
                    memberInfo.setWalletAddress(CryptoWalletGenerator.generateWalletAddress());
                    //设置随机昵称
                    memberInfo.setNickname(NicknameGeneratorUtil.generateNickname());
                    //设置支付密码
                    memberInfo.setPaymentPassword(passwordEncoder.encode(initiateWalletActivationReq.getPaymentPassword()));

                    //设置首次登录ip
                    memberInfo.setFirstLoginIp(realIP);
                    //设置首次登录时间
                    memberInfo.setFirstLoginTime(LocalDateTime.now());

                    //生成下级邀请码
                    memberInfo.setInvitationCode(UniqueCodeGeneratorUtil.generateInvitationCode());

                    if (memberInfoService.save(memberInfo)) {

                        log.info("激活钱包处理成功, 请求ip: {}, 激活钱包页面信息:{}, 商户信息: {}, 会员信息: {}, req: {}", realIP, activateWallet, merchantInfo, memberInfo, initiateWalletActivationReq);

                        //生成登录token

                        //加密用户名
                        byte[] decodedKey = Base64.getDecoder().decode("wz+glqDb2YceJ3piABkWig==");
                        SecretKeySpec reqKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

                        HashMap<String, String> reqMap = new HashMap<>();
                        reqMap.put("data", RsaUtil.encryptData(memberInfo.getMemberAccount(), reqKey));

                        String res = RequestUtil.get("http://127.0.0.1:20001/oauth/generateToken", reqMap, realIP);

                        if (res == null) {
                            log.error("进入钱包失败, 获取AES密钥和token失败: res = null, 会员信息: {}, 请求ip: {}, req: {}, 商户信息: {}", memberInfo, realIP, initiateWalletActivationReq, merchantInfo);
                            return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
                        }

                        //解密数据
                        //从redis里面获取 AES密钥 和token
                        JSONObject tokenAndKey = retrieveTokenAndKey(memberInfo.getMemberAccount());

                        if (tokenAndKey == null) {
                            log.error("进入钱包失败, 获取AES密钥和token失败: tokenAndKey = null, 请求ip: {}, req: {}, 商户信息: {}", realIP, initiateWalletActivationReq, merchantInfo);
                            return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
                        }

                        // 2. 使用AES密钥解密数据
                        String token = RsaUtil.decryptData((String) tokenAndKey.get("encryptedData"), convertStringToAESKey((String) tokenAndKey.get("aesKey")));

                        if (StringUtils.isEmpty(token)) {
                            log.error("进入钱包失败, token为null: 请求ip: {}, req: {}, 商户信息: {}", realIP, initiateWalletActivationReq, merchantInfo);
                            return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
                        }

                        InitiateWalletActivationVo initiateWalletActivationVo = new InitiateWalletActivationVo();

                        //会员id
                        initiateWalletActivationVo.setMemberId(String.valueOf(memberInfo.getId()));

                        //钱包地址
                        initiateWalletActivationVo.setWalletAccessUrl(arProperty.getWalletAccessUrl());

                        //token
                        initiateWalletActivationVo.setToken(token);

                        //返回地址
                        initiateWalletActivationVo.setReturnUrl(activateWallet.getReturnUrl());

                        //将激活钱包页面从Redis里面删除
//                    redisTemplate.delete(initiateWalletActivationReq.getToken());

                        //返回数据
                        return RestResult.ok(initiateWalletActivationVo);
                    }
                } else {
                    log.error("激活钱包失败处理失败: 订单已失效: req: {}, 请求ip: {}", initiateWalletActivationReq, realIP);
                    return RestResult.failure(ResultCode.EXPIRED);
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("激活钱包失败处理失败: req: {}, e: {}", initiateWalletActivationReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 取消支付
     *
     * @param cancelPaymentReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult cancelPayment(CancelPaymentReq cancelPaymentReq) {


        //分布式锁key ar-wallet-confirmPayment+订单token
        String key = "ar-wallet-confirmPayment" + cancelPaymentReq.getToken();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                if (redisTemplate.hasKey(cancelPaymentReq.getToken())) {
                    //获取支付页面信息
                    PaymentInfo paymentInfo = (PaymentInfo) redisTemplate.opsForValue().get(cancelPaymentReq.getToken());

                    //获取订单信息 加排他行锁
                    MerchantCollectOrders merchantCollectOrders = merchantCollectOrdersMapper.selectMerchantCollectOrdersForUpdate(paymentInfo.getPlatformOrder());

                    if (merchantCollectOrders == null) {
                        log.error("收银台 取消支付 接口处理失败: 订单不存在, 订单号: {}", paymentInfo.getPlatformOrder());
                        return RestResult.failure(ResultCode.ORDER_EXPIRED);
                    }

                    log.info("收银台 取消支付 : 获取到支付订单锁, 订单状态: {}, 订单号: {}", merchantCollectOrders.getOrderStatus(), merchantCollectOrders.getPlatformOrder());

                    //判断订单是待支付状态才进行处理
                    if (!merchantCollectOrders.getOrderStatus().equals(CollectionOrderStatusEnum.BE_PAID.getCode())) {
                        log.error("收银台 取消支付 接口处理失败: 非法的订单状态: {}, 订单号: {}", merchantCollectOrders.getOrderStatus(), merchantCollectOrders.getPlatformOrder());
                        return RestResult.failure(ResultCode.DATA_DUPLICATE_SUBMISSION);
                    }

                    //将订单改为已取消
                    //更新订单信息: 订单状态 (已取消)
                    Boolean updateCancelPayment = merchantCollectOrdersService.cancelPayment(merchantCollectOrders.getPlatformOrder());

                    //回调商户 订单已取消
                    if (updateCancelPayment) {
                        //取消订单成功 异步回调通知
                        //异步通知
                        TaskInfo taskInfo = new TaskInfo(merchantCollectOrders.getPlatformOrder(), TaskTypeEnum.DEPOSIT_NOTIFICATION.getCode(), System.currentTimeMillis());
                        rabbitMQService.sendRechargeSuccessCallbackNotification(taskInfo);

                        log.info("收银台 取消支付 接口处理成功, 订单号: {}", merchantCollectOrders.getPlatformOrder());

                        //返回数据
                        return RestResult.ok();
                    } else {
                        log.error("收银台 取消支付 接口处理失败, 订单号: {}", merchantCollectOrders.getPlatformOrder());
                    }
                } else {
                    log.error("收银台 取消支付 接口处理失败: 订单已失效: req: {}", cancelPaymentReq);
                    return RestResult.failure(ResultCode.ORDER_EXPIRED);
                }
            }
        } catch (Exception e) {
            log.error("收银台 取消支付 接口处理失败: req: {}, e: {}", cancelPaymentReq, e.getMessage());
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 用户登录
     *
     * @param username
     * @param password
     * @return {@link Object}
     */
//    public Object login(String username, String password) {
//        String BasicInfo = arProperty.getBasicAuthName() + ":" + arProperty.getBasicAuthPasswd();
//        String authHeader = "Basic " + Base64.getEncoder().encodeToString(BasicInfo.getBytes());
//        return authClient.getAccessToken(authHeader, "password", username, password);
//    }


    /**
     * 生成激活页面token并存储激活信息到Redis
     *
     * @param activateWallet
     * @param duration
     * @return {@link String}
     */
    public String createActivateWalletToken(ActivateWallet activateWallet, long duration) {
        String token = generateTokenActivateWallet("activateWallet" + activateWallet.getMerchantCode() + activateWallet.getMemberId(), duration, arProperty.getSecretKey());
        redisTemplate.opsForValue().set(token, activateWallet, duration, TimeUnit.MILLISECONDS);
        return token;
    }

    public String generateTokenActivateWallet(String subject, long ttlMillis, String secretKey) {

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // 使用UUID作为JTI
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(nowMillis + ttlMillis))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * 生成订单token并存储支付信息到Redis
     *
     * @param paymentInfo
     * @param duration
     * @return {@link String}
     */
    public String createPaymentToken(PaymentInfo paymentInfo, long duration) {
        String token = generateTokenPayment("payment" + paymentInfo.getMerchantCode() + paymentInfo.getMemberId(), duration, arProperty.getSecretKey());
        redisTemplate.opsForValue().set(token, paymentInfo, duration, TimeUnit.MILLISECONDS);
        return token;
    }


    public String generateTokenPayment(String subject, long ttlMillis, String secretKey) {

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        return Jwts.builder()
                .setId(UUID.randomUUID().toString()) // 使用UUID作为JTI
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(new Date(nowMillis + ttlMillis))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }


    /**
     * 校验请求
     *
     * @param apiRequest           请求对象
     * @param requestIp            请求IP
     * @param merchantInfo         商户信息
     * @param merchantPublicKeyStr 商户公钥
     * @param apiName
     * @return ApiResponse对象，如果参数有效则为null
     */
    private ApiResponse validateRequest(ApiRequest apiRequest, String requestIp, MerchantInfo merchantInfo, String merchantPublicKeyStr, String apiName) {

        if (apiRequest == null || StringUtils.isEmpty(apiRequest.getMerchantCode()) || StringUtils.isEmpty(apiRequest.getEncryptedData()) || StringUtils.isEmpty(apiRequest.getEncryptedKey())) {
            log.error(apiName + "失败, 请求参数错误, 请求参数: {}, 请求ip: {}", apiRequest, requestIp);
            return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
        }

        if (merchantInfo == null || StringUtils.isEmpty(merchantInfo.getCode())) {
            log.error(apiName + "失败, 商户号不存在 请求参数: {}, 商户信息: {}", apiRequest, merchantInfo);
            return ApiResponse.of(ApiResponseEnum.INVALID_REQUEST, null);
        }

        //校验ip
        if (!IpUtil.validateClientIp(requestIp, merchantInfo.getApiAllowedIps())) {
            log.error(apiName + "失败, ip校验失败: 请求ip: {}, 商户信息: {}", requestIp, merchantInfo);
            return ApiResponse.of(ApiResponseEnum.INVALID_IP, null);
        }

        if (StringUtils.isEmpty(merchantPublicKeyStr)) {
            log.error(apiName + "失败, 获取商户公钥失败: 请求ip: {}, 商户信息: {}", requestIp, merchantInfo);
            return ApiResponse.of(ApiResponseEnum.INVALID_MERCHANT_PUBLIC_KEY, null);
        }

        return null;
    }


//    @Override
//    public RestResult<InitiateWalletActivationVo> initiateAppWalletActivation(InitiateAppWalletActivationReq initiateWalletActivationReq, HttpServletRequest request) {
//
//
//        //分布式锁key ar-wallet-initiateWalletActivation+订单token
//        String key = "ar-wallet-initiateAppWalletActivation" + initiateWalletActivationReq.getToken();
//        RLock lock = redissonUtil.getLock(key);
//
//        boolean req = false;
//
//        String realIP = IpUtil.getRealIP(request);
//
//        try {
//            req = lock.tryLock(10, TimeUnit.SECONDS);
//
//            if (req) {
//
//                    //查看该会员是否被注册 (会员id 手机号)
//                    MemberInfo checkMemberRegistered = memberInfoService.checkMemberRegistered("app" + initiateWalletActivationReq.getMobileNumber(), initiateWalletActivationReq.getMobileNumber());
//
//              if(checkMemberRegistered !=null) return getByMember(checkMemberRegistered);
//
//                  //注册会员
//                  MemberInfo memberInfo = new MemberInfo();
//                  //设置会员账号 (手机号)
//                  memberInfo.setMemberAccount(initiateWalletActivationReq.getMobileNumber());
//                  //设置手机号
//                  memberInfo.setMobileNumber(initiateWalletActivationReq.getMobileNumber());
//                  //设置密码 商户那边注册的会员 不设置登录密码  避免被登录  设置一个复杂的随机密码
//                  memberInfo.setPassword(passwordEncoder.encode("123456"));
//
//                  //设置支付密码提示语
//                  memberInfo.setPaymentPasswordHint("123456");
//
//
//                  //设置会员类型 (内部商户会员)
//                  memberInfo.setMemberType(MemberTypeEnum.APP_MEMBER.getCode());
//
//                  //设置注册设备
//                  memberInfo.setRegisterDevice(UserAgentUtil.getDeviceType(request.getHeader("user-agent")));
//                  //设置注册ip
//                  memberInfo.setRegisterIp(realIP);
//                  //设置钱包地址
//                  memberInfo.setWalletAddress(CryptoWalletGenerator.generateWalletAddress());
//                  //设置随机昵称
//                  memberInfo.setNickname(NicknameGeneratorUtil.generateNickname());
//                  //设置支付密码
//                  memberInfo.setPaymentPassword(passwordEncoder.encode("123456"));
//
//                  //设置首次登录ip
//                  memberInfo.setFirstLoginIp(realIP);
//                  //设置首次登录时间
//                  memberInfo.setFirstLoginTime(LocalDateTime.now());
//
//                  if (memberInfoService.save(memberInfo)) {
//                      AppToken appToken = new AppToken();
//                      appToken.setMemberId(memberInfo.getId().toString());
//                      appToken.setToken(initiateWalletActivationReq.getToken());
//                      appToken.setMobileNumber(initiateWalletActivationReq.getMobileNumber());
//                      appTokenService.save(appToken);
//                      log.info("激活钱包处理成功, 请求ip: {}, 激活钱包页面信息:{}, 会员信息: {}, req: {}", realIP, initiateWalletActivationReq, memberInfo, initiateWalletActivationReq);
//                      return getByMember(memberInfo);
//                      //返回数据
//
//                  }
//
//
//            }
//        } catch (Exception e) {
//            //手动回滚
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            log.error("激活钱包失败处理失败: req: {}, e: {}", initiateWalletActivationReq, e);
//        } finally {
//            //释放锁
//            if (req && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
//    }


//    public RestResult<InitiateWalletActivationVo>  getByMember(MemberInfo memberInfo){
//
//
//        JSONObject josnObject = new JSONObject();
//        josnObject.put("grant_type", "password");
//        josnObject.put("username", memberInfo.getMemberAccount());
//        josnObject.put("password", "123456");
//        String url = "http://127.0.0.1:20001/oauth/token?grant_type=password&password=123456&totpCode=443482&username=" + memberInfo.getMemberAccount();
//        String res = RequestUtil.getForAuth(url, josnObject.toJSONString());
//        if (StringUtils.isEmpty(res)) return RestResult.failed();
//        JSONObject tokenInfo = JSONObject.parseObject(res);
//        JSONObject dataOject = tokenInfo.getJSONObject("data");
//        if (dataOject == null) return RestResult.failed();
//        //JSONObject accessTokenOject = dataOject.getJSONObject("accessToken");
//        String token = dataOject.getString("access_token");
//
//
//        InitiateWalletActivationVo initiateWalletActivationVo = new InitiateWalletActivationVo();
//
//        //会员id
//        initiateWalletActivationVo.setMemberId(String.valueOf(memberInfo.getId()));
//
//        //钱包地址
//        initiateWalletActivationVo.setWalletAccessUrl(arProperty.getWalletAccessUrl());
//
//        //token
//        initiateWalletActivationVo.setToken(token);
//        return RestResult.ok(initiateWalletActivationVo);
//
//    }

    /**
     * 查看退回订单
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @Override
    public ApiResponse checkCashBack(ApiRequest apiRequest, HttpServletRequest request) {

        //获取请求IP
        String requestIp = IpUtil.getRealIP(request);
        log.info("查询余额退回订单: 商户号: {}, 请求IP: {}", apiRequest.getMerchantCode(), requestIp);

        //获取商户信息
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(apiRequest.getMerchantCode());

        // 校验商户信息
        if (ObjectUtils.isEmpty(merchantInfo)) {
            log.error("查询余额退回订单接口失败, 商户信息不存在: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
            return ApiResponse.of(ApiResponseEnum.INVALID_REQUEST, null);
        }

        //获取商户公钥
        if (ObjectUtils.isEmpty(merchantInfo.getMerchantPublicKey())) {
            return ApiResponse.of(ApiResponseEnum.INVALID_MERCHANT_PUBLIC_KEY, null);
        }

        String merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();


        //校验请求
        ApiResponse apiResponse = validateRequest(apiRequest, requestIp, merchantInfo, merchantPublicKeyStr, "查询余额退回订单");
        if (apiResponse != null) {
            return apiResponse;
        }

        try {

            //商户公钥
//            merchantPublicKeyStr = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAve+UgiJRCFUrqU2PJ6B33p4BUcRnZ0X0IQZbTqqOmqMAQxjfQC6Pzl4m+IxJwxZyVYLTOVdnxADIAPHZqPRiccw2V+hZt7wUYcFGbyfMKIoumkyISJ5iqaFEv/OOu9Dz7pCGvd18OUjq1/STlLQNgKcn/tqKkLGc2/e6R6Nsy9kngtdqRN7mGs045dYA7n/dtpBR5QbGb6YjA4PmKVleDynQQ250qA++UdKjgm0Ww19jhEP8Hpc7CenEdPra06yRUFUD/a5WapQ8coURYtWBrR4uwfWrym0oC87GCYmAwIRqjJdf6pnkorxoO/IwNVh7vlbLhNCTUfJCWaiueIFM5wIDAQAB";

            PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

            //平台私钥
            PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());

            //使用平台私钥解密数据
            CheckCashBackReq checkCashBackReq = RsaUtil.decryptData(apiRequest.getEncryptedKey(), apiRequest.getEncryptedData(), platformPrivateKey, CheckCashBackReq.class);
            if (ObjectUtils.isEmpty(checkCashBackReq)) {
                log.error("查询余额退回订单接口失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}", requestIp, apiRequest, merchantInfo);
                return ApiResponse.of(ApiResponseEnum.PARAM_VALID_FAIL, null);
            }
            //手动调用验证明文参数
            Set<ConstraintViolation<CheckCashBackReq>> violations = validator.validate(checkCashBackReq);
            if (!violations.isEmpty()) {
                // 处理验证错误
                for (ConstraintViolation<CheckCashBackReq> violation : violations) {
                    log.error("查询余额退回订单接口失败, 参数校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, checkCashBackReq);
                    System.out.println(violation.getMessage());
                    return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, violation.getMessage(), null);
                }
            }

            //使用商户公钥验证签名
            if (!RsaUtil.verifySignature(checkCashBackReq, checkCashBackReq.getSign(), merchantPublicKey)) {
                log.error("查询余额退回订单接口失败, 签名校验失败: 请求ip: {}, req: {}, 商户信息: {}, 请求明文: {}", requestIp, apiRequest, merchantInfo, checkCashBackReq);
                return ApiResponse.of(ApiResponseEnum.SIGNATURE_ERROR, null);
            }

            //查询订单信息
            // @todo 查询订单逻辑
            CashBackOrder cashBackOrder = cashBackOrderService.getCashBackOrder(checkCashBackReq.getMerchantOrder());
            // 判断订单
            if (ObjectUtils.isEmpty(cashBackOrder)) {
                return ApiResponse.of(ApiResponseEnum.ORDER_NOT_FOUND, null);
            }

            //组装返回数据
            CashBackVo cashBackVo = new CashBackVo();
            cashBackVo.setMerchantOrder(cashBackOrder.getMerchantOrder());
            cashBackVo.setPlatformOrder(cashBackOrder.getPlatformOrder());
            cashBackVo.setAmount(String.valueOf(cashBackOrder.getAmount()));
            cashBackVo.setOrderStatus(String.valueOf(cashBackOrder.getOrderStatus()));
            cashBackVo.setMerchantCode(cashBackOrder.getMerchantCode());
            cashBackVo.setMerchantName(String.valueOf(System.currentTimeMillis() / 1000));
            cashBackVo.setMerchantMemberId(cashBackOrder.getMerchantMemberId());
            cashBackVo.setTimestamp(String.valueOf(System.currentTimeMillis()));

            //签名并加密数据
            EncryptedData encryptedData = RsaUtil.signAndEncryptData(cashBackVo, platformPrivateKey, merchantPublicKey);
            ApiResponseVo apiResponseVo = new ApiResponseVo();
            BeanUtils.copyProperties(encryptedData, apiResponseVo);
            apiResponseVo.setMerchantCode(merchantInfo.getCode());
            log.info("查询余额退回订单成功, 请求ip: {}, 请求明文: {}, 返回明文: {}", requestIp, checkCashBackReq, cashBackVo);
            return ApiResponse.of(ApiResponseEnum.SUCCESS, apiResponseVo);
        } catch (BadPaddingException e) {
            log.error("查询余额退回订单失败, 解密失败，无效的密文或密钥错误 e: {}", e.getMessage());
            return ApiResponse.of(ApiResponseEnum.DECRYPTION_ERROR, null);
        } catch (Exception e) {
            log.error("查询余额退回订单失败 req: {}, e: {}", apiRequest, e.getMessage());
        }

        return ApiResponse.of(ApiResponseEnum.SYSTEM_EXECUTION_ERROR, null);
    }
}
