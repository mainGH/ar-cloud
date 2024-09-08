package org.ar.wallet.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.NotifyStatusEnum;
import org.ar.wallet.entity.EncryptedData;
import org.ar.wallet.entity.MerchantCollectOrders;
import org.ar.wallet.entity.MerchantInfo;
import org.ar.wallet.entity.MerchantPaymentOrders;
import org.ar.wallet.mapper.MerchantCollectOrdersMapper;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.req.SendRechargeSuccessCallbackReq;
import org.ar.wallet.service.AsyncNotifyService;
import org.ar.wallet.service.IMerchantCollectOrdersService;
import org.ar.wallet.service.IMerchantInfoService;
import org.ar.wallet.service.IMerchantPaymentOrdersService;
import org.ar.wallet.util.DurationCalculatorUtil;
import org.ar.wallet.util.RequestUtil;
import org.ar.wallet.util.RsaUtil;
import org.ar.wallet.vo.ApiResponseVo;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncNotifyServiceImpl implements AsyncNotifyService {

    @Autowired
    private IMerchantInfoService merchantInfoService;
    private final IMerchantCollectOrdersService merchantCollectOrdersService;
    private final IMerchantPaymentOrdersService merchantPaymentOrdersService;
    private final ArProperty arProperty;
    private final RedissonUtil redissonUtil;
    private final MerchantCollectOrdersMapper merchantCollectOrdersMapper;

    /**
     * 发送 充值成功 异步回调通知
     *
     * @param orderNo
     * @param type    1 自动回调  2 手动回调
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean sendRechargeSuccessCallback(String orderNo, String type) {


        //分布式锁key ar-wallet-sendRechargeSuccessCallback+订单号
        String key = "ar-wallet-sendRechargeSuccessCallback" + orderNo;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                String info = type.equals("1") ? "MQ异步回调通知" : "手动回调通知";

                //查询代收订单 加上排他行锁
                MerchantCollectOrders merchantCollectOrders = merchantCollectOrdersMapper.selectMerchantCollectOrdersForUpdate(orderNo);

                if (merchantCollectOrders == null) {
                    log.error("发送 发送充值订单回调 " + info + " 失败: 订单不存在, 订单号: {}", orderNo);
                    return Boolean.FALSE;
                }

                //校验订单状态是否属于 未回调  回调失败  手动回调失败
                if (!NotifyStatusEnum.isUnsuccessful(merchantCollectOrders.getTradeCallbackStatus())) {
                    log.error("发送 发送充值订单回调 " + info + " 失败: 订单已经是回调成功状态, 订单号: {}", orderNo);
                    return Boolean.TRUE;
                }

                //获取商户信息
                MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(merchantCollectOrders.getMerchantCode());

                if (merchantInfo == null) {
                    log.error("发送 发送充值订单回调 " + info + " 失败: 获取商户信息失败, 订单号: {}, 商户号: {}", orderNo, merchantCollectOrders.getMerchantCode());
                    return Boolean.FALSE;
                }

                //获取商户公钥
                String merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();

                if (StringUtils.isEmpty(merchantPublicKeyStr)) {
                    log.error("发送 发送充值订单回调 " + info + " 失败: 获取商户公钥失败, 订单号: {}, 商户号: {}", orderNo, merchantCollectOrders.getMerchantCode());
                    return Boolean.FALSE;
                }

                try {
                    //商户公钥
                    PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

                    //平台私钥
                    PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());


                    //通知数据
                    SendRechargeSuccessCallbackReq sendRechargeSuccessCallbackReq = new SendRechargeSuccessCallbackReq();

                    //商户号
                    sendRechargeSuccessCallbackReq.setMerchantCode(merchantCollectOrders.getMerchantCode());

                    //商户订单号
                    sendRechargeSuccessCallbackReq.setMerchantTradeNo(merchantCollectOrders.getMerchantOrder());

                    //平台订单号
                    sendRechargeSuccessCallbackReq.setTradeNo(merchantCollectOrders.getPlatformOrder());

                    //订单金额
                    sendRechargeSuccessCallbackReq.setAmount(merchantCollectOrders.getAmount().toString());

                    //交易状态
                    sendRechargeSuccessCallbackReq.setTradeStatus(merchantCollectOrders.getOrderStatus());

                    //时间戳
                    sendRechargeSuccessCallbackReq.setTimestamp(String.valueOf(System.currentTimeMillis() / 1000));


                    //签名并加密数据
                    EncryptedData encryptedData = RsaUtil.signAndEncryptData(sendRechargeSuccessCallbackReq, platformPrivateKey, merchantPublicKey);
                    ApiResponseVo apiResponseVo = new ApiResponseVo();
                    BeanUtils.copyProperties(encryptedData, apiResponseVo);
                    apiResponseVo.setMerchantCode(merchantInfo.getCode());


                    //发送请求
                    String res = RequestUtil.HttpRestClientToJson(merchantCollectOrders.getTradeNotifyUrl(), JSON.toJSONString(apiResponseVo));

                    log.info("发送 发送充值订单回调 " + info + " 请求地址: {}, 请求明文: {}, 请求密文: {}, 返回数据: {}", merchantCollectOrders.getTradeNotifyUrl(), sendRechargeSuccessCallbackReq, apiResponseVo, res);

                    if (StringUtils.isNotEmpty(res) && "SUCCESS".equals(res)) {

                        LocalDateTime now = LocalDateTime.now();

                        //更新订单完成时间
                        merchantCollectOrders.setCompletionTime(now);

                        //交易回调状态
                        //判断是自动回调还是手动回调
                        merchantCollectOrders.setTradeCallbackStatus(
                                "1".equals(type) ? NotifyStatusEnum.SUCCESS.getCode() : NotifyStatusEnum.MANUAL_SUCCESS.getCode()
                        );

                        //交易回调时间
                        merchantCollectOrders.setTradeCallbackTime(now);

                        //计算完成时长
                        merchantCollectOrders.setCompleteDuration(DurationCalculatorUtil.orderCompleteDuration(merchantCollectOrders.getCreateTime(), now));

                        //更新订单信息
                        boolean update = merchantCollectOrdersService.updateById(merchantCollectOrders);

                        if (update) {
                            //发送回调成功
                            log.info("发送 发送充值订单回调 " + info + " 成功, 订单号: {}, 返回数据: {}", merchantCollectOrders.getPlatformOrder(), res);

                            return Boolean.TRUE;
                        } else {
                            log.info("发送 发送充值订单回调 " + info + " 成功, 更新订单信息失败, 订单号: {}, 返回数据: {}", merchantCollectOrders.getPlatformOrder(), res);
                        }
                    } else {

                        //将交易回调状态改为 回调失败
                        //交易回调状态
                        //判断是自动回调还是手动回调
                        merchantCollectOrders.setTradeCallbackStatus(
                                "1".equals(type) ? NotifyStatusEnum.FAILED.getCode() : NotifyStatusEnum.MANUAL_FAILED.getCode()
                        );

                        //更新订单信息
                        boolean update = merchantCollectOrdersService.updateById(merchantCollectOrders);

                        //发送回调失败
                        log.error("发送 发送充值订单回调 " + info + " 失败, 订单号: {}, 返回数据: {}", merchantCollectOrders.getPlatformOrder(), res);
                    }

                    return Boolean.FALSE;

                } catch (Exception e) {
                    log.error("发送 发送充值订单回调 " + info + " 失败: , 订单号: {}, 商户号: {}, e: {}", orderNo, merchantCollectOrders.getMerchantCode(), e);
                    return Boolean.FALSE;
                }

            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("发送 发送充值订单回调 失败: , 订单号: {}, e: {}", orderNo, e);
            return Boolean.FALSE;
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return Boolean.FALSE;
    }


    /**
     * 发送 提现成功 异步回调通知
     *
     * @param orderNo
     * @param type    1 自动回调  2 手动回调
     * @return {@link Boolean}
     */
    @Override
    public Boolean sendWithdrawalSuccessCallback(String orderNo, String type) {

        String info = type.equals("1") ? "MQ异步回调通知" : "手动回调通知";

        //查询代付订单
        MerchantPaymentOrders merchantPaymentOrders = merchantPaymentOrdersService.getOrderInfoByOrderNumber(orderNo);

        if (merchantPaymentOrders == null) {
            log.error("发送 提现成功 " + info + " 失败: 订单不存在, 订单号: {}", orderNo);
            return Boolean.FALSE;
        }

        //校验订单状态是否属于 未回调  回调失败  手动回调失败
        if (!NotifyStatusEnum.isUnsuccessful(merchantPaymentOrders.getTradeCallbackStatus())) {
            log.error("发送 提现成功 " + info + " 失败: 订单已经是回调成功状态, 订单号: {}", orderNo);
            return Boolean.TRUE;
        }

        //获取商户信息
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(merchantPaymentOrders.getMerchantCode());

        if (merchantInfo == null) {
            log.error("发送 提现成功 " + info + " 失败: 获取商户信息失败, 订单号: {}, 商户号: {}", orderNo, merchantPaymentOrders.getMerchantCode());
            return Boolean.FALSE;
        }

        //获取商户公钥
        String merchantPublicKeyStr = merchantInfo.getMerchantPublicKey();

        if (StringUtils.isEmpty(merchantPublicKeyStr)) {
            log.error("发送 提现成功 " + info + " 失败: 获取商户公钥失败, 订单号: {}, 商户号: {}", orderNo, merchantPaymentOrders.getMerchantCode());
            return Boolean.FALSE;
        }

        try {
            //商户公钥
            PublicKey merchantPublicKey = RsaUtil.getPublicKeyFromString(merchantPublicKeyStr);

            //平台私钥
            PrivateKey platformPrivateKey = RsaUtil.getPrivateKeyFromString(arProperty.getPrivateKey());


            //通知数据
            SendRechargeSuccessCallbackReq sendRechargeSuccessCallbackReq = new SendRechargeSuccessCallbackReq();

            //商户号
            sendRechargeSuccessCallbackReq.setMerchantCode(merchantPaymentOrders.getMerchantCode());

            //商户订单号
            sendRechargeSuccessCallbackReq.setMerchantTradeNo(merchantPaymentOrders.getMerchantOrder());

            //平台订单号
            sendRechargeSuccessCallbackReq.setTradeNo(merchantPaymentOrders.getPlatformOrder());

            //订单金额
            sendRechargeSuccessCallbackReq.setAmount(merchantPaymentOrders.getAmount().toString());

            //交易状态
            sendRechargeSuccessCallbackReq.setTradeStatus(merchantPaymentOrders.getOrderStatus());

            //时间戳
            sendRechargeSuccessCallbackReq.setTimestamp(String.valueOf(System.currentTimeMillis() / 1000));


            //签名并加密数据
            EncryptedData encryptedData = RsaUtil.signAndEncryptData(sendRechargeSuccessCallbackReq, platformPrivateKey, merchantPublicKey);
            ApiResponseVo apiResponseVo = new ApiResponseVo();
            BeanUtils.copyProperties(encryptedData, apiResponseVo);
            apiResponseVo.setMerchantCode(merchantInfo.getCode());


            //发送请求
            String res = RequestUtil.HttpRestClientToJson(merchantPaymentOrders.getTradeNotifyUrl(), JSON.toJSONString(apiResponseVo));

            log.info("发送 提现成功 " + info + " 请求地址: {}, 请求明文: {}, 请求密文: {}, 返回数据: {}", merchantPaymentOrders.getTradeNotifyUrl(), sendRechargeSuccessCallbackReq, apiResponseVo, res);

            if (StringUtils.isNotEmpty(res) && "SUCCESS".equals(res)) {

                LocalDateTime now = LocalDateTime.now();

                //更新订单完成时间
                merchantPaymentOrders.setCompletionTime(now);

                //交易回调状态
                //判断是自动回调还是手动回调
                merchantPaymentOrders.setTradeCallbackStatus(
                        "1".equals(type) ? NotifyStatusEnum.SUCCESS.getCode() : NotifyStatusEnum.MANUAL_SUCCESS.getCode()
                );

                //交易回调时间
                merchantPaymentOrders.setTradeCallbackTime(now);

                //计算完成时长
                merchantPaymentOrders.setCompleteDuration(DurationCalculatorUtil.orderCompleteDuration(merchantPaymentOrders.getCreateTime(), now));

                //更新订单信息
                boolean update = merchantPaymentOrdersService.updateById(merchantPaymentOrders);

                if (update) {
                    //发送回调成功
                    log.info("发送 提现成功 " + info + " 成功, 订单号: {}, 返回数据: {}", merchantPaymentOrders.getPlatformOrder(), res);

                    return Boolean.TRUE;
                } else {
                    log.info("发送 提现成功 " + info + " 成功, 更新订单信息失败, 订单号: {}, 返回数据: {}", merchantPaymentOrders.getPlatformOrder(), res);
                }
            } else {

                //将交易回调状态改为 回调失败

                //交易回调状态
                //判断是自动回调还是手动回调
                merchantPaymentOrders.setTradeCallbackStatus(
                        "1".equals(type) ? NotifyStatusEnum.FAILED.getCode() : NotifyStatusEnum.MANUAL_FAILED.getCode()
                );

                //更新订单信息
                boolean update = merchantPaymentOrdersService.updateById(merchantPaymentOrders);

                //发送回调失败
                log.error("发送 提现成功 " + info + " 失败, 订单号: {}, 返回数据: {}", merchantPaymentOrders.getPlatformOrder(), res);
            }

            return Boolean.FALSE;

        } catch (Exception e) {
            log.error("发送 提现成功 " + info + " 失败, 订单号: {}, 商户号: {}, e: {}", orderNo, merchantPaymentOrders.getMerchantCode(), e);
            return Boolean.FALSE;
        }
    }
}
