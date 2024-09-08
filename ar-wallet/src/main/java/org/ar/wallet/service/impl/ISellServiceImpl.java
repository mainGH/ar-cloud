package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.common.pay.req.MemberInfoCreditScoreReq;
import org.ar.common.redis.constants.RedisKeys;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.*;
import org.ar.wallet.oss.OssService;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.rabbitmq.RabbitMQService;
import org.ar.wallet.req.CancelOrderReq;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.req.SellOrderListReq;
import org.ar.wallet.req.SellReq;
import org.ar.wallet.service.*;
import org.ar.wallet.util.*;
import org.ar.wallet.vo.*;
import org.ar.wallet.webSocket.MemberSendAmountList;
import org.ar.wallet.webSocket.NotifyOrderStatusChangeSend;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ar.common.core.result.ResultCode.ORDER_STATUS_VERIFICATION_FAILED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ISellServiceImpl implements ISellService {

    private final IPaymentOrderService paymentOrderService;
    private final IMemberInfoService memberInfoService;
    private final ICollectionInfoService collectionInfoService;
    private final RedissonUtil redissonUtil;
    private final ICollectionOrderService collectionOrderService;
    private final IMatchingOrderService matchingOrderService;
    private final ITradeConfigService tradeConfigService;
    private final OssService ossService;

    private final CollectionInfoMapper collectionInfoMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final MatchPoolMapper matchPoolMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final CollectionOrderMapper collectionOrderMapper;
    private final MatchingOrderMapper matchingOrderMapper;
    private final IMemberAccountChangeService memberAccountChangeService;
    private final MemberSendAmountList memberSendAmountList;
    private final IAppealOrderService appealOrderService;
    private final AppealOrderMapper appealOrderMapper;

    private final RedisUtil redisUtil;
    private final IMemberGroupService memberGroupService;
    private final IWithdrawalCancellationService withdrawalCancellationService;
    private final RedisTemplate redisTemplate;
    //从nacos获取配置
    private final ArProperty arProperty;
    private final RabbitMQService rabbitMQService;

    @Autowired
    private IMatchPoolService matchPoolService;

    @Autowired
    private NotifyOrderStatusChangeSend notifyOrderStatusChangeSend;

    private final OrderNumberGeneratorUtil orderNumberGenerator;

    @Autowired
    private MerchantInfoServiceImpl merchantInfoService;

    @Autowired
    private ITradeIpBlacklistService tradeIpBlacklistService;

    @Value("${oss.baseUrl}")
    private String baseUrl;

    @Autowired
    private UpiTransactionService upiTransactionService;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private TradeConfigHelperUtil tradeConfigHelperUtil;

    @Autowired
    private ITransactionRewardsService transactionRewardsService;

    @Autowired
    private IMemberTaskStatusService memberTaskStatusService;

    @Autowired
    private ITaskManagerService taskManagerService;

    @Autowired
    private IControlSwitchService controlSwitchService;
    @Autowired
    private OrderChangeEventService orderChangeEventService;

    /**
     * 订单校验
     *
     * @param sellReq
     * @param memberInfo
     * @param tradeConfig
     * @param collectionInfo
     * @return {@link RestResult}
     */
    @Override
    public RestResult orderValidation(SellReq sellReq, MemberInfo memberInfo, TradeConfig tradeConfig, CollectionInfo collectionInfo) {

        //根据会员标签获取对应配置信息
        TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo);

        //判断卖出金额 是否在 最小卖出金额 和最大卖出金额之间
        OrderAmountValidationResult orderAmountValid = TradeValidationUtil.isOrderAmountValid(sellReq.getAmount(), schemeConfigByMemberTag.getSchemeMinSellAmount(), schemeConfigByMemberTag.getSchemeMaxSellAmount());

        if (orderAmountValid == OrderAmountValidationResult.TOO_LOW) {
            log.error("卖出处理失败: 订单校验失败: 卖出数量不能低于会员最小卖出金额, 卖出金额: {}, 会员最小卖出金额: {}, 会员信息: {}, req: {}", sellReq.getAmount(), schemeConfigByMemberTag.getSchemeMinSellAmount(), memberInfo, sellReq);
            //订单金额太低
            return RestResult.failure(ResultCode.SELL_AMOUNT_TOO_LOW);
        }

        if (orderAmountValid == OrderAmountValidationResult.TOO_HIGH) {
            log.error("卖出处理失败: 订单校验失败: 卖出数量超过限制, 订单金额: {}, 会员最大卖出金额: {}, 会员信息: {}, req: {}", sellReq.getAmount(), schemeConfigByMemberTag.getSchemeMaxSellAmount(), memberInfo, sellReq);
            //订单金额超过最大限制
            return RestResult.failure(ResultCode.ORDER_AMOUNT_EXCEEDS_LIMIT);
        }


        //如果有最小限额的话, 也要校验最小限额是否在最小卖出金额和最大卖出金额之间
        if (sellReq.getMinimumAmount() != null) {

            //判断卖出金额 是否在 最小卖出金额 和最大卖出金额之间
            OrderAmountValidationResult orderAmountValidMinimumAmount = TradeValidationUtil.isOrderAmountValid(sellReq.getMinimumAmount(), schemeConfigByMemberTag.getSchemeMinSellAmount(), schemeConfigByMemberTag.getSchemeMaxSellAmount());

            if (orderAmountValidMinimumAmount == OrderAmountValidationResult.TOO_LOW) {
                //订单金额太低
                log.error("卖出处理失败: 订单校验失败: 最小限额不能低于会员最小卖出金额, 卖出金额: {}, 会员最小卖出金额: {}, 会员信息: {}, req: {}", sellReq.getAmount(), schemeConfigByMemberTag.getSchemeMinSellAmount(), memberInfo, sellReq);
                return RestResult.failure(ResultCode.MINIMUM_LIMIT_TOO_LOW);
            }

            if (orderAmountValidMinimumAmount == OrderAmountValidationResult.TOO_HIGH) {
                //订单金额超过最大限制
                log.error("卖出处理失败: 订单校验失败: 最小限额超过限制, 订单金额: {}, 会员最大卖出金额: {}, 会员信息: {}, req: {}", sellReq.getAmount(), schemeConfigByMemberTag.getSchemeMaxSellAmount(), memberInfo, sellReq);
                return RestResult.failure(ResultCode.ORDER_AMOUNT_EXCEEDS_LIMIT);
            }
        }

        //校验卖出数量不能低于1
        if (sellReq.getAmount().compareTo(new BigDecimal(1)) < 0) {
            log.error("卖出处理失败: 订单校验失败: 卖出数量不能低于1, 会员信息: {}, req: {}", memberInfo, sellReq);
            return RestResult.failure(ResultCode.SELL_AMOUNT_TOO_LOW);
        }

        //校验最小限额不能低于1
        if (sellReq.getMinimumAmount() != null && sellReq.getMinimumAmount().compareTo(new BigDecimal(1)) < 0) {
            log.error("卖出处理失败: 订单校验失败: 最小限额不能低于1, 会员信息: {}, req: {}", memberInfo, sellReq);
            return RestResult.failure(ResultCode.MINIMUM_LIMIT_TOO_LOW);
        }

        //检查收款信息是否属于开启状态
        if (collectionInfo == null || CollectionInfoStatusEnum.CLOSE.getCode().equals(collectionInfo.getCollectedStatus())) {
            log.error("卖出处理失败: 订单校验失败: 收款信息未启用, 会员信息: {}, req: {}, 收款信息: {}", memberInfo, sellReq, collectionInfo);
            return RestResult.failure(ResultCode.COLLECTION_INFO_NOT_ENABLED);
        }

        //校验upi是否达到单日收款限制
        Long dailyTransactionCount = upiTransactionService.getDailyTransactionCount(collectionInfo.getUpiId());
        if (dailyTransactionCount != null && dailyTransactionCount >= tradeConfig.getMaxDailyUpiTransactions()) {
            log.error("卖出处理失败: 订单校验失败: upi已达到单日收款限制, upi今日收款次数: {}, 会员信息: {}, req: {}, 收款信息: {}", dailyTransactionCount, memberInfo, sellReq, collectionInfo);
            return RestResult.failure(ResultCode.UPI_RECEIPT_LIMIT_REACHED);
        }

        String memberId = String.valueOf(memberInfo.getId());

        //检查收款信息是否属于该会员
        if (!collectionInfo.getMemberId().equals(memberId)) {
            log.error("卖出处理失败: 订单校验失败: 非法操作-收款信息校验失败, 会员信息: {}, req: {}, 收款信息: {}", memberInfo, sellReq, collectionInfo);
            return RestResult.failure(ResultCode.ILLEGAL_OPERATION_COLLECTION_INFO_CHECK_FAILED);
        }

        //校验金额是否为整十
        if (!AmountVerifyUtil.isMultipleOfTen(sellReq.getAmount())) {
            log.error("卖出处理失败: 订单校验失败: 卖出数量必须为整十, 会员信息: {}, req: {}", memberInfo, sellReq);
            return RestResult.failure(ResultCode.SELL_AMOUNT_MUST_BE_INTEGER_10);
        }

        //校验最小限额是否为整十
        if (sellReq.getMinimumAmount() != null && !AmountVerifyUtil.isMultipleOfTen(sellReq.getMinimumAmount())) {
            log.error("卖出处理失败: 订单校验失败: 卖出数量必须为整十, 会员信息: {}, req: {}", memberInfo, sellReq);
            return RestResult.failure(ResultCode.SELL_AMOUNT_MUST_BE_INTEGER_10);
        }

        //校验最小限额不能超出卖出数量
        if (sellReq.getMinimumAmount() != null && sellReq.getMinimumAmount().compareTo(sellReq.getAmount()) > 0) {
            log.error("卖出处理失败: 订单校验失败: 最小限额不能超出卖出数量, 会员信息: {}, req: {}", memberInfo, sellReq);
            return RestResult.failure(ResultCode.MINIMUM_LIMIT_EXCEEDS_SELL_AMOUNT);
        }

        //校验卖出会员状态 是否启用
        if (MemberStatusEnum.DISABLE.getCode().equals(memberInfo.getStatus())) {
            log.error("卖出下单处理失败: 该会员状态异常 req: {}, 会员信息: {}", sellReq, memberInfo);
            return RestResult.failure(ResultCode.NO_PERMISSION);
        }

        //校验卖出会员状态 是否启用
        if (SellStatusEnum.DISABLE.getCode().equals(memberInfo.getSellStatus())) {
            log.error("卖出下单处理失败: 该会员卖出状态异常 req: {}, 会员信息: {}", sellReq, memberInfo);
            return RestResult.failure(ResultCode.MEMBER_SELL_STATUS_NOT_AVAILABLE);
        }

        //正在进行中的卖出订单列表
        List<SellOrderListVo> ongoingOrders = getOngoingSellOrdersByMemberId(String.valueOf(memberInfo.getId()));

        if (ongoingOrders != null && ongoingOrders.size() > 0) {
            //使用 Collections.sort 方法结合 Lambda 表达式进行排序
            Collections.sort(ongoingOrders, (o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()));
        }

        //计算进行中的订单数 只统计未拆单的订单
        long count = ongoingOrders.stream().filter(order -> order.getIsSplitOrder() == 0).count();

        //判断该用户同时存在订单 是否超过最大限制 (同时卖出最多订单数)
        if (count >= schemeConfigByMemberTag.getSchemeMaxSellOrderNum()) {
            log.error("卖出处理失败: 订单校验失败: 已超过订单数量最大限制, 正在进行中的卖出订单数量: {}, 配置卖出最多订单数: {}, 会员信息: {}, req: {}", count, schemeConfigByMemberTag.getSchemeMaxSellOrderNum(), memberInfo, sellReq);
            return RestResult.failure(ResultCode.EXCEEDS_MAX_ORDER_COUNT_LIMIT);
        }

        //判断该会员是钱包会员还是商户会员
        if (MemberTypeEnum.WALLET_MEMBER.getCode().equals((memberInfo.getMemberType()))) {
            //钱包会员

            //校验是否超过最大拆单数
            if (sellReq.getMinimumAmount() != null) {
                int SplitOrderCount = sellReq.getAmount().divide(sellReq.getMinimumAmount(), 0, RoundingMode.CEILING).intValue();
                if (sellReq.getMinimumAmount() != null && SplitOrderCount > tradeConfig.getMaxSplitOrderCount()) {
                    log.error("卖出处理失败: 订单校验失败: 超过最大拆单笔数, 当前拆单笔数: {}, 最大拆单笔数: {}, 会员信息: {}, req: {},", SplitOrderCount, tradeConfig.getMaxSplitOrderCount(), memberInfo, sellReq);
                    return RestResult.failure(ResultCode.EXCEEDS_MAXIMUM_SPLIT_ORDER_COUNT);
                }
            }
        }

        //判断提单金额是否大于用户余额
        if (sellReq.getAmount().compareTo(memberInfo.getBalance()) > 0) {
            log.error("卖出处理失败: 订单校验失败: 余额不足, 订单金额: {}, 会员余额: {}, 会员信息: {}, req: {}", sellReq.getAmount(), memberInfo.getBalance(), memberInfo, sellReq);
            return RestResult.failure(ResultCode.INSUFFICIENT_BALANCE);
        }

        return null;
    }

    /**
     * 卖出处理
     *
     * @param sellReq
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public RestResult<SellOrderVo> sellProcessor(SellReq sellReq, HttpServletRequest request) {

        //获取当前会员id
        Long memberId = UserContext.getCurrentUserId();

        if (memberId == null) {
            log.error("卖出处理失败: 获取会员id失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-sell+会员id
        String key = "ar-wallet-sell" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取当前会员信息 加上排他行锁
                MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(memberId);

                if (memberInfo == null) {
                    log.error("卖出处理失败: 获取会员信息失败");
                    return RestResult.failure(ResultCode.RELOGIN);
                }
                // 获取配置信息
                TradeConfig tradeConfig = tradeConfigService.getById(1);

                /*BigDecimal tradeCreditScoreLimit = tradeConfig.getTradeCreditScoreLimit();
                // 信用分判断
                if (memberInfo.getCreditScore().compareTo(tradeCreditScoreLimit) < 0) {
                    log.error("卖出处理失败: 会员信用分过低, 当前信用分:{}", memberInfo.getCreditScore());
                    return RestResult.failure(ResultCode.LOW_CREDIT_SCORE);
                }*/

                //获取卖出 ip
                String realIP = IpUtil.getRealIP(request);

                String appEnv = arProperty.getAppEnv();
                boolean isTestEnv = "sit".equals(appEnv) || "dev".equals(appEnv);

                if (!isTestEnv) {
                    //线上环境 校验ip是否在交易黑名单中
                    if (tradeIpBlacklistService.isIpBlacklisted(realIP)) {
                        log.error("卖出处理失败, 该交易ip处于黑名单列表中, 会员id: {}, 会员账号: {}, 会员信息: {}, 交易ip: {}", memberId, memberInfo.getMemberAccount(), memberInfo, realIP);
                        return RestResult.failure(ResultCode.IP_BLACKLISTED);
                    }
                }

                //校验会员有没有实名认证
//                if (StringUtils.isEmpty(memberInfo.getRealName()) || StringUtils.isEmpty(memberInfo.getIdCardNumber())) {
//                    log.error("卖出处理失败: 该会员没有实名认证 req: {}, 会员信息: {}", sellReq, memberInfo);
//                    return RestResult.failure(ResultCode.MEMBER_NOT_VERIFIED);
//                }

                //校验会员是否有卖出权限
                if (!MemberPermissionCheckerUtil.hasPermission(memberGroupService.getAuthListById(memberInfo.getMemberGroup()), MemberPermissionEnum.SELL)) {
                    log.error("卖出下单失败, 当前会员所在分组没有卖出权限, 会员账号: {}", memberInfo.getMemberAccount());
                    return RestResult.failure(ResultCode.NO_PERMISSION);
                }

                //账变前余额
                BigDecimal previousBalance = memberInfo.getBalance();

                //账变后余额
                BigDecimal newBalance = memberInfo.getBalance().subtract(sellReq.getAmount());


                //根据会员标签获取对应配置信息
                TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo);

                //获取收款卡信息 加上排他行锁
                CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(sellReq.getCollectionInfoId());

                //订单校验
                RestResult restResult = orderValidation(sellReq, memberInfo, tradeConfig, collectionInfo);
                if (restResult != null) {
                    log.error("卖出下单 订单校验失败: 会员账号: {}, sellReq: {}, 失败原因: {}", memberInfo.getMemberAccount(), JSON.toJSONString(sellReq, SerializerFeature.WriteMapNullValue), restResult.getMsg());
                    return restResult;
                }

                log.info("卖出下单 订单校验成功 会员账号: {}, req: {}", memberInfo.getMemberAccount(), JSON.toJSONString(sellReq, SerializerFeature.WriteMapNullValue));

                //更新会员: 扣除余额 (将会员余额转到到冻结金额中)、将进行中的卖出订单数+1 累计卖出次数 + 1
                memberInfoService.updatedMemberInfo(memberInfo, sellReq.getAmount());

                //更新收款信息: 今日收款金额、今日收款笔数
                collectionInfoService.addCollectionInfoQuotaAndCount(sellReq, collectionInfo);

                //生成订单号 C2C: 拆单订单号  MC: 非拆单订单号 (条件:最小限额不等于null 不等于0 不等于订单金额)
                String platformOrder = orderNumberGenerator.generateOrderNo(sellReq.getMinimumAmount() != null && sellReq.getMinimumAmount().compareTo(new BigDecimal(0)) > 0 && sellReq.getMinimumAmount().compareTo(sellReq.getAmount()) != 0 ? "C2C" : "MC");

                //添加会员账变
                memberAccountChangeService.recordMemberTransaction(String.valueOf(memberInfo.getId()), sellReq.getAmount(), MemberAccountChangeEnum.WITHDRAW.getCode(), platformOrder, previousBalance, newBalance, "");

                //匹配时间戳
                Long lastUpdateTimestamp = System.currentTimeMillis();

                //判断是否有最小限额(是否拆单)  如果有拆单 那么将订单存入匹配池  如果没有拆单 那么将订单存入卖出订单表
                if (platformOrder.startsWith("MC")) {
                    //该笔订单没有拆单 将订单存入卖出订单表
                    //生成卖出订单
                    createSellOrder(sellReq, memberInfo, platformOrder, schemeConfigByMemberTag, collectionInfo, lastUpdateTimestamp, realIP);
                } else {
                    //该笔订单有拆单 将订单存入匹配池
                    //生成匹配池订单
                    createMatchPoolOrder(sellReq, memberInfo, platformOrder, collectionInfo, lastUpdateTimestamp, schemeConfigByMemberTag, realIP);
                }

                //从配置表获取 钱包用户卖出匹配时长 并将分钟转为毫秒
                long millis = TimeUnit.MINUTES.toMillis(schemeConfigByMemberTag.getSchemeSellMatchingDuration());

                //将最后匹配时间存储到Redis 过期时间为12小时
                String redisLastMatchTimeKey = RedisKeys.ORDER_LASTMATCHTIME + platformOrder;
                redisTemplate.opsForValue().set(redisLastMatchTimeKey, lastUpdateTimestamp);
                redisTemplate.expire(redisLastMatchTimeKey, 12, TimeUnit.HOURS);

                //发送匹配超时的MQ消息
                TaskInfo taskInfo = new TaskInfo(platformOrder, TaskTypeEnum.WALLET_MEMBER_SALE_MATCH_TIMEOUT.getCode(), lastUpdateTimestamp);
                rabbitMQService.sendTimeoutTask(taskInfo, millis);

                //匹配时长
                Integer durationMinutes = schemeConfigByMemberTag.getSchemeSellMatchingDuration();

                //将匹配倒计时记录到redis 卖出订单
                redisUtil.setMatchExpireTime(platformOrder, durationMinutes);

                //执行事务同步回调
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 事务提交后执行的Redis操作
                        BuyListVo buyListVo = new BuyListVo();
                        //订单号
                        buyListVo.setPlatformOrder(platformOrder);
                        //订单金额
                        buyListVo.setAmount(sellReq.getAmount());

                        //最小限额 如果前端有传最小限额 那么就填入  否则就填订单金额
                        if (sellReq.getMinimumAmount() != null) {
                            buyListVo.setMinimumAmount(sellReq.getMinimumAmount());
                        } else {
                            buyListVo.setMinimumAmount(sellReq.getAmount());
                        }

                        //最大限额 卖出下单 订单金额就是最大限额
                        buyListVo.setMaximumAmount(sellReq.getAmount());
                        //支付方式 目前只有UPI 先写死
                        buyListVo.setPayType(PayTypeEnum.INDIAN_UPI.getCode());
                        //头像
                        buyListVo.setAvatar(memberInfo.getAvatar());
                        //会员id
                        buyListVo.setMemberId(String.valueOf(memberInfo.getId()));
                        //会员类型
                        buyListVo.setMemberType(memberInfo.getMemberType());
                        //信用分
                        buyListVo.setCreditScore(memberInfo.getCreditScore());
                        //存入redis买入金额列表
                        redisUtil.addOrderIdToList(buyListVo, "1");

                        //推送最新的 金额列表给前端
                        memberSendAmountList.send();

                        // 卖出订单事件处理
                        orderChangeEventService.processSellOrder(NotifyOrderStatusChangeMessage.builder().platformOrder(platformOrder).memberId(String.valueOf(memberInfo.getId())).type(NotificationTypeEnum.NOTIFY_SELLER.getCode()).build());

                        //将该收款upi今日收款次数+1
                        upiTransactionService.incrementDailyTransactionCountAndMarkAsProcessed(collectionInfo.getUpiId(), platformOrder);

                        // 发送计算会员等级消息
                        rabbitMQService.sendMemberUpgradeMessage(String.valueOf(memberId));
                    }
                });

                log.info("卖出下单接口处理成功, 会员账号: {}, 请求参数: {}, 卖出匹配时长(分钟): {}", memberInfo.getMemberAccount(), JSON.toJSONString(sellReq, SerializerFeature.WriteMapNullValue), schemeConfigByMemberTag.getSchemeSellMatchingDuration());

                SellOrderVo sellOrderVo = new SellOrderVo();
                sellOrderVo.setPlatformOrder(platformOrder);
                return RestResult.ok(sellOrderVo);
            } else {
                //没获取到锁 直接返回操作频繁
                return RestResult.failure(ResultCode.TOO_FREQUENT);
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("卖出下单接口处理失败 会员id: {}, req: {}, e: {}", memberId, sellReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 根据会员ID 获取当前正在进行中的订单(不去重)
     *
     * @param memberId
     * @return {@link List}<{@link SellOrderListVo}>
     */
    @Override
    public List<SellOrderListVo> getOngoingSellOrdersByMemberId(String memberId) {

        List<SellOrderListVo> sellOrderListVoList = new ArrayList<>();

        List<MatchPool> MatchPoolList = new ArrayList<>();
        //查询匹配池中 状态为 匹配中 进行中 匹配超时的订单
        MatchPoolList = matchPoolService.getOngoingSellOrder(memberId);

        for (MatchPool matchPool : MatchPoolList) {

            //判断如果订单处于 进行中状态, 那么查询子订单是否还有未完成的订单
            if (matchPool.getOrderStatus().equals(OrderStatusEnum.IN_PROGRESS.getCode())) {
                //查询匹配池订单下面的子订单 并根据子订单状态 更新匹配池订单状态
                updateMatchPoolOrderStatus(matchPool.getMatchOrder());
            }

            SellOrderListVo sellOrderListVo = new SellOrderListVo();
            BeanUtil.copyProperties(matchPool, sellOrderListVo);

            // 查看如果是手动完成的状态 改为 已完成状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }

            //匹配剩余时间
            sellOrderListVo.setMatchExpireTime(redisUtil.getMatchRemainingTime(matchPool.getMatchOrder()));
            //确认中剩余时间
            sellOrderListVo.setConfirmExpireTime(redisUtil.getConfirmRemainingTime(matchPool.getMatchOrder()));
            //待支付剩余时间
            sellOrderListVo.setPaymentExpireTime(redisUtil.getPaymentRemainingTime(matchPool.getMatchOrder()));

            //设置订单号
            sellOrderListVo.setPlatformOrder(matchPool.getMatchOrder());

            //设置为母订单
            sellOrderListVo.setIsParentOrder(1);

            //优化超时显示
            //判断如果订单是匹配中状态, 但是匹配剩余时间低于0 那么将返回前端的订单状态改为匹配超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode()) && (sellOrderListVo.getMatchExpireTime() == null || sellOrderListVo.getMatchExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
            }

            //判断如果订单是确认中状态, 但是确认剩余时间低于0 那么将返回前端的订单状态改为确认超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) && (sellOrderListVo.getConfirmExpireTime() == null || sellOrderListVo.getConfirmExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
            }

            //判断如果订单是支付中状态, 但是支付剩余时间低于0 那么将返回前端的订单状态改为支付超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode()) && (sellOrderListVo.getPaymentExpireTime() == null || sellOrderListVo.getPaymentExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());
            }

            sellOrderListVoList.add(sellOrderListVo);
        }

        //查询卖出表中 正在进行中的订单
        List<PaymentOrder> paymentOrderList = new ArrayList<>();
        paymentOrderList = paymentOrderService.ongoingSellOrders(memberId);

        for (PaymentOrder paymentOrder : paymentOrderList) {
            SellOrderListVo sellOrderListVo = new SellOrderListVo();
            BeanUtil.copyProperties(paymentOrder, sellOrderListVo);

            //判断如果是手动完成状态 就改为已完成状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }

            //匹配剩余时间
            sellOrderListVo.setMatchExpireTime(redisUtil.getMatchRemainingTime(paymentOrder.getPlatformOrder()));
            //确认中剩余时间
            sellOrderListVo.setConfirmExpireTime(redisUtil.getConfirmRemainingTime(paymentOrder.getPlatformOrder()));
            //待支付剩余时间
            sellOrderListVo.setPaymentExpireTime(redisUtil.getPaymentRemainingTime(paymentOrder.getPlatformOrder()));

            //是否是拆单 如果是拆单就不计算到 进行中的订单数量
            if (StringUtils.isNotEmpty(paymentOrder.getMatchOrder())) {
                //是拆单 该笔订单不计算到进行中的订单数量中
                sellOrderListVo.setIsSplitOrder(1);
            }

            //优化超时显示
            //判断如果订单是匹配中状态, 但是匹配剩余时间低于0 那么将返回前端的订单状态改为匹配超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode()) && (sellOrderListVo.getMatchExpireTime() == null || sellOrderListVo.getMatchExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
            }

            //判断如果订单是确认中状态, 但是确认剩余时间低于0 那么将返回前端的订单状态改为确认超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) && (sellOrderListVo.getConfirmExpireTime() == null || sellOrderListVo.getConfirmExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
            }

            //判断如果订单是支付中状态, 但是支付剩余时间低于0 那么将返回前端的订单状态改为支付超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode()) && (sellOrderListVo.getPaymentExpireTime() == null || sellOrderListVo.getPaymentExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());
            }

            // 设置 人工审核 状态
            if(OrderStatusEnum.isAuditing(paymentOrder.getOrderStatus(), paymentOrder.getAuditDelayTime())){
                sellOrderListVo.setOrderStatus(OrderStatusEnum.AUDITING.getCode());
            }

            sellOrderListVoList.add(sellOrderListVo);
        }

        Iterator<SellOrderListVo> iterator = sellOrderListVoList.iterator();
        while (iterator.hasNext()) {
            SellOrderListVo sellOrderListVo = iterator.next();
            // 如果是母订单 并且状态是进行中
            if (sellOrderListVo.getIsParentOrder() == 1 && sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.IN_PROGRESS.getCode())) {

                boolean hasChildOrders = false;
                // 检查是否有子订单
                for (SellOrderListVo order : sellOrderListVoList) {
                    //过滤母订单
                    if (order.getIsParentOrder() == 0) {
                        if (order.getMatchOrder() != null && order.getMatchOrder().equals(sellOrderListVo.getPlatformOrder())) {
                            hasChildOrders = true;
                            break;
                        }
                    }
                }
                // 如果没有子订单，则移除母订单
                if (!hasChildOrders) {
                    iterator.remove();
                }
            }
        }
        return sellOrderListVoList;
    }

    @Override
    public List<SellProcessingOrderListVo> processingSellOrderList(Long memberId, boolean getFromRedis) {
        List<SellProcessingOrderListVo> sellOrderListVoList = new ArrayList<>();
        List<MatchPool> matchPoolList;
        List<PaymentOrder> paymentOrderList;
        if(ObjectUtils.isEmpty(arProperty.getProcessOrderSource())
                || arProperty.getProcessOrderSource() == 1
                || !getFromRedis
        ){
            matchPoolList = matchPoolService.getProcessingOrderByMemberId(String.valueOf(memberId));
            paymentOrderList = paymentOrderService.getProcessingOrderByMemberId(String.valueOf(memberId));
        }else{
            // 从redis中获取进行中的订单号信息
            List<String> memberProcessingOrder = redisUtil.getMemberProcessingOrder(String.valueOf(memberId));
            if(ObjectUtils.isEmpty(memberProcessingOrder)){
                return Collections.emptyList();
            }
            matchPoolList = matchPoolService.getSellOrderList(memberProcessingOrder);
            paymentOrderList = paymentOrderService.getOrderListByPlatformOrderList(memberProcessingOrder);
        }

        MemberInfo memberInfo = memberInfoService.getMemberInfoById(String.valueOf(memberId));
        TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo);
        TradeConfig tradeConfig = tradeConfigService.getById(1);
        // 匹配时长配置
        Integer matchingDuration = schemeConfigByMemberTag.getSchemeSellMatchingDuration();
        // 确认时长配置
        Integer confirmDuration = schemeConfigByMemberTag.getSchemeConfirmExpirationTime();
        // 待支付时常配置
        Integer rechargeExpirationDuration = tradeConfig.getRechargeExpirationTime();

        for (MatchPool matchPool : matchPoolList) {

            //判断如果订单处于 进行中状态, 那么查询子订单是否还有未完成的订单
            if (matchPool.getOrderStatus().equals(OrderStatusEnum.IN_PROGRESS.getCode())) {
                //查询匹配池订单下面的子订单 并根据子订单状态 更新匹配池订单状态
                updateMatchPoolOrderStatus(matchPool.getMatchOrder());
            }

            SellProcessingOrderListVo sellOrderListVo = new SellProcessingOrderListVo();
            BeanUtil.copyProperties(matchPool, sellOrderListVo);

            // 查看如果是手动完成的状态 改为 已完成状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }

            Long countdown = null;
            Long countdownLimit = null;
            //设置订单号
            sellOrderListVo.setPlatformOrder(matchPool.getMatchOrder());

            //设置为母订单
            sellOrderListVo.setIsParentOrder(1);

            //优化超时显示
            //判断如果订单是匹配中状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode())) {
                long matchRemainingTime = redisUtil.getMatchRemainingTime(matchPool.getMatchOrder());
                countdown = matchRemainingTime < 1 ? 0 : matchRemainingTime * 1000L;
                countdownLimit = matchingDuration * 60 * 1000L;
                if(countdown < 1){
                    sellOrderListVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
                }
            }

            //判断如果订单是确认中状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode())) {
                long confirmRemainingTime = redisUtil.getConfirmRemainingTime(matchPool.getMatchOrder());
                countdown = confirmRemainingTime < 1 ? 0 : confirmRemainingTime * 1000L;
                countdownLimit = confirmDuration * 60 * 1000L;
                if(countdown < 1){
                    sellOrderListVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());;
                }
            }

            //判断如果订单是支付中状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode())) {
                long paymentRemainingTime = redisUtil.getPaymentRemainingTime(matchPool.getMatchOrder());
                countdown = paymentRemainingTime < 1 ? 0 : paymentRemainingTime * 1000L;
                countdownLimit = rechargeExpirationDuration * 60 * 1000L;
                if(countdown < 1){
                    continue;
                }
            }
            sellOrderListVo.setCountdown(countdown);
            sellOrderListVo.setCountdownLimit(countdownLimit);
            sellOrderListVoList.add(sellOrderListVo);
        }

        for (PaymentOrder paymentOrder : paymentOrderList) {
            SellProcessingOrderListVo sellOrderListVo = new SellProcessingOrderListVo();
            BeanUtil.copyProperties(paymentOrder, sellOrderListVo);

            //判断如果是手动完成状态 就改为已完成状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }
            Long countdown = null;
            Long countdownLimit = null;
            //是否是拆单 如果是拆单就不计算到 进行中的订单数量
            if (StringUtils.isNotEmpty(paymentOrder.getMatchOrder())) {
                //是拆单 该笔订单不计算到进行中的订单数量中
                sellOrderListVo.setIsSplitOrder(1);
            }


            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode())) {
                long matchRemainingTime = redisUtil.getMatchRemainingTime(paymentOrder.getPlatformOrder());
                countdown = matchRemainingTime < 1 ? 0 : matchRemainingTime * 1000L;
                countdownLimit = matchingDuration * 60 * 1000L;
                if(countdown < 1){
                    sellOrderListVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
                }
            }

            //判断如果订单是确认中状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode())) {
                long confirmRemainingTime = redisUtil.getConfirmRemainingTime(paymentOrder.getPlatformOrder());
                countdown = confirmRemainingTime < 1 ? 0 : confirmRemainingTime * 1000L;
                countdownLimit = confirmDuration * 60 * 1000L;
                if(countdown < 1){
                    sellOrderListVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());;
                }
            }

            //判断如果订单是支付中状态
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode())) {
                long paymentRemainingTime = redisUtil.getPaymentRemainingTime(paymentOrder.getPlatformOrder());
                countdown = paymentRemainingTime < 1 ? 0 : paymentRemainingTime * 1000L;
                countdownLimit = rechargeExpirationDuration * 60 * 1000L;
                if(countdown < 1){
                    continue;
                }
            }
            // 人工审核状态添加
            if(OrderStatusEnum.isAuditing(paymentOrder.getOrderStatus(), paymentOrder.getAuditDelayTime())){
                sellOrderListVo.setOrderStatus(OrderStatusEnum.AUDITING.getCode());
            }
            sellOrderListVo.setCountdown(countdown);
            sellOrderListVo.setCountdownLimit(countdownLimit);
            sellOrderListVoList.add(sellOrderListVo);
        }

        Iterator<SellProcessingOrderListVo> iterator = sellOrderListVoList.iterator();
        while (iterator.hasNext()) {
            SellProcessingOrderListVo sellOrderListVo = iterator.next();
            // 如果是母订单 并且状态是进行中
            if (sellOrderListVo.getIsParentOrder() == 1 && sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.IN_PROGRESS.getCode())) {

                boolean hasChildOrders = false;
                // 检查是否有子订单
                for (SellProcessingOrderListVo order : sellOrderListVoList) {
                    //过滤母订单
                    if (order.getIsParentOrder() == 0) {
                        if (order.getMatchOrder() != null && order.getMatchOrder().equals(sellOrderListVo.getPlatformOrder())) {
                            hasChildOrders = true;
                            break;
                        }
                    }
                }
                // 如果没有子订单，则移除母订单
                if (!hasChildOrders) {
                    iterator.remove();
                }
            }
        }
        return sellOrderListVoList;
    }

    /**
     * 生成卖出订单
     *
     * @param sellReq
     * @param memberInfo
     * @param platformOrder
     * @param schemeConfigByMemberTag
     * @param collectionInfo
     * @param lastUpdateTimestamp
     * @param realIP
     * @return {@link Boolean}
     */
    @Override
    public Boolean createSellOrder(SellReq sellReq, MemberInfo memberInfo, String platformOrder, TradeConfigScheme schemeConfigByMemberTag, CollectionInfo collectionInfo, Long lastUpdateTimestamp, String realIP) {
        //生成卖出订单信息
        PaymentOrder paymentOrder = new PaymentOrder();
        BeanUtils.copyProperties(sellReq, paymentOrder);

        //判断如果是商户会员 那么加上商户名称
        if (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberInfo.getMemberType())) {
            MerchantInfo merchantInfoByCode = merchantInfoService.getMerchantInfoByCode(memberInfo.getMerchantCode());
            if (merchantInfoByCode != null) {
                paymentOrder.setMerchantCode(merchantInfoByCode.getCode());
                paymentOrder.setMerchantName(merchantInfoByCode.getUsername());
            }
        }

        //预计匹配时间
        paymentOrder.setEstimatedMatchTime(schemeConfigByMemberTag.getSchemeSellMatchingDuration());

        //设置会员头像
        paymentOrder.setAvatar(memberInfo.getAvatar());

        //设置会员ID
        paymentOrder.setMemberId(String.valueOf(memberInfo.getId()));

        //设置会员账号
        paymentOrder.setMemberAccount(memberInfo.getMemberAccount());

        //生成平台订单号
        paymentOrder.setPlatformOrder(platformOrder);

        //设置实际金额 (默认订单金额就是实际金额)
        paymentOrder.setActualAmount(paymentOrder.getAmount());

        //设置UPI_ID
        paymentOrder.setUpiId(collectionInfo.getUpiId());

        //设置UPI_Name
        paymentOrder.setUpiName(collectionInfo.getUpiName());

        //设置会员手机号
        paymentOrder.setMobileNumber(memberInfo.getMobileNumber());

        //设置交易ip
        paymentOrder.setClientIp(realIP);

        //生成奖励
        //查看会员如果有单独配置卖出奖励 那么就读取单独配置的卖出奖励
        if (memberInfo.getSellBonusProportion() != null && memberInfo.getSellBonusProportion().compareTo(new BigDecimal(0)) > 0) {
            paymentOrder.setBonus(sellReq.getAmount().multiply((new BigDecimal(memberInfo.getSellBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
            log.info("卖出处理 生成卖出订单: 会员卖出奖励(该会员单独配置的奖励): {}", paymentOrder.getBonus());
        } else {
            //判断该会员是钱包会员还是商户会员
            if (MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberInfo.getMemberType())) {
                //钱包会员
                //会员没有单独配置卖出奖励, 获取配置表奖励比例 并计算出改笔订单奖励值
                if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                    paymentOrder.setBonus(sellReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                    log.info("卖出处理 生成卖出订单: 会员卖出奖励(后台配置表奖励): {}", paymentOrder.getBonus());
                }
            } else {
                //判断该商户是否单独配置了奖励比例 如果是的话 就直接取该商户单独配置的奖励

                //获取商户信息
                MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(memberInfo.getMerchantCode());

                if (merchantInfo != null) {

                    //该商户单独配置的卖出奖励不为null并且大于0
                    if (merchantInfo.getWithdrawalRewards() != null && merchantInfo.getWithdrawalRewards().compareTo(new BigDecimal(0)) > 0) {
                        paymentOrder.setBonus(sellReq.getAmount().multiply((new BigDecimal(merchantInfo.getWithdrawalRewards().toString()).divide(BigDecimal.valueOf(100)))));
                    } else {
                        //商户会员 该商户没有单独配置卖出奖励 那么读取默认奖励
                        if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                            paymentOrder.setBonus(sellReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                            log.info("卖出处理 生成卖出订单: 会员卖出奖励(后台配置表奖励): {}", paymentOrder.getBonus());
                        }
                    }
                } else {
                    //就算商户不存在 也要按默认配置取计算奖励
                    if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                        paymentOrder.setBonus(sellReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                        log.info("卖出处理 生成卖出订单: 会员卖出奖励(后台配置表奖励): {}", paymentOrder.getBonus());
                    }
                }
            }
        }

        //设置收款信息id
        paymentOrder.setCollectionInfoId(collectionInfo.getId());

        //设置匹配时间戳
        paymentOrder.setLastUpdateTimestamp(lastUpdateTimestamp);

        boolean save = paymentOrderService.save(paymentOrder);

        log.info("卖出下单 生成卖出订单: {}, sql执行结果: {}", paymentOrder, save);

        return save;
    }

    /**
     * 生成匹配池订单
     *
     * @param sellReq
     * @param memberInfo
     * @param platformOrder
     * @param collectionInfo
     * @param lastUpdateTimestamp
     * @param schemeConfigByMemberTag
     * @param realIP
     * @return {@link Boolean}
     */
    @Override
    public Boolean createMatchPoolOrder(SellReq sellReq, MemberInfo memberInfo, String platformOrder, CollectionInfo collectionInfo, Long lastUpdateTimestamp, TradeConfigScheme schemeConfigByMemberTag, String realIP) {
        MatchPool matchPool = new MatchPool();
        BeanUtils.copyProperties(sellReq, matchPool);

        //判断如果是商户会员 那么加上商户名称
        if (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberInfo.getMemberType())) {
            MerchantInfo merchantInfoByCode = merchantInfoService.getMerchantInfoByCode(memberInfo.getMerchantCode());
            if (merchantInfoByCode != null) {
                matchPool.setMerchantCode(merchantInfoByCode.getCode());
                matchPool.setMerchantName(merchantInfoByCode.getUsername());
            }
        }

        //预计匹配时间
        matchPool.setEstimatedMatchTime(schemeConfigByMemberTag.getSchemeSellMatchingDuration());

        //设置会员头像
        matchPool.setAvatar(memberInfo.getAvatar());

        //设置会员ID
        matchPool.setMemberId(String.valueOf(memberInfo.getId()));

        //设置会员账号
        matchPool.setMemberAccount(memberInfo.getMemberAccount());

        //生成匹配订单号
        matchPool.setMatchOrder(platformOrder);

        //设置剩余金额
        matchPool.setRemainingAmount(sellReq.getAmount());

        //生成UPI_ID
        matchPool.setUpiId(collectionInfo.getUpiId());

        //生成UPI_Name
        matchPool.setUpiName(collectionInfo.getUpiName());

        //设置匹配时间戳
        matchPool.setLastUpdateTimestamp(lastUpdateTimestamp);

        //设置最大金额
        matchPool.setMaximumAmount(matchPool.getRemainingAmount());

        //设置收款信息id
        matchPool.setCollectionInfoId(collectionInfo.getId());

        //设置会员手机号
        matchPool.setMobileNumber(memberInfo.getMobileNumber());

        //设置交易ip
        matchPool.setClientIp(realIP);

        //生成奖励
        //查看会员如果有单独配置卖出奖励 那么就读取单独配置的卖出奖励
        if (memberInfo.getSellBonusProportion() != null && memberInfo.getSellBonusProportion().compareTo(new BigDecimal(0)) > 0) {
            matchPool.setBonus(sellReq.getAmount().multiply((new BigDecimal(memberInfo.getSellBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
            log.info("卖出处理 生成匹配池订单: 会员卖出奖励(该会员单独配置的奖励): {}", matchPool.getBonus());
        } else {
            //判断该会员是钱包会员还是商户会员
            if (MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberInfo.getMemberType())) {
                //钱包会员
                //会员没有单独配置卖出奖励, 获取配置表奖励比例 并计算出改笔订单奖励值
                if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                    matchPool.setBonus(sellReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                    log.info("卖出处理 生成匹配池订单: 会员卖出奖励(后台配置表奖励): {}", matchPool.getBonus());
                }
            } else {
                //判断该商户是否单独配置了奖励比例 如果是的话 就直接取该商户单独配置的奖励

                //获取商户信息
                MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(memberInfo.getMerchantCode());

                if (merchantInfo != null) {

                    //该商户单独配置的卖出奖励不为null并且大于0
                    if (merchantInfo.getWithdrawalRewards() != null && merchantInfo.getWithdrawalRewards().compareTo(new BigDecimal(0)) > 0) {
                        matchPool.setBonus(sellReq.getAmount().multiply((new BigDecimal(merchantInfo.getWithdrawalRewards().toString()).divide(BigDecimal.valueOf(100)))));
                    } else {
                        //商户会员 该商户没有单独配置卖出奖励 那么读取默认奖励
                        if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                            matchPool.setBonus(sellReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                            log.info("卖出处理 生成匹配池订单: 会员卖出奖励(后台配置表奖励): {}", matchPool.getBonus());
                        }
                    }
                } else {
                    //就算商户不存在 也要按默认配置取计算奖励
                    if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                        matchPool.setBonus(sellReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                        log.info("卖出处理 生成匹配池订单: 会员卖出奖励(后台配置表奖励): {}", matchPool.getBonus());
                    }
                }
            }
        }

        boolean save = matchPoolService.save(matchPool);

        log.info("卖出下单 生成匹配池订单: {}, sql执行结果: {}", matchPool, save);

        return save;
    }

    /**
     * 交易成功处理 (钱包用户)
     *
     * @param sellPlatformOrder
     * @return {@link RestResult}<{@link BuyCompletedVo}>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public RestResult<BuyCompletedVo> transactionSuccessHandler(String sellPlatformOrder, Long memberId, PaymentOrder payOrder, CollectionOrder colOrder, String type, String paymentPassword) {

        //获取当前会员id
        AssertUtil.notEmpty(memberId, ResultCode.RELOGIN);

        //分布式锁key ar-wallet-transactionSuccessHandler+订单号
        String key = "ar-wallet-transactionSuccessHandler" + sellPlatformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                boolean isSplitOrder = false;

                String buyMemberId = null;

                MatchPool matchPool = null;

                //获取卖出订单 加排他行锁
                PaymentOrder paymentOrder;
                if (ObjectUtils.isNotEmpty(payOrder)) {
                    paymentOrder = payOrder;
                } else {
                    paymentOrder = paymentOrderMapper.selectPaymentForUpdate(sellPlatformOrder);
                }

                //判断该笔订单是否属于该会员
                if (paymentOrder == null || !paymentOrder.getMemberId().equals(String.valueOf(memberId))) {
                    log.error("确认到账 处理失败: 该订单不存在或该订单不属于该会员 会员id: {}, 订单信息: {}", memberId, paymentOrder);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //校验该笔订单是否处于确认中、申诉中、确认超时、金额错误状态
                if (!(OrderStatusEnum.CONFIRMATION.getCode().equals(paymentOrder.getOrderStatus()) || OrderStatusEnum.COMPLAINT.getCode().equals(paymentOrder.getOrderStatus()) || OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(paymentOrder.getOrderStatus()) || OrderStatusEnum.AMOUNT_ERROR.getCode().equals(paymentOrder.getOrderStatus()))) {
                    log.error("确认到账 处理失败: 非法操作, 订单状态必须为确认中或申诉中: 会员id: {}, 当前订单状态: {}, 订单信息: {}", memberId, paymentOrder.getOrderStatus(), paymentOrder);
                    return RestResult.failure(ORDER_STATUS_VERIFICATION_FAILED);
                }

                //校验支付密码 (如果是前台请求过来的才进行校验支付密码)
                //1表示前台请求
                if ("1".equals(type)) {

                    //获取会员信息
                    MemberInfo currentMemberInfo = memberInfoService.getById(memberId);

                    if (currentMemberInfo == null) {
                        log.error("确认到账处理失败: 获取会员信息失败");
                        return RestResult.failure(ResultCode.RELOGIN);
                    }

                    //校验支付密码
                    if (!passwordEncoder.matches(paymentPassword, currentMemberInfo.getPaymentPassword())) {
                        log.error("收银台 确认支付 接口处理失败: 支付密码错误: 会员信息: {}, 订单号: {}, 支付密码: {}", currentMemberInfo, sellPlatformOrder, paymentPassword);
                        return RestResult.failure(ResultCode.PASSWORD_VERIFICATION_FAILED);
                    }
                }
                // 人工审核流程
                TradeConfig tradeConfig = tradeConfigService.getById(1);
                //获取撮合列表订单 加排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(paymentOrder.getMatchingPlatformOrder());
                if ("1".equals(type) && tradeConfig.getIsManualReview() == 1) {
                    if (matchingOrder.getAuditDelayTime() == null) {
                        BuyCompletedVo buyCompletedVo = auditMatchingOrder(matchingOrder, colOrder, paymentOrder, tradeConfig.getManualReviewTime());
                        return RestResult.ok(buyCompletedVo);
                    } else {
                        log.error("确认到账 处理失败: 非法操作, 订单状态不能为人工审核 会员id: {}, 订单信息: {}", memberId, paymentOrder);
                        return RestResult.failure(ORDER_STATUS_VERIFICATION_FAILED);
                    }
                }

                //查看是否有申诉订单, 如果有的话 要把申诉订单关掉

                //如果卖出订单是申诉中状态, 那么要把申诉订单进行关闭 (改为2: 已支付)
                if (OrderStatusEnum.COMPLAINT.getCode().equals(paymentOrder.getOrderStatus())) {

                    //查询申诉订单 加上排他行锁
                    AppealOrder appealOrder = appealOrderMapper.selectAppealOrderBywithdrawOrderNoForUpdate(paymentOrder.getPlatformOrder());

                    log.info("确认到账, 订单是申诉中状态, 将申诉订单改为已支付, 申诉订单信息: {}, 卖出订单信息: {}", appealOrder, paymentOrder);

                    String appealOrderStatus = String.valueOf(appealOrder.getAppealStatus());
                    // 1是待处理
                    if (appealOrder != null) {

                        if ("1".equals(appealOrderStatus) || "4".equals(appealOrderStatus)) {
                            //将申诉订单改为 已支付
                            UpdateWrapper<AppealOrder> appealOrderUpdateWrapper = new UpdateWrapper<>();

                            appealOrderUpdateWrapper.eq("withdraw_order_no", paymentOrder.getPlatformOrder()); // 使用卖出订单号作为更新条件
                            appealOrderUpdateWrapper.set("appeal_status", "2"); // 设置申诉订单的状态为已支付

                            // 执行更新
                            int updateAppealOrder = appealOrderMapper.update(null, appealOrderUpdateWrapper);

                            log.info("确认到账, 订单是申诉中状态, 将申诉订单改为已支付, 卖出订单信息: {}, sql执行结果: {}", paymentOrder, updateAppealOrder);
                        } else {
                            log.error("确认到账, 订单是申诉中状态, 获取申诉订单失败, 卖出订单信息: {}", paymentOrder);
                        }
                    } else {
                        log.error("确认到账, 订单是申诉中状态, 获取申诉订单失败, 卖出订单信息: {}", paymentOrder);
                    }
                }

                //更新卖出订单-----
                updatePaymentOrder(paymentOrder, type);


                //更新撮合列表订单-----
                updateMatchingOrder(matchingOrder, type);

                //获取买入订单 加排他行锁
                CollectionOrder collectionOrder;
                if (ObjectUtils.isNotEmpty(colOrder)) {
                    collectionOrder = colOrder;
                } else {
                    collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(matchingOrder.getCollectionPlatformOrder());
                }

                //更新买入订单-----
                updateCollectionOrder(collectionOrder, type);

                //获取买入会员信息(加锁)
                MemberInfo buyMemberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(collectionOrder.getMemberId()));
                buyMemberId = String.valueOf(buyMemberInfo.getId());
                //更新买入会员信息
                updateBuyMemberInfo(buyMemberInfo, collectionOrder);

                //获取卖出会员信息 加排他行锁 扣除会员冻结金额 如果会员有卖出奖励 那么就给会员增加奖励到余额里面
                MemberInfo sellMemberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(paymentOrder.getMemberId()));
                //更新卖出会员信息
                updateSellMemberInfo(sellMemberInfo, paymentOrder);

                //判断是否拆单 如果有拆单 那么更新匹配池数据
                if (StringUtils.isNotEmpty(paymentOrder.getMatchOrder())) {

                    //卖出订单号的匹配订单号不为空 说明该笔订单有拆单
                    isSplitOrder = true;

                    //查询到该笔匹配池订单 加上排他行锁
                    matchPool = matchPoolMapper.selectMatchPoolForUpdate(paymentOrder.getMatchOrder());

                    if (matchPool != null) {
                        //更新匹配池订单信息
                        updatematchPool(matchPool);
                    }
                }

                //获取收款信息 加排他行锁
                CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(paymentOrder.getCollectionInfoId());
                //更新收款信息
                updateCollectionInfo(collectionInfo, collectionOrder);


                //判断是否开启了总任务开关
                if (controlSwitchService.isSwitchEnabled(SwitchIdEnum.CHECK_ACTIVE_TASKS.getSwitchId())) {

                    //获取买入 卖出任务列表
                    Map<String, List<TaskManager>> taskListMap = taskManagerService.fetchBuyAndSellTaskList();

                    if (taskListMap != null) {

                        //获取所有开启的买入任务
                        List<TaskManager> buyTaskList = taskListMap.get("buy");

                        if (buyTaskList != null && buyTaskList.size() > 0) {

                            for (TaskManager buyTaskManager : buyTaskList) {
                                //处理买入会员的今日买入任务 (如果满足了条件就自动完成)
                                boolean handleDailyTaskBuy = memberTaskStatusService.handleDailyBuyTask(buyMemberInfo, buyTaskManager);

                                if (!handleDailyTaskBuy) {
                                    log.error("买入任务处理失败, 买入订单号: {}, 卖出订单号: {}", collectionOrder.getPlatformOrder(), paymentOrder.getPlatformOrder());
                                    //每日任务处理失败, 手动抛出异常进行回滚
                                    throw new RuntimeException();
                                }
                            }
                        } else {
                            log.info("交易成功处理, 买入任务活动未开启, 买入订单号: {}, 卖出订单号: {}", collectionOrder.getPlatformOrder(), paymentOrder.getPlatformOrder());
                        }

                        //获取所有开启的卖出任务
                        List<TaskManager> sellTaskList = taskListMap.get("sell");

                        if (sellTaskList != null && sellTaskList.size() > 0) {

                            for (TaskManager sellTaskManager : sellTaskList) {
                                //处理卖出会员的今日卖出任务  (如果满足了条件就自动完成)
                                boolean handleDailyTaskSell = memberTaskStatusService.handleDailySellTask(sellMemberInfo, sellTaskManager);

                                if (!handleDailyTaskSell) {
                                    //每日任务处理失败, 手动抛出异常进行回滚
                                    throw new RuntimeException();
                                }
                            }
                        } else {
                            log.info("交易成功处理, 卖出任务活动未开启, 买入订单号: {}, 卖出订单号: {}", collectionOrder.getPlatformOrder(), paymentOrder.getPlatformOrder());
                        }
                    }
                } else {
                    log.info("交易成功处理, 总任务活动开关未开启, 买入订单号: {}, 卖出订单号: {}", collectionOrder.getPlatformOrder(), paymentOrder.getPlatformOrder());
                }

                log.info("交易成功处理, 更新信用分数, 买家:{}, 卖家:{}", collectionOrder.getMemberId(), paymentOrder.getMemberId());
                // 更新买家信用分
                memberInfoService.updateCreditScore(MemberInfoCreditScoreReq.builder().id(Long.valueOf(collectionOrder.getMemberId())).eventType(CreditEventTypeEnum.AUTO_DONE.getCode()).tradeType(1).build());
                // 更新卖家信用分
                memberInfoService.updateCreditScore(MemberInfoCreditScoreReq.builder().id(Long.valueOf(paymentOrder.getMemberId())).eventType(CreditEventTypeEnum.MANUAL_DONE.getCode()).tradeType(2).build());


                log.info("确认到账, 交易成功处理 成功: 买入会员账号: {}, 卖出会员账号: {}, 买入订单号: {}, 卖出订单号: {}, 撮合列表订单号: {}", buyMemberInfo.getMemberAccount(), sellMemberInfo.getMemberAccount(), collectionOrder.getPlatformOrder(), paymentOrder.getPlatformOrder(), matchingOrder.getPlatformOrder());

                //奖励
                BuyCompletedVo buyCompletedVo = new BuyCompletedVo();
                buyCompletedVo.setBonus(paymentOrder.getBonus());

                //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                final String finalCollectionplatformOrder = collectionOrder.getPlatformOrder();
                final String finalMatchOrder = paymentOrder.getMatchOrder();
                final boolean finalIsSplitOrder = isSplitOrder;
                final String finalBuyMemberId = buyMemberId;
                final MatchPool finalMatchPool = matchPool;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        //清空买方连续买入失败次数
                        redisUtil.clearMemberBuyFailureAndCooldown(finalBuyMemberId);

                        //清空卖方连续买入失败次数
                        if (paymentOrder != null) {
                            redisUtil.clearMemberBuyFailureAndCooldown(paymentOrder.getMemberId());
                        }

                        //发送交易成功的通知给前端
                        NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage = new NotifyOrderStatusChangeMessage(finalBuyMemberId, NotificationTypeEnum.NOTIFY_BUYER.getCode(), finalCollectionplatformOrder);
                        notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);
                        // 人工审核的订单也推送给卖方
                        if (matchingOrder.getAuditDelayTime() != null) {
                            notifyOrderStatusChangeSend.send(new NotifyOrderStatusChangeMessage(matchingOrder.getPaymentMemberId(), NotificationTypeEnum.NOTIFY_SELLER.getCode(), matchingOrder.getPaymentPlatformOrder()));
                        }

                        // 发送计算买家等级消息
                        rabbitMQService.sendMemberUpgradeMessage(finalBuyMemberId);

                        // 发送计算卖家等级消息
                        rabbitMQService.sendMemberUpgradeMessage(paymentOrder.getMemberId());

                        if (finalIsSplitOrder) {
                            //匹配池订单有子订单 查询全部子订单 并根据子订单状态更新匹配池订单状态
                            updateMatchPoolOrderStatus(finalMatchOrder);

                            //拆单 统计母订单ip
                            if (finalMatchPool != null) {
                                //拆单 统计母订单ip信息
                                redisUtil.recordTransactionAndCheckLimit(finalMatchPool.getClientIp(), finalMatchPool.getMatchOrder(), finalMatchPool.getCreateTime());
                            }
                        } else {
                            //非拆单 统计卖出订单ip信息
                            redisUtil.recordTransactionAndCheckLimit(paymentOrder.getClientIp(), paymentOrder.getPlatformOrder(), paymentOrder.getCreateTime());
                        }

                        //统计买入订单 ip信息
                        redisUtil.recordTransactionAndCheckLimit(collectionOrder.getClientIp(), collectionOrder.getPlatformOrder(), collectionOrder.getCreateTime());
                    }
                });

                return RestResult.ok(buyCompletedVo);
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("确认到账, 交易处理 失败 会员id: {}, 订单号: {}, e: {}", memberId, sellPlatformOrder, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 人工审核订单
     * @param matchingOrder
     * @param delayMinutes
     * @return
     */
    private BuyCompletedVo auditMatchingOrder(MatchingOrder matchingOrder, CollectionOrder colOrder, PaymentOrder paymentOrder, long delayMinutes) {
        LocalDateTime delayTime = LocalDateTime.now().plusMinutes(delayMinutes);
        // 更新撮合订单的工审核截至时间
        matchingOrder.setAuditDelayTime(delayTime);
        boolean b = matchingOrderService.updateById(matchingOrder);
        log.info("确认到账 更新撮合订单的工审核截至时间: {}, platformOrder:{}, sql执行结果: {}", delayMinutes, matchingOrder.getPlatformOrder(), b);

        //获取买入订单 加排他行锁
        CollectionOrder collectionOrder;
        if (ObjectUtils.isNotEmpty(colOrder)) {
            collectionOrder = colOrder;
        } else {
            collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(matchingOrder.getCollectionPlatformOrder());
        }
        // 更新买单的人工审核截至时间
        collectionOrder.setAuditDelayTime(delayTime);
        b = collectionOrderService.updateById(collectionOrder);
        log.info("确认到账 更新买单的人工审核截至时间: {}, platformOrder:{}, sql执行结果: {}", delayMinutes, collectionOrder.getPlatformOrder(), b);

        // 更新卖单的人工审核截至时间
        paymentOrder.setAuditDelayTime(delayTime);
        b = paymentOrderService.updateById(paymentOrder);
        log.info("确认到账 更新卖单的人工审核截至时间: {}, platformOrder:{}, sql执行结果: {}", delayMinutes, paymentOrder.getPlatformOrder(), b);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                //发送订单状态变化的消息给前端
                NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage = new NotifyOrderStatusChangeMessage(collectionOrder.getMemberId(), NotificationTypeEnum.NOTIFY_BUYER.getCode(), collectionOrder.getPlatformOrder());
                notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);

                //发送人工审核超时自动确认订单的MQ消息
                long delayMillis = TimeUnit.MINUTES.toMillis(delayMinutes);
                TaskInfo taskInfo = new TaskInfo(matchingOrder.getPlatformOrder(), TaskTypeEnum.CONFIRM_FINISH_ORDER_ON_AUDIT_TIMEOUT.getCode(), System.currentTimeMillis());
                rabbitMQService.sendTimeoutTask(taskInfo, delayMillis);
            }
        });

        BuyCompletedVo result = new BuyCompletedVo();
        result.setDelayMinutes(delayMinutes);

        return result;
    }


    /**
     * 交易成功 更新撮合列表订单
     *
     * @param matchingOrder
     * @return {@link Boolean}
     */
    Boolean updateMatchingOrder(MatchingOrder matchingOrder, String type) {

        if ("3".equals(type)) {
            // 将订单标识为 通过KYC自动完成
            matchingOrder.setKycAutoCompletionStatus(1);
        }

        //更新订单状态: 已完成
        matchingOrder.setStatus(OrderStatusEnum.SUCCESS.getCode());
        //更新完成时间
        matchingOrder.setCompletionTime(LocalDateTime.now(ZoneId.systemDefault()));
        //更新完成时长
        matchingOrder.setCompleteDuration(DurationCalculatorUtil.secondsBetween(matchingOrder.getCreateTime(), LocalDateTime.now()));

        boolean b = matchingOrderService.updateById(matchingOrder);

        log.info("确认到账 更新匹配池订单信息: {}, sql执行结果: {}", matchingOrder, b);

        return b;
    }

    /**
     * 交易成功 更新买入订单
     *
     * @param collectionOrder
     * @return {@link Boolean}
     */
    public Boolean updateCollectionOrder(CollectionOrder collectionOrder, String type) {

        if ("3".equals(type)) {
            // 将订单标识为 通过KYC自动完成
            collectionOrder.setKycAutoCompletionStatus(1);
        }

        //更新订单状态: 已完成
        collectionOrder.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
        //更新完成时间
        collectionOrder.setCompletionTime(LocalDateTime.now());
        //完成时长
        collectionOrder.setCompleteDuration(DurationCalculatorUtil.secondsBetween(collectionOrder.getCreateTime(), LocalDateTime.now()));

        boolean b = collectionOrderService.updateById(collectionOrder);

        log.info("确认到账 更新买入订单信息: {}, sql执行结果: {}", collectionOrder, b);

        return b;
    }


    /**
     * 交易成功 更新卖出订单
     *
     * @param paymentOrder
     * @return {@link Boolean}
     */
    Boolean updatePaymentOrder(PaymentOrder paymentOrder, String type) {

        if ("3".equals(type)) {
            // 将订单标识为 通过KYC自动完成
            paymentOrder.setKycAutoCompletionStatus(1);
        }

        //更新订单状态: 已完成
        paymentOrder.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
        //更新完成时间
        paymentOrder.setCompletionTime(LocalDateTime.now());
        //更新完成时长
        paymentOrder.setCompleteDuration(DurationCalculatorUtil.secondsBetween(paymentOrder.getCreateTime(), LocalDateTime.now()));
        // 重新计算奖励金额
        BigDecimal rewardAmount = transactionRewardsService.canReward(paymentOrder.getMemberId(), paymentOrder.getBonus());
        paymentOrder.setBonus(rewardAmount);

        boolean b = paymentOrderService.updateById(paymentOrder);

        log.info("确认到账 更新卖出订单信息: {}, sql执行结果: {}", paymentOrder, b);

        return b;
    }


    /**
     * 交易成功 更新买入会员信息
     *
     * @param buyMemberInfo
     * @return {@link Boolean}
     */
    @Override
    public Boolean updateBuyMemberInfo(MemberInfo buyMemberInfo, CollectionOrder collectionOrder) {

        //账变前余额
        BigDecimal previousBalance = buyMemberInfo.getBalance();

        //增加买入会员余额
        buyMemberInfo.setBalance(buyMemberInfo.getBalance().add(collectionOrder.getActualAmount()));

        //账变后余额
        BigDecimal newBalance = buyMemberInfo.getBalance();

        //添加会员余额账变
        memberAccountChangeService.recordMemberTransaction(String.valueOf(buyMemberInfo.getId()), collectionOrder.getActualAmount(), MemberAccountChangeEnum.RECHARGE.getCode(), collectionOrder.getPlatformOrder(), previousBalance, newBalance, "");

        //判断该笔买入订单是否有奖励
        if (collectionOrder.getBonus() != null && collectionOrder.getBonus().compareTo(new BigDecimal(0)) > 0) {

            //账变前余额
            BigDecimal previousBalance2 = buyMemberInfo.getBalance();

            //该笔订单有奖励 给会员余额添加该奖励
            buyMemberInfo.setBalance(buyMemberInfo.getBalance().add(collectionOrder.getBonus()));

            //账变后余额
            BigDecimal newBalance2 = buyMemberInfo.getBalance();

            //添加会员买入奖励账变
            memberAccountChangeService.recordMemberTransaction(String.valueOf(buyMemberInfo.getId()), collectionOrder.getBonus(), MemberAccountChangeEnum.BUY_BONUS.getCode(), collectionOrder.getPlatformOrder(), previousBalance2, newBalance2, "");

            //累计买入奖励
            buyMemberInfo.setTotalBuyBonus(buyMemberInfo.getTotalBuyBonus().add(collectionOrder.getBonus()));


            //添加买入奖励记录表
            TransactionRewards transactionRewards = new TransactionRewards();
            //会员id
            transactionRewards.setMemberId(buyMemberInfo.getId());
            //平台订单号
            transactionRewards.setPlatformOrder(collectionOrder.getPlatformOrder());
            //奖励金额
            transactionRewards.setRewardAmount(collectionOrder.getBonus());
            //奖励类型 2-买入奖励
            transactionRewards.setType(1);

            //保存 买入奖励记录
            transactionRewardsService.save(transactionRewards);
        }

        //累计买入成功次数
        buyMemberInfo.setTotalBuySuccessCount(buyMemberInfo.getTotalBuySuccessCount() + 1);

        //累计买入成功金额
        buyMemberInfo.setTotalBuySuccessAmount(buyMemberInfo.getTotalBuySuccessAmount().add(collectionOrder.getActualAmount()));

        //更新今日买入成功金额
        buyMemberInfo.setTodayBuySuccessAmount(buyMemberInfo.getTodayBuySuccessAmount().add(collectionOrder.getActualAmount()));

        //更新今日买入成功次数
        buyMemberInfo.setTodayBuySuccessCount(buyMemberInfo.getTodayBuySuccessCount() + 1);

        //校验买入会员分组是否需要自动调整
        MemberInfo memberInfo = memberGroupService.determineMemberGroup(buyMemberInfo);

        //更新买入会员信息
        boolean b = memberInfoService.updateById(memberInfo);

        log.info("确认到账 更新买入会员信息: {}, sql执行结果: {}", memberInfo, b);

        return b;
    }


    /**
     * 交易成功 更新卖出会员信息
     *
     * @param sellMemberInfo
     * @param paymentOrder
     * @return {@link Boolean}
     */
    Boolean updateSellMemberInfo(MemberInfo sellMemberInfo, PaymentOrder paymentOrder) {

        //扣除卖出会员冻结金额
        sellMemberInfo.setFrozenAmount(sellMemberInfo.getFrozenAmount().subtract(paymentOrder.getAmount()));

        //判断该笔订单是否有奖励
        if (paymentOrder.getBonus() != null && paymentOrder.getBonus().compareTo(new BigDecimal(0)) > 0) {

            //判断奖励是否达到当日奖励阈值
            if (paymentOrder.getBonus().compareTo(BigDecimal.ZERO) > 0) {
                //未达到限制 添加奖励
                log.info("奖励未达到当日奖励阈值, 添加卖出奖励: 订单号: {}, 会员id: {}", paymentOrder.getPlatformOrder(), sellMemberInfo.getId());

                //账变前余额
                BigDecimal previousBalance = sellMemberInfo.getBalance();

                //将奖励添加到会员余额
                sellMemberInfo.setBalance(sellMemberInfo.getBalance().add(paymentOrder.getBonus()));

                //账变后余额
                BigDecimal newBalance = sellMemberInfo.getBalance();

                //添加会员卖出奖励账变
                memberAccountChangeService.recordMemberTransaction(String.valueOf(sellMemberInfo.getId()), paymentOrder.getBonus(), MemberAccountChangeEnum.SELL_BONUS.getCode(), paymentOrder.getPlatformOrder(), previousBalance, newBalance, "");

                //累计卖出奖励
                sellMemberInfo.setTotalSellBonus(sellMemberInfo.getTotalSellBonus().add(paymentOrder.getBonus()));

                //添加卖出奖励记录表
                TransactionRewards transactionRewards = new TransactionRewards();
                //会员id
                transactionRewards.setMemberId(sellMemberInfo.getId());
                //平台订单号
                transactionRewards.setPlatformOrder(paymentOrder.getPlatformOrder());
                //奖励金额
                transactionRewards.setRewardAmount(paymentOrder.getBonus());
                //奖励类型 2-卖出奖励
                transactionRewards.setType(2);

                //保存 卖出奖励记录
                transactionRewardsService.save(transactionRewards);
            } else {
                //已达到限制 不添加奖励了
                log.info("奖励已达到当日奖励阈值, 不添加卖出奖励, 订单号: {}, 会员id: {}", paymentOrder.getPlatformOrder(), sellMemberInfo.getId());
            }
        }

        //累计卖出成功金额
        sellMemberInfo.setTotalSellSuccessAmount(sellMemberInfo.getTotalSellSuccessAmount().add(paymentOrder.getActualAmount()));

        //累计卖出成功次数
        sellMemberInfo.setTotalSellSuccessCount(sellMemberInfo.getTotalSellSuccessCount() + 1);

        //更新今日卖出成功金额
        sellMemberInfo.setTodaySellSuccessAmount(sellMemberInfo.getTodaySellSuccessAmount().add(paymentOrder.getActualAmount()));

        //更新今日卖出成功次数
        sellMemberInfo.setTodaySellSuccessCount(sellMemberInfo.getTodaySellSuccessCount() + 1);

        //将进行中的卖出订单数-1
//        sellMemberInfo.setActiveSellOrderCount(sellMemberInfo.getActiveSellOrderCount() - 1);

        //校验卖出会员分组是否需要自动调整
        MemberInfo memberInfo = memberGroupService.determineMemberGroup(sellMemberInfo);

        //更新卖出会员信息
        boolean b = memberInfoService.updateById(memberInfo);

        log.info("确认到账 更新卖出会员信息: {}, sql执行结果: {}", memberInfo, b);

        return b;
    }


    /**
     * 交易成功 更新收款信息
     *
     * @param collectionInfo
     * @return {@link Boolean}
     */
    Boolean updateCollectionInfo(CollectionInfo collectionInfo, CollectionOrder collectionOrder) {

        if (collectionInfo != null) {
            //更新收款信息 已收款金额
            collectionInfo.setCollectedAmount(collectionInfo.getCollectedAmount().add(collectionOrder.getActualAmount()));

            //更新收款信息 已收款笔数
            collectionInfo.setCollectedCount(collectionInfo.getCollectedCount() + 1);

            //更新收款信息 今日成功收款笔数
            collectionInfo.setTodaySuccessCollectedCount(collectionInfo.getTodaySuccessCollectedCount() + 1);

            //更新收款信息 今日成功收款金额
            collectionInfo.setTodaySuccessCollectedAmount(collectionInfo.getTodaySuccessCollectedAmount().add(collectionOrder.getActualAmount()));

            //更新收款信息
            boolean b = collectionInfoService.updateById(collectionInfo);

            log.info("确认到账 更新收款信息: {}, sql执行结果: {}", collectionInfo, b);

            return b;
        }
        return Boolean.TRUE;
    }


    /**
     * 交易成功 更新匹配池信息
     *
     * @param matchPool
     * @return {@link Boolean}
     */
    Boolean updatematchPool(MatchPool matchPool) {

        //进行中订单数 -1
        matchPool.setInProgressOrderCount(matchPool.getInProgressOrderCount() - 1);

        //已完成订单数 + 1
        matchPool.setCompletedOrderCount(matchPool.getCompletedOrderCount() + 1);

        boolean b = matchPoolService.updateById(matchPool);

        log.info("确认到账 更新匹配池信息: {}, sql执行结果: {}", matchPool, b);

        return b;
    }


    /**
     * 取消卖出订单处理
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult cancelSellOrder(CancelOrderReq cancelOrderReq) {

        //对取消原因表单进行HTML清洗
        cancelOrderReq.setReason(JsoupUtil.clean(cancelOrderReq.getReason()));

        //获取当前会员信息
        MemberInfo getMemberInfo = memberInfoService.getMemberInfo();
        if (getMemberInfo == null) {
            log.error("取消卖出订单处理失败, 该会员不存在: {}", getMemberInfo);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //获取当前会员id
        Long memberId = getMemberInfo.getId();

        //分布式锁key ar-wallet-sell+订单号
        String key = "ar-wallet-sell" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                MatchPool matchPool = null;

                PaymentOrder paymentOrder = null;

                //判断该笔订单是匹配池订单还是卖出订单
                if (cancelOrderReq.getPlatformOrder().startsWith("C2C")) {

                    //匹配池订单

                    //查询匹配池订单 加上排他行锁
                    matchPool = matchPoolMapper.selectMatchPoolForUpdate(cancelOrderReq.getPlatformOrder());

                    //校验该笔订单是否属于该会员 校验订单是否处于匹配超时状态
                    if (matchPool == null || !matchPool.getMemberId().equals(String.valueOf(memberId)) || !OrderStatusEnum.MATCH_TIMEOUT.getCode().equals(matchPool.getOrderStatus())) {
                        log.error("取消卖出订单处理失败: 订单状态必须是匹配超时状态才能取消 会员id: {}, 当前订单状态: {}, 订单信息: {}, req: {}", memberId, matchPool.getOrderStatus(), matchPool, cancelOrderReq);
                        return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                    }

                    //判断该笔订单如果是已取消状态, 那么直接返回成功
                    if (matchPool.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
                        return RestResult.ok();
                    }

                    //获取会员信息 加上排他行锁
                    MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(matchPool.getMemberId()));

                    //账变前余额
                    BigDecimal previousBalance = memberInfo.getBalance();

                    //账变后余额
                    BigDecimal newBalance = memberInfo.getBalance().add(matchPool.getRemainingAmount());

                    //更新会员信息
                    cancelSellOrderUpdateMemberInfo(memberInfo, matchPool.getRemainingAmount());

                    //记录会员账变信息
                    memberAccountChangeService.recordMemberTransaction(String.valueOf(memberInfo.getId()), matchPool.getRemainingAmount(), MemberAccountChangeEnum.CANCEL_RETURN.getCode(), matchPool.getMatchOrder(), previousBalance, newBalance, "");

                    //获取收款信息 加上排他行锁
                    CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(matchPool.getCollectionInfoId());
                    //更新收款信息
                    cancelSellOrderUpdateCollectionInfo(collectionInfo, matchPool.getRemainingAmount());

                    //更新匹配池订单信息
                    cancelSellOrderUpdateMatchPool(matchPool, cancelOrderReq);

                    log.info("取消卖出订单处理成功 会员账号: {}, 匹配池订单号: {}", memberInfo.getMemberAccount(), cancelOrderReq.getPlatformOrder());

                    //推送最新的 金额列表给前端
                    memberSendAmountList.send();

                } else if (cancelOrderReq.getPlatformOrder().startsWith("MC")) {

                    //卖出订单

                    //查询卖出订单 加上排他行锁
                    paymentOrder = paymentOrderMapper.selectPaymentForUpdate(cancelOrderReq.getPlatformOrder());

                    //校验该笔订单是否属于该会员 校验订单是否处于匹配超时状态
                    if (paymentOrder == null || !paymentOrder.getMemberId().equals(String.valueOf(memberId)) || !OrderStatusEnum.MATCH_TIMEOUT.getCode().equals(paymentOrder.getOrderStatus())) {
                        log.error("取消卖出订单处理失败: 订单状态为匹配超时才能取消, 会员id: {}, 当前订单状态: {}, 订单信息: {}, req: {}", memberId, paymentOrder.getOrderStatus(), paymentOrder, cancelOrderReq);
                        return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                    }

                    //判断该笔订单如果是已取消状态, 那么直接返回成功
                    if (paymentOrder.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
                        return RestResult.ok();
                    }

                    //获取会员信息 加上排他行锁
                    MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(paymentOrder.getMemberId()));

                    //账变前余额
                    BigDecimal previousBalance = memberInfo.getBalance();

                    //账变后余额
                    BigDecimal newBalance = memberInfo.getBalance().add(paymentOrder.getAmount());

                    //更新会员信息
                    cancelSellOrderUpdateMemberInfo(memberInfo, paymentOrder.getAmount());

                    //记录会员账变信息
                    memberAccountChangeService.recordMemberTransaction(String.valueOf(memberInfo.getId()), paymentOrder.getAmount(), MemberAccountChangeEnum.CANCEL_RETURN.getCode(), paymentOrder.getPlatformOrder(), previousBalance, newBalance, "");

                    //获取收款信息 加上排他行锁
                    CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(paymentOrder.getCollectionInfoId());
                    //更新收款信息
                    cancelSellOrderUpdateCollectionInfo(collectionInfo, paymentOrder.getAmount());

                    //更新卖出订单信息
                    cancelSellOrderUpdatePaymentOrder(paymentOrder, cancelOrderReq);

                    log.info("取消卖出订单处理成功 会员账号: {}, 卖出订单号: {}", memberInfo.getMemberAccount(), cancelOrderReq.getPlatformOrder());

                } else {
                    log.error("取消卖出订单处理失败 订单号错误 会员id :{}, 订单号: {}", memberId, cancelOrderReq);
                    return RestResult.failure(ResultCode.ORDER_NUMBER_ERROR);
                }

                //注册事务同步回调机制
                final MatchPool finalMatchPool = matchPool;
                final PaymentOrder finalPaymentOrder = paymentOrder;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        String upiId = (finalMatchPool != null) ? finalMatchPool.getUpiId() : finalPaymentOrder.getUpiId();

                        String cancelSellorderNo = (finalMatchPool != null) ? finalMatchPool.getMatchOrder() : finalPaymentOrder.getPlatformOrder();

                        //符合条件的话 将该收款信息 单日收款次数 -1
                        upiTransactionService.decrementDailyTransactionCountIfApplicable(upiId, cancelSellorderNo);

                        // 从进行中订单缓存中移除
                        orderChangeEventService.processCancelSellOrder(NotifyOrderStatusChangeMessage.builder().platformOrder(cancelOrderReq.getPlatformOrder()).memberId(String.valueOf(memberId)).build());

                    }
                });

                return RestResult.ok();
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("取消卖出订单处理失败 订单号错误 会员id :{}, 订单号: {}, e: {}", memberId, cancelOrderReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 继续匹配
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult continueMatching(PlatformOrderReq platformOrderReq) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("继续匹配处理失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-continueMatching+会员id
        String key = "ar-wallet-continueMatching" + memberInfo.getId();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                boolean isSplitOrder = false;
                MatchPool matchPool = null;
                PaymentOrder paymentOrder = null;

                //根据会员标签获取对应配置信息
                TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo);

                //从配置表获取 钱包用户卖出匹配时长 并将分钟转为毫秒
                long millis = TimeUnit.MINUTES.toMillis(schemeConfigByMemberTag.getSchemeSellMatchingDuration());

                Long lastUpdateTimestamp = System.currentTimeMillis();

                String memberId = String.valueOf(memberInfo.getId());

                //判断该笔订单是否拆单
                if (platformOrderReq.getPlatformOrder().startsWith("C2C")) {
                    //拆单

                    isSplitOrder = true;

                    //获取匹配池订单 加排他行锁
                    matchPool = matchPoolMapper.selectMatchPoolForUpdate(platformOrderReq.getPlatformOrder());

                    //校验该笔订单是否属于该会员 状态是否为 匹配超时
                    if (matchPool == null || !matchPool.getMemberId().equals(memberId)) {
                        log.error("继续匹配 失败: 该订单不属于该会员或该订单状态不为匹配超时, 会员账号: {}, 订单信息: {}, req: {}", memberInfo.getMemberAccount(), matchPool, platformOrderReq);
                        return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                    }

                    //判断该笔订单是否为 匹配中状态 如果为匹配中状态 直接返回成功
                    if (matchPool.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode())) {
                        return RestResult.ok();
                    }

                    //判断订单状态如果不是匹配超时 那么返回 订单状态校验失败
                    if (!OrderStatusEnum.MATCH_TIMEOUT.getCode().equals(matchPool.getOrderStatus())) {
                        log.error("继续匹配 失败: 订单状态校验失败 当前订单状态: {}, 会员账号: {}, 订单信息: {}, req: {}", matchPool.getOrderStatus(), memberInfo.getMemberAccount(), matchPool, platformOrderReq);
                        return RestResult.failure(ORDER_STATUS_VERIFICATION_FAILED);

                    }

                    //校验该笔订单剩余金额是否小于最小金额
                    if (matchPool.getRemainingAmount().compareTo(matchPool.getMinimumAmount()) < 0) {
                        log.error("继续匹配 失败: 该笔订单剩余金额小于最小金额, 会员账号: {}, 订单信息: {}, req: {}", memberInfo.getMemberAccount(), matchPool, platformOrderReq);
                        return RestResult.failure(ResultCode.INSUFFICIENT_BALANCE_2);
                    }

                    //将该笔订单的最大金额改为 剩余金额
                    matchPool.setMaximumAmount(matchPool.getRemainingAmount());

                    //将该笔订单状态改为 继续匹配
                    matchPool.setOrderStatus(OrderStatusEnum.BE_MATCHED.getCode());
                    //更新匹配时间戳
                    matchPool.setLastUpdateTimestamp(lastUpdateTimestamp);
                    //将匹配池 继续匹配字段 设置为1
                    matchPool.setContinueMatching("1");
                    //预计匹配时间
                    matchPool.setEstimatedMatchTime(schemeConfigByMemberTag.getSchemeSellMatchingDuration());

                    //更新匹配池订单
                    boolean b = matchPoolService.updateById(matchPool);

                    log.info("继续匹配 更新匹配池订单信息: {}, 会员账号: {}, sql执行结果: {}, 最后匹配时间戳: {}", matchPool, memberInfo.getMemberAccount(), b, lastUpdateTimestamp);

                    //将最后匹配时间存储到Redis 过期时间为12小时
                    String redisLastMatchTimeKey = RedisKeys.ORDER_LASTMATCHTIME + matchPool.getMatchOrder();
                    redisTemplate.opsForValue().set(redisLastMatchTimeKey, lastUpdateTimestamp);
                    redisTemplate.expire(redisLastMatchTimeKey, 12, TimeUnit.HOURS);

                    //发送匹配超时的MQ消息
                    TaskInfo taskInfo = new TaskInfo(matchPool.getMatchOrder(), TaskTypeEnum.WALLET_MEMBER_SALE_MATCH_TIMEOUT.getCode(), lastUpdateTimestamp);
                    rabbitMQService.sendTimeoutTask(taskInfo, millis);

//                    QueueInfo collectQueueInfo = new QueueInfo(RabbitMqConstants.AR_WALLET_DELAYED_ORDER_TIMEOUT_QUEUE_NAME, matchPool.getMatchOrder(), OrderTimeOutEnum.WALLET_MEMBER_SALE_MATCH_TIMEOUT.getCode(), lastUpdateTimestamp);
//                    rabbitMQUtil.sendDelayedMessage(matchPool.getMatchOrder(), Integer.parseInt(String.valueOf(millis)), new CorrelationData(JSON.toJSONString(collectQueueInfo)));

                    //将匹配倒计时记录到redis 卖出订单
                    redisUtil.setMatchExpireTime(matchPool.getMatchOrder(), schemeConfigByMemberTag.getSchemeSellMatchingDuration());

                } else {
                    //非拆单

                    //获取卖出订单 加排他行锁
                    paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrderReq.getPlatformOrder());

                    //校验该笔订单是否属于该会员 状态是否为 匹配超时
                    if (paymentOrder == null || !paymentOrder.getMemberId().equals(memberId) || !OrderStatusEnum.MATCH_TIMEOUT.getCode().equals(paymentOrder.getOrderStatus())) {
                        log.error("继续匹配 失败: 该订单不属于该会员或该订单状态不为匹配超时, 会员信息: {}, req: {}, 卖出订单信息: {}", memberInfo, platformOrderReq, paymentOrder);
                        return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                    }

                    //判断该笔订单是否为 匹配中状态 如果为匹配中状态 直接返回成功
                    if (paymentOrder.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode())) {
                        return RestResult.ok();
                    }

                    //将该笔订单状态改为 继续匹配
                    paymentOrder.setOrderStatus(OrderStatusEnum.BE_MATCHED.getCode());
                    //更新匹配时间戳
                    paymentOrder.setLastUpdateTimestamp(lastUpdateTimestamp);
                    //将卖出订单 继续匹配字段设置为1
                    paymentOrder.setContinueMatching(1);
                    //预计匹配时间
                    paymentOrder.setEstimatedMatchTime(schemeConfigByMemberTag.getSchemeSellMatchingDuration());

                    //更新卖出订单
                    boolean b = paymentOrderService.updateById(paymentOrder);

                    log.info("继续匹配 更新卖出订单信息: {}, 会员账号: {}, sql执行结果: {}, 最后匹配时间戳: {}", paymentOrder, memberInfo.getMemberAccount(), b, lastUpdateTimestamp);


                    //将最后匹配时间存储到Redis 过期时间为12小时
                    String redisLastMatchTimeKey = RedisKeys.ORDER_LASTMATCHTIME + paymentOrder.getPlatformOrder();
                    redisTemplate.opsForValue().set(redisLastMatchTimeKey, lastUpdateTimestamp);
                    redisTemplate.expire(redisLastMatchTimeKey, 12, TimeUnit.HOURS);

                    //发送匹配超时的MQ消息
                    TaskInfo taskInfo = new TaskInfo(paymentOrder.getPlatformOrder(), TaskTypeEnum.WALLET_MEMBER_SALE_MATCH_TIMEOUT.getCode(), lastUpdateTimestamp);
                    rabbitMQService.sendTimeoutTask(taskInfo, millis);

//                    QueueInfo collectQueueInfo = new QueueInfo(RabbitMqConstants.AR_WALLET_DELAYED_ORDER_TIMEOUT_QUEUE_NAME, paymentOrder.getPlatformOrder(), OrderTimeOutEnum.WALLET_MEMBER_SALE_MATCH_TIMEOUT.getCode(), lastUpdateTimestamp);
//                    rabbitMQUtil.sendDelayedMessage(paymentOrder.getPlatformOrder(), Integer.parseInt(String.valueOf(millis)), new CorrelationData(JSON.toJSONString(collectQueueInfo)));

                    //将匹配倒计时记录到redis 卖出订单
                    redisUtil.setMatchExpireTime(paymentOrder.getPlatformOrder(), schemeConfigByMemberTag.getSchemeSellMatchingDuration());

                }

                log.info("继续匹配 处理成功 会员账号: {}, req: {}", memberInfo.getMemberAccount(), platformOrderReq);


                //执行事务同步回调
                final boolean finalIsSplitOrder = isSplitOrder;
                final MatchPool finalMatchPool = matchPool;
                final PaymentOrder finalPaymentOrder = paymentOrder;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 事务提交后执行的Redis操作

                        String upiId = (finalMatchPool != null) ? finalMatchPool.getUpiId() : finalPaymentOrder.getUpiId();

                        //将该收款upi今日收款次数+1
                        upiTransactionService.incrementDailyTransactionCountAndMarkAsProcessed(upiId, platformOrderReq.getPlatformOrder());

                        if (finalIsSplitOrder) {
                            //拆单
                            BuyListVo buyListVo = new BuyListVo();
                            //订单号
                            buyListVo.setPlatformOrder(platformOrderReq.getPlatformOrder());
                            //订单金额
                            buyListVo.setAmount(finalMatchPool.getAmount());
                            //最小限额
                            buyListVo.setMinimumAmount(finalMatchPool.getMinimumAmount());
                            //最大限额 就是剩余金额
                            buyListVo.setMaximumAmount(finalMatchPool.getRemainingAmount());
                            //支付方式 目前只有UPI 先写死
                            buyListVo.setPayType(PayTypeEnum.INDIAN_UPI.getCode());
                            //头像
                            buyListVo.setAvatar(memberInfo.getAvatar());
                            //会员id
                            buyListVo.setMemberId(String.valueOf(memberInfo.getId()));
                            //会员类型
                            buyListVo.setMemberType(memberInfo.getMemberType());
                            //信用分
                            buyListVo.setCreditScore(memberInfo.getCreditScore());
                            //存入redis买入金额列表
                            redisUtil.addOrderIdToList(buyListVo, "2");

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                        } else {
                            //非拆单
                            BuyListVo buyListVo = new BuyListVo();
                            //订单号
                            buyListVo.setPlatformOrder(platformOrderReq.getPlatformOrder());
                            //订单金额
                            buyListVo.setAmount(finalPaymentOrder.getAmount());
                            //最小限额 非拆单就是订单金额
                            buyListVo.setMinimumAmount(finalPaymentOrder.getAmount());
                            //最大限额 非拆单就是订单金额
                            buyListVo.setMaximumAmount(finalPaymentOrder.getAmount());
                            //支付方式 目前只有UPI 先写死
                            buyListVo.setPayType(PayTypeEnum.INDIAN_UPI.getCode());
                            //头像
                            buyListVo.setAvatar(memberInfo.getAvatar());
                            //会员id
                            buyListVo.setMemberId(String.valueOf(memberInfo.getId()));
                            //会员类型
                            buyListVo.setMemberType(memberInfo.getMemberType());
                            //信用分
                            buyListVo.setCreditScore(memberInfo.getCreditScore());
                            //存入redis买入金额列表
                            redisUtil.addOrderIdToList(buyListVo, "2");

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();
                        }


                    }
                });

                return RestResult.ok();
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.info("继续匹配 处理失败 会员账号: {}, req: {} e: {}", memberInfo.getMemberAccount(), platformOrderReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 获取卖出页面接口数据
     *
     * @return {@link RestResult}<{@link SellListVo}>
     */
    @Override
    public RestResult<SellListVo> fetchPageData() {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取卖出页面接口数据失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //获取配置信息
        TradeConfig tradeConfig = tradeConfigService.getById(1);

        SellListVo sellListVo = new SellListVo();

        BeanUtils.copyProperties(tradeConfig, sellListVo);

        //可用余额
        sellListVo.setBalance(memberInfo.getBalance());

        //根据会员标签获取对应配置信息
        TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo);

        //卖出最多订单数
        Integer maxSellOrderNum = schemeConfigByMemberTag.getSchemeMaxSellOrderNum();

        //卖出奖励比例
        BigDecimal salesBonus = null;

        //查看会员如果有单独配置卖出奖励 那么就读取单独配置的卖出奖励
        if (memberInfo.getSellBonusProportion() != null && memberInfo.getSellBonusProportion().compareTo(new BigDecimal(0)) > 0) {
            salesBonus = memberInfo.getSellBonusProportion();
        } else {
            //判断该会员是钱包会员还是商户会员
            if (MemberTypeEnum.WALLET_MEMBER.getCode().equals(memberInfo.getMemberType())) {
                //钱包会员
                //会员没有单独配置卖出奖励, 获取配置表奖励比例 并计算出改笔订单奖励值
                if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                    salesBonus = schemeConfigByMemberTag.getSchemeSalesBonusProportion();
                }
            } else {
                //判断该商户是否单独配置了奖励比例 如果是的话 就直接取该商户单独配置的奖励

                //获取商户信息
                MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(memberInfo.getMerchantCode());

                if (merchantInfo != null) {

                    //该商户单独配置的卖出奖励不为null并且大于0
                    if (merchantInfo.getWithdrawalRewards() != null && merchantInfo.getWithdrawalRewards().compareTo(new BigDecimal(0)) > 0) {

                        salesBonus = merchantInfo.getWithdrawalRewards();
                    } else {
                        //商户会员 该商户没有单独配置卖出奖励 那么读取默认奖励
                        if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                            salesBonus = schemeConfigByMemberTag.getSchemeSalesBonusProportion();
                        }
                    }
                } else {
                    //就算商户不存在 也要按默认配置取计算奖励
                    if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                        salesBonus = schemeConfigByMemberTag.getSchemeSalesBonusProportion();
                    }
                }
            }
        }

        //最多拆单数
        Integer splitOrderCount = tradeConfig.getMaxSplitOrderCount();

        //卖出匹配时长
        Integer sellMatchingDuration = schemeConfigByMemberTag.getSchemeSellMatchingDuration();

        //最大卖出金额
        BigDecimal maxSellAmount = schemeConfigByMemberTag.getSchemeMaxSellAmount();

        //卖出最多订单数 判断会员是商户会员还是钱包会员
        sellListVo.setMaxSellOrderNum(maxSellOrderNum);

        //查看会员如果有单独配置卖出奖励 那么就读取单独配置的卖出奖励
        if (memberInfo.getSellBonusProportion() != null && memberInfo.getSellBonusProportion().compareTo(new BigDecimal(0)) > 0) {
            //会员单独配置了卖出奖励
            sellListVo.setSalesBonus(memberInfo.getSellBonusProportion());
        } else {
            //会员没有单独配置卖出奖励, 读取配置表奖励比例
            //卖出奖励比例 判断会员是商户会员还是钱包会员
            sellListVo.setSalesBonus(salesBonus);
        }

        //最多拆单数
        sellListVo.setSplitOrderCount(splitOrderCount);

        //卖出匹配时长
        sellListVo.setSellMatchingDuration(sellMatchingDuration);

        //正在进行中的卖出订单列表
        List<SellOrderListVo> ongoingOrders = getOngoingSellOrdersByMemberId(String.valueOf(memberInfo.getId()));

        if (ongoingOrders != null && ongoingOrders.size() > 0) {
            //使用 Collections.sort 方法结合 Lambda 表达式进行排序
            Collections.sort(ongoingOrders, (o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()));
        }

        sellListVo.setOngoingOrders(ongoingOrders);

        if (ongoingOrders != null) {
            long count = ongoingOrders.stream()
                    .filter(order -> order.getIsSplitOrder() == 0)
                    .count();
            sellListVo.setOngoingOrderCount(count);
        }

        //计算进行中的订单数 只统计未拆单的订单
//        long count = ongoingOrders.stream().filter(order -> order.getIsSplitOrder() == 0).count();
//
//        //正在进行中的订单数
//        sellListVo.setOngoingOrderCount(count);

        //最大卖出金额
        sellListVo.setMaxSellAmount(maxSellAmount);

        //最小卖出金额
        sellListVo.setMemberMinimumSellAmount(schemeConfigByMemberTag.getSchemeMinSellAmount());

        //卖出余额 统计未拆单的订单金额 和母订单的剩余金额 状态为 匹配中或匹配超时
        sellListVo.setSellBalance(calculateRemainingBalance(ongoingOrders, String.valueOf(memberInfo.getId())));

        //交易中 总卖出金额 - 卖出余额
        sellListVo.setInTransaction(calculateAmountInTransaction(ongoingOrders));

        //查询该会员 默认收款信息
        CollectionInfo defaultCollectionInfoByMemberId = collectionInfoService.getDefaultCollectionInfoByMemberId(String.valueOf(memberInfo.getId()));

        //如果存在默认收款信息 那么设置默认收款信息
        if (defaultCollectionInfoByMemberId != null) {
            sellListVo.setDefaultPaymentInfoId(defaultCollectionInfoByMemberId.getId());
            sellListVo.setDefaultUpiId(defaultCollectionInfoByMemberId.getUpiId());
        }
        sellListVo.setMemberSellStatus(memberInfo.getSellStatus());
        log.info("获取卖出页面接口数据成功 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), sellListVo);

        return RestResult.ok(sellListVo);
    }


    /**
     * 统计交易中金额
     *
     * @param ongoingOrders
     * @return {@link BigDecimal}
     */
    private BigDecimal calculateAmountInTransaction(List<SellOrderListVo> ongoingOrders) {

        //交易中金额
        BigDecimal inTransaction = BigDecimal.ZERO;

        for (SellOrderListVo ongoingOrder : ongoingOrders) {
            //只统计进行中的 非拆单订单或 拆单的子订单
            if (ongoingOrder.getPlatformOrder().startsWith("MC") && isOrderInProgress(ongoingOrder.getOrderStatus())) {
                inTransaction = inTransaction.add(ongoingOrder.getAmount());
            }
        }

        return inTransaction;
    }

    /**
     * 判断订单状态是否为进行中 过滤匹配中和匹配超时
     *
     * @param status
     * @return boolean
     */
    private boolean isOrderInProgress(String status) {
        return OrderStatusEnum.BE_PAID.getCode().equals(status) ||//待支付
                OrderStatusEnum.CONFIRMATION.getCode().equals(status) ||//确认中
                OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(status) ||//确认超时
                OrderStatusEnum.COMPLAINT.getCode().equals(status) ||//申诉中
                OrderStatusEnum.AMOUNT_ERROR.getCode().equals(status);//金额错误
    }

    /**
     * 计算会员卖出余额
     *
     * @param ongoingOrders
     * @param memberId
     * @return {@link BigDecimal}
     */
    private BigDecimal calculateRemainingBalance(List<SellOrderListVo> ongoingOrders, String memberId) {

        //未拆单的订单金额 匹配中 匹配超时状态
        BigDecimal nonSplitOrderAmount = BigDecimal.ZERO;

        //统计该会员的剩余金额 (状态为匹配中或匹配超时)
        BigDecimal remainingAmount = matchPoolService.sumRemainingAmount(memberId);

        for (SellOrderListVo ongoingOrder : ongoingOrders) {

            boolean isMatchingOrTimeout = ongoingOrder.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode()) || ongoingOrder.getOrderStatus().equals(OrderStatusEnum.MATCH_TIMEOUT.getCode());

            //统计未拆单的订单金额 匹配中或匹配超时状态
            if (ongoingOrder.getPlatformOrder().startsWith("MC") && ongoingOrder.getIsSplitOrder() == 0 && isMatchingOrTimeout) {
                nonSplitOrderAmount = nonSplitOrderAmount.add(ongoingOrder.getAmount());
            }
        }

        //卖出余额 = 匹配中或匹配超时状态中的 未拆掉订单金额 + 母订单剩余金额
        BigDecimal sellBalance = nonSplitOrderAmount.add(remainingAmount);

        return sellBalance;
    }

    /**
     * 统计子订单金额
     *
     * @param parentOrder
     * @param allOrders
     * @return {@link BigDecimal}
     */
    private BigDecimal calculateSubOrderAmount(SellOrderListVo parentOrder, List<SellOrderListVo> allOrders) {

        //子订单金额
        BigDecimal subOrderAmount = BigDecimal.ZERO;

        for (SellOrderListVo subOrder : allOrders) {

            if (subOrder.getIsSplitOrder() == 1 && subOrder.getMatchOrder() != null && subOrder.getMatchOrder().equals(parentOrder.getPlatformOrder())) {
                //累加订单金额
                subOrderAmount = subOrderAmount.add(subOrder.getAmount());
            }

        }
        return subOrderAmount;
    }

    /**
     * 提交金额错误处理
     *
     * @param platformOrder
     * @param images
     * @param video
     * @param orderActualAmount
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult processAmountError(String platformOrder, List<String> images, String video, BigDecimal orderActualAmount) {

        if (images == null) {
            log.error("提交金额错误处理失败: 会员没有上传图片文件 直接驳回, 订单号: {}, 文件名: {}", platformOrder, images);
            return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
        }

        // 检查images列表中的每个文件名是否符合规茨
        for (String image : images) {
            if (!FileUtil.isValidImageExtension(image)) {
                // 如果有文件不符合规茨，则返回错误
                log.error("提交金额错误处理失败: 会员上传图片文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, images);
                return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
            }
        }

        if (StringUtils.isNotEmpty(video)) {
            //视频文件名不为空才进行校验视频文件名
            if (!FileUtil.isValidVideoExtension(video)) {
                // 如果有文件不符合规茨，则返回错误
                log.error("提交金额错误处理失败: 会员上传视频文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, video);
                return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
            }
        }

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("提交金额错误处理失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-sellOrderAppealProcess+会员id
        String key = "ar-wallet-sellOrderAppealProcess" + memberInfo.getId();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取卖出订单 加上排他行锁
                PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrder);

                String memberId = String.valueOf(memberInfo.getId());

                //校验该订单是否属于当前会员
                if (paymentOrder == null || !paymentOrder.getMemberId().equals(memberId)) {
                    log.error("提交金额错误 处理失败, 订单校验失败 订单不存在或订单不属于该会员 会员信息: {}, 订单信息: {}", memberInfo, paymentOrder);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //判断当前订单状态如果是申诉中 直接返回成功
                if (paymentOrder.getOrderStatus().equals(OrderStatusEnum.COMPLAINT.getCode())) {
                    return RestResult.ok();
                }

                //校验该笔订单状态是否为: 确认中 确认超时
                if (!(paymentOrder.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) || paymentOrder.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode()))) {
                    log.error("提交金额错误 处理失败, 订单状态必须为确认中 当前订单状态为: {}, 订单信息: {}, 会员信息: {}", paymentOrder.getOrderStatus(), paymentOrder, memberInfo);
                    return RestResult.failure(ORDER_STATUS_VERIFICATION_FAILED);
                }


                //文件处理
//                JsonObject saveFile = saveFile(images, video);
//                if (saveFile.getString("errMsg") != null) {
//                    log.error("提交金额错误 文件校验失败 订单号: {}, msg: {}", platformOrder, saveFile);
//                    return RestResult.failure(ResultCode.FILE_VERIFICATION_FAILED);
//                }

//                String amountErrorImage = saveFile.getString("amountErrorImage");
//
//                String amountErrorVideo = saveFile.getString("amountErrorVideo");


                String amountErrorImage;
                StringBuilder sb = new StringBuilder();
                if (images != null && images.size() > 0) {

                    for (int i = 0; i < images.size(); i++) {

                        // 对每个图片地址进行修改或拼接
                        String imageWithBaseURL = baseUrl + images.get(i);

                        // 添加到StringBuilder对象
                        sb.append(imageWithBaseURL);

                        // 除了最后一个元素外，在每个元素后添加逗号
                        if (i < images.size() - 1) {
                            sb.append(",");
                        }
                    }

                    amountErrorImage = sb.toString();
                } else {
                    // 处理null情况，例如赋予一个默认值或者执行其他逻辑
                    amountErrorImage = ""; // 或者根据需要进行处理
                }

                String amountErrorVideo = (StringUtils.isNotEmpty(video)) ? baseUrl + video : null;

                //更新卖出订单
                paymentOrderToAmountError(paymentOrder, amountErrorImage, amountErrorVideo, orderActualAmount);


                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(paymentOrder.getMatchingPlatformOrder());
                //更新撮合列表
                matchingOrderToAmountError(matchingOrder, amountErrorImage, amountErrorVideo, orderActualAmount);


                //获取买入订单 加上排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(matchingOrder.getCollectionPlatformOrder());
                //更新买入订单
                collectionOrderToAmountError(collectionOrder, amountErrorImage, amountErrorVideo, orderActualAmount);

                log.info("提交金额错误 处理成功 会员账号: {}, 卖出订单号: {}, 买入订单号: {}, 撮合列表订单号: {}", memberInfo.getMemberAccount(), platformOrder, collectionOrder.getPlatformOrder(), matchingOrder.getPlatformOrder());

                //获取被申诉人信息 加上排他行锁
                MemberInfo respondent = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(collectionOrder.getMemberId()));
                //更新被申诉人信息 (被申诉次数) 将被申诉人 被申诉次数+1
                memberInfoService.incrementMemberComplaintCount(respondent);

                //生成申诉订单
                createAppealOrder(memberInfo, collectionOrder, paymentOrder, amountErrorImage, amountErrorVideo, "Incorrect Amount", "4");

                //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                final String finalCollectionplatformOrder = collectionOrder.getPlatformOrder();
                final String finalBuyMemberId = collectionOrder.getMemberId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        //通知买方
                        NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage = new NotifyOrderStatusChangeMessage(finalBuyMemberId, NotificationTypeEnum.NOTIFY_BUYER.getCode(), finalCollectionplatformOrder);

                        notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);

                    }
                });

                return RestResult.ok();
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("提交金额错误 处理失败 会员账号: {}, 卖出订单号: {}, e: {}", memberInfo.getMemberAccount(), platformOrder, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }


    /**
     * 文件处理
     *
     * @param images
     * @param video
     * @return {@link RestResult}
     */
    @Override
    public JsonObject saveFile(MultipartFile[] images, MultipartFile video) {

        JsonObject resJson = new JsonObject();


        if (images.length < 1 || images.length > 3) {
            log.error("提交金额错误 文件校验失败 请上传最少1张最多3张证明图片 会员上传的图片数量: {}", images.length);
            resJson.put("errMsg", "请上传最少1张最多3张证明图片");
            return resJson;
        }


        StringBuilder sb = new StringBuilder();

        for (MultipartFile image : images) {
            //图片文件校验
            RestResult validateFile = FileUtil.validateFile(image, arProperty.getMaxImageFileSize(), "image");
            if (validateFile != null) {
                log.error("提交金额错误 图片文件校验失败: {}", validateFile.getMsg());
                resJson.put("errMsg", validateFile.getMsg());
                return resJson;
            }

            //调用阿里云存储服务 将图片上传上去 并获取到文件名
            String fileName = ossService.uploadFile(image);

            if (fileName == null) {
                log.error("提交金额错误失败: 图片文件上传失败");
                resJson.put("errMsg", "上传失败");
                return resJson;
            }

            sb.append(fileName).append(",");
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1); // 删除最后的逗号
        }
        String amountErrorImage = sb.toString();


        //视频文件校验
        String amountErrorVideo = null;
        if (video != null) {
            RestResult validateFile = FileUtil.validateFile(video, arProperty.getMaxVideoFileSize(), "video");
            if (validateFile != null) {
                log.error("提交金额错误失败: 视频文件上传失败: {}", validateFile.getMsg());
                resJson.put("errMsg", validateFile.getMsg());
                return resJson;
            }

            //调用阿里云存储服务 将视频上传上去 并获取到文件名
            amountErrorVideo = ossService.uploadFile(video);

            if (amountErrorVideo == null) {
                log.error("提交金额错误失败: 视频文件上传失败");
                resJson.put("errMsg", "上传失败");
                return resJson;
            }
        }

        resJson.put("amountErrorImage", amountErrorImage);
        resJson.put("amountErrorVideo", amountErrorVideo);

        return resJson;
    }

    /**
     * 获取卖出订单列表
     *
     * @param sellOrderListReq
     * @return {@link RestResult}<{@link List}<{@link SellOrderListVo}>>
     */
    @Override
    public RestResult<PageReturn<SellOrderListVo>> sellOrderList(SellOrderListReq sellOrderListReq) {

        if (sellOrderListReq == null) {
            sellOrderListReq = new SellOrderListReq();
        }

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取卖出订单列表失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //查询匹配池订单 所有符合条件的记录
        List<SellOrderListVo> matchPoolOrderList = new ArrayList<>();
        matchPoolOrderList = matchPoolService.getMatchPoolOrderList(sellOrderListReq, memberInfo);

        //查询卖出订单 所有符合条件的记录
        List<SellOrderListVo> paymentOrderList = new ArrayList<>();
        paymentOrderList = paymentOrderService.getPaymentOrderOrderList(sellOrderListReq, memberInfo);

        log.info("获取卖出订单列表: matchPoolList: {}, paymentOrderList: {}, 会员账号: {}", matchPoolOrderList, paymentOrderList, memberInfo.getMemberAccount());

        //合并两个List 并根据订单时间进行排序
        List<SellOrderListVo> sellOrderListVos = new ArrayList<>();
        sellOrderListVos = Stream.concat(matchPoolOrderList.stream(), paymentOrderList.stream()).sorted(Comparator.comparing(SellOrderListVo::getCreateTime).reversed()).collect(Collectors.toList());

        //优化剩余时间为0状态没变的延迟
        for (SellOrderListVo sellOrderListVo : sellOrderListVos) {

            //判断如果是手动完成的话 就改为已完成
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }

            //判断如果订单是匹配中状态, 但是匹配剩余时间低于0 那么将返回前端的订单状态改为匹配超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode()) && (sellOrderListVo.getMatchExpireTime() == null || sellOrderListVo.getMatchExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
            }

            //判断如果订单是待支付状态, 但是支付剩余时间低于0 那么将返回前端的订单状态改为支付超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode()) && (sellOrderListVo.getPaymentExpireTime() == null || sellOrderListVo.getPaymentExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());
            }

            //判断如果订单是确认中状态, 但是确认剩余时间低于0 那么将返回前端的订单状态改为确认超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) && (sellOrderListVo.getConfirmExpireTime() == null || sellOrderListVo.getConfirmExpireTime() < 1)) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
            }

            // 人工审核状态
            if (OrderStatusEnum.isAuditing(sellOrderListVo.getOrderStatus(), sellOrderListVo.getAuditDelayTime())) {
                sellOrderListVo.setOrderStatus(OrderStatusEnum.AUDITING.getCode());
            }

        }

        PageReturn<SellOrderListVo> sellOrderListVoPageReturn = new PageReturn<>();

        long page = sellOrderListReq.getPageNo(); // 页码，假设从1开始
        long size = sellOrderListReq.getPageSize(); // 每页大小
        int totalRecords = sellOrderListVos.size(); // 总记录数

        //计算当前页的起始和结束索引
        int start = (int) ((page - 1) * size);
        int end = (int) Math.min(start + size, totalRecords);

        //提取当前页的数据
        List<SellOrderListVo> currentPageList = sellOrderListVos.subList(start, end);

        sellOrderListVoPageReturn.setPageNo(sellOrderListReq.getPageNo());
        sellOrderListVoPageReturn.setPageSize(size);
        sellOrderListVoPageReturn.setTotal(Long.valueOf(totalRecords));
        sellOrderListVoPageReturn.setList(currentPageList);

        log.info("获取卖出订单列表成功 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), sellOrderListVoPageReturn);

        return RestResult.ok(sellOrderListVoPageReturn);
    }

    /**
     * 匹配中(拆单) 页面数据处理
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link MatchPoolSplittingVo}>
     */
//    @Override
//    public RestResult<MatchPoolSplittingVo> matchPoolSplitting(PlatformOrderReq platformOrderReq) {
//
//        //获取当前会员信息
//        MemberInfo memberInfo = memberInfoService.getMemberInfo();
//
//        if (memberInfo == null) {
//            log.error("获取 前台-匹配中(拆单) 页面数据失败: 获取会员信息失败");
//            return RestResult.failure(ResultCode.RELOGIN);
//        }
//
//        //查询匹配池订单
//        MatchPool matchPoolOrder = matchPoolService.getMatchPoolOrderByOrderNo(platformOrderReq.getPlatformOrder());
//
//        if (matchPoolOrder == null || !matchPoolOrder.getMemberId().equals(memberInfo.getMemberId())) {
//            log.error("获取 前台-匹配中(拆单) 页面数据失败: 订单不存在或订单不属于该会员 会员信息: {}, 订单信息: {}", memberInfo, matchPoolOrder);
//            return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
//        }
//
//        //返回对象
//        MatchPoolSplittingVo matchPoolSplittingVo = new MatchPoolSplittingVo();
//
//        //将匹配池订单复制给返回对象
//        BeanUtils.copyProperties(matchPoolOrder, matchPoolSplittingVo);
//
//        //查询匹配池对应的所有子订单
//        matchPoolSplittingVo.setSellOrderList(paymentOrderService.getPaymentOrderListByMatchOrder(matchPoolOrder.getMatchOrder()));
//
//        log.info("获取匹配中(拆单) 页面数据成功 会员账号: {},  返回数据: {}", memberInfo.getMemberAccount(), matchPoolSplittingVo);
//
//        return RestResult.ok(matchPoolSplittingVo);
//    }

    /**
     * 卖出订单申诉处理
     *
     * @param platformOrder
     * @param appealReason
     * @param images
     * @param video
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult sellOrderAppealProcess(String platformOrder, String appealReason, List<String> images, String video) {


        if (images == null) {
            log.error("卖出订单申诉处理失败: 会员没有上传图片文件 直接驳回, 订单号: {}, 文件名: {}", platformOrder, images);
            return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
        }

        // 检查images列表中的每个文件名是否符合规茨
        for (String image : images) {
            if (!FileUtil.isValidImageExtension(image)) {
                // 如果有文件不符合规茨，则返回错误
                log.error("卖出订单申诉处理失败: 会员上传图片文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, images);
                return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
            }
        }

        if (StringUtils.isNotEmpty(video)) {
            //视频文件名不为空才进行校验视频文件名
            if (!FileUtil.isValidVideoExtension(video)) {
                // 如果有文件不符合规茨，则返回错误
                log.error("卖出订单申诉处理失败: 会员上传视频文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, video);
                return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
            }
        }

        //对申诉原因表单进行HTML清洗
        appealReason = JsoupUtil.clean(appealReason);

        //获取当前会员id
        Long memberId = UserContext.getCurrentUserId();

        //分布式锁key ar-wallet-sellOrderAppealProcess+订单号
        String key = "ar-wallet-sellOrderAppealProcess" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取当前会员信息
                MemberInfo complainant = memberInfoService.getById(memberId);

                if (complainant == null) {
                    log.error("卖出订单申诉处理失败: 获取会员信息失败");
                    return RestResult.failure(ResultCode.RELOGIN);
                }

                String complainantId = String.valueOf(complainant.getId());

                //获取卖出订单 加上排他行锁
                PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrder);

                //校验该订单是否属于当前会员
                if (paymentOrder == null || !paymentOrder.getMemberId().equals(complainantId)) {
                    log.error("卖出订单申诉 提交失败 订单不存在或订单不属于该会员 会员信息: {}, 卖出订单信息: {}", complainant, paymentOrder);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //判断当前订单状态如果是申诉中 直接返回成功
                if (paymentOrder.getOrderStatus().equals(OrderStatusEnum.COMPLAINT.getCode())) {
                    return RestResult.ok();
                }

                //判断当前订单状态 是否为: 确认超时  只有确认超时的订单才能进行买入申诉
                if (!OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(paymentOrder.getOrderStatus())) {
                    log.error("卖出订单申诉 提交失败 超时订单才能进行申诉 当前订单状态: {}, 会员信息: {}, 卖出订单信息: {}", paymentOrder.getOrderStatus(), complainant, paymentOrder);
                    return RestResult.failure(ORDER_STATUS_VERIFICATION_FAILED);
                }

                if (OrderStatusEnum.isAuditing(paymentOrder.getOrderStatus(), paymentOrder.getAuditDelayTime())) {
                    log.error("卖出订单申诉 提交失败 人工审核状态订单不能进行申诉, 会员账号:{}, 卖出订单信息: {}", complainant.getMemberAccount(), paymentOrder);
                    return RestResult.failure(ORDER_STATUS_VERIFICATION_FAILED);
                }

                //文件处理
//                JsonObject saveFile = appealOrderService.saveFile(images, video);
//                if (saveFile.getString("errMsg") != null) {
//                    log.error("卖出申诉 文件校验失败: {}, msg: {}", platformOrder, saveFile);
//                    return RestResult.failure(ResultCode.FILE_VERIFICATION_FAILED);
//                }

//                String appealImage = saveFile.getString("appealImage");
//                String appealVideo = saveFile.getString("appealVideo");


                String appealImage;
                StringBuilder sb = new StringBuilder();
                if (images != null && images.size() > 0) {

                    for (int i = 0; i < images.size(); i++) {

                        // 对每个图片地址进行修改或拼接
                        String imageWithBaseURL = baseUrl + images.get(i);

                        // 添加到StringBuilder对象
                        sb.append(imageWithBaseURL);

                        // 除了最后一个元素外，在每个元素后添加逗号
                        if (i < images.size() - 1) {
                            sb.append(",");
                        }
                    }

                    appealImage = sb.toString();
                } else {
                    // 处理null情况，例如赋予一个默认值或者执行其他逻辑
                    appealImage = ""; // 或者根据需要进行处理
                }

                String appealVideo = (StringUtils.isNotEmpty(video)) ? baseUrl + video : null;

                //更新卖出订单状态为 申诉中
                updatePaymentOrderToAppealInProgress(paymentOrder);

                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(paymentOrder.getMatchingPlatformOrder());
                //更新撮合列表订单为 申诉中
                updateMatchingOrderToAppealInProgress(matchingOrder);

                //获取买入订单 加上排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(matchingOrder.getCollectionPlatformOrder());
                //更新买入订单为 申诉中
                updateCollectionOrderToAppealInProgress(collectionOrder);

                //获取被申诉人信息 加上排他行锁
                MemberInfo respondent = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(collectionOrder.getMemberId()));
                //更新被申诉人信息 (被申诉次数)
                respondent.setAppealCount(respondent.getAppealCount() + 1);
                memberInfoService.updateById(respondent);

                //生成申诉订单
                createAppealOrder(complainant, collectionOrder, paymentOrder, appealImage, appealVideo, appealReason, "1");

                log.info("卖出订单申诉, 提交成功: {}, 会员账号: {}", platformOrder, complainant.getMemberAccount());


                //注册事务同步回调
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        //通知买方
                        NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage = new NotifyOrderStatusChangeMessage(collectionOrder.getMemberId(), NotificationTypeEnum.NOTIFY_BUYER.getCode(), collectionOrder.getPlatformOrder());

                        notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);
                    }
                });

                return RestResult.ok();

            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("买入处理失败 会员id: {}, 订单号: {}, e: {}", memberId, platformOrder, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        log.error("买入处理失败 会员id: {}, 订单号: {}", memberId, platformOrder);
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 获取取消卖出页面数据
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link CancelSellPageDataVo}>
     */
    @Override
    public RestResult<CancelSellPageDataVo> getCancelSellPageData(PlatformOrderReq platformOrderReq) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取取消卖出页面数据失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        String memberId = String.valueOf(memberInfo.getId());

        //判断是否拆单
        if (platformOrderReq.getPlatformOrder().startsWith("C2C")) {
            //拆单
            //根据匹配池订单号 获取匹配池订单
            MatchPool matchPool = matchPoolService.getMatchPoolOrderByOrderNo(platformOrderReq.getPlatformOrder());


            if (matchPool == null || !matchPool.getMemberId().equals(memberId)) {
                log.error("获取取消卖出页面数据 失败: 订单不存在或订单不属于该会员");
                return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
            }

            CancelSellPageDataVo cancelSellPageDataVo = new CancelSellPageDataVo();

            BeanUtils.copyProperties(matchPool, cancelSellPageDataVo);

            //设置卖出数量
            cancelSellPageDataVo.setSellQuantity(matchPool.getAmount());

            //获取充值取消原因列表
            cancelSellPageDataVo.setReason(withdrawalCancellationService.getSellCancelReasonsList());

            //订单号
            cancelSellPageDataVo.setPlatformOrder(matchPool.getMatchOrder());

            log.info("获取取消卖出页面数据 成功 会员账号: {}, req: {}, 返回数据: {}", memberInfo.getMemberAccount(), platformOrderReq, cancelSellPageDataVo);

            return RestResult.ok(cancelSellPageDataVo);

        } else {
            //非拆单

            //根据卖出订单号 查询卖出订单
            PaymentOrder paymentOrder = paymentOrderService.getPaymentOrderByOrderNo(platformOrderReq.getPlatformOrder());

            if (paymentOrder == null || !paymentOrder.getMemberId().equals(memberId)) {
                log.error("获取取消卖出页面数据 失败: 订单不存在或订单不属于该会员");
                return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
            }

            CancelSellPageDataVo cancelSellPageDataVo = new CancelSellPageDataVo();

            BeanUtils.copyProperties(paymentOrder, cancelSellPageDataVo);

            //设置卖出数量
            cancelSellPageDataVo.setSellQuantity(paymentOrder.getAmount());

            //获取充值取消原因列表
            cancelSellPageDataVo.setReason(withdrawalCancellationService.getSellCancelReasonsList());

            log.info("获取取消卖出页面数据 成功 会员账号: {}, req: {}, 返回数据: {}", memberInfo.getMemberAccount(), platformOrderReq, cancelSellPageDataVo);

            return RestResult.ok(cancelSellPageDataVo);
        }
    }

    /**
     * 查看卖出订单详情
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link MatchPoolSplittingVo}>
     */
    @Override
    public RestResult<SellOrderDetailsVo> getSellOrderDetails(PlatformOrderReq platformOrderReq) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("查看卖出订单详情失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        String memberId = String.valueOf(memberInfo.getId());

        //卖出订单详情vo
        SellOrderDetailsVo sellOrderDetailsVo = new SellOrderDetailsVo();

        //判断订单是匹配池订单还是卖出订单
        if (platformOrderReq.getPlatformOrder().startsWith("C2C")) {
            //匹配池订单
            MatchPool matchPool = matchPoolService.getMatchPoolOrderByOrderNo(platformOrderReq.getPlatformOrder());

            if (matchPool == null || !matchPool.getMemberId().equals(memberId)) {
                log.error("查看卖出订单详情失败 订单不存在或订单不属于该会员, 会员信息: {}, 订单信息: {}", memberInfo, matchPool);
                return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
            }

            BeanUtils.copyProperties(matchPool, sellOrderDetailsVo);

            //兼容取消原因和失败原因
            if (sellOrderDetailsVo.getRemark() == null){
                sellOrderDetailsVo.setRemark(sellOrderDetailsVo.getCancellationReason());
            }

            //如果是手动完成状态, 改为已完成状态
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }

            //匹配池订单号
            sellOrderDetailsVo.setPlatformOrder(matchPool.getMatchOrder());

            //设置匹配剩余时间
            sellOrderDetailsVo.setMatchExpireTime(redisUtil.getMatchRemainingTime(matchPool.getMatchOrder()));

            //优化剩余时间为0 状态还没更新的延迟
            //判断如果订单是匹配中状态, 但是匹配剩余时间低于0 那么将返回前端的订单状态改为匹配超时
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode()) && (sellOrderDetailsVo.getMatchExpireTime() == null || sellOrderDetailsVo.getMatchExpireTime() < 1)) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
            }

            //判断如果订单是待支付状态, 但是支付剩余时间低于0 那么将返回前端的订单状态改为支付超时
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode()) && (sellOrderDetailsVo.getPaymentExpireTime() == null || sellOrderDetailsVo.getPaymentExpireTime() < 1)) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());
            }

            //判断如果订单是确认中状态, 但是确认剩余时间低于0 那么将返回前端的订单状态改为确认超时
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) && (sellOrderDetailsVo.getConfirmExpireTime() == null || sellOrderDetailsVo.getConfirmExpireTime() < 1)) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
            }

            //查询该匹配池订单的子订单
            List<SellOrderListVo> paymentOrderListByMatchOrder = paymentOrderService.getPaymentOrderListByMatchOrder(matchPool.getMatchOrder());

            sellOrderDetailsVo.setSellOrderList(paymentOrderListByMatchOrder);

            return RestResult.ok(sellOrderDetailsVo);

        } else if (platformOrderReq.getPlatformOrder().startsWith("MC")) {
            //卖出订单
            PaymentOrder paymentOrder = paymentOrderService.getPaymentOrderByOrderNo(platformOrderReq.getPlatformOrder());

            BeanUtils.copyProperties(paymentOrder, sellOrderDetailsVo);

            //判断状态如果是手动完成 那么就改为已完成
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }

            //是否经过申诉
            if (paymentOrder.getAppealTime() != null) {
                sellOrderDetailsVo.setIsAppealed(1);

                //查询申诉订单
                AppealOrder appealOrderBySellOrderNo = appealOrderService.getAppealOrderBySellOrderNo(paymentOrder.getPlatformOrder());

                if (appealOrderBySellOrderNo != null) {
                    //设置申诉类型
                    sellOrderDetailsVo.setDisplayAppealType(appealOrderBySellOrderNo.getDisplayAppealType());
                }
            }

            //匹配剩余时间
            sellOrderDetailsVo.setMatchExpireTime(redisUtil.getMatchRemainingTime(platformOrderReq.getPlatformOrder()));
            //确认中剩余时间
            sellOrderDetailsVo.setConfirmExpireTime(redisUtil.getConfirmRemainingTime(platformOrderReq.getPlatformOrder()));
            //待支付剩余时间
            sellOrderDetailsVo.setPaymentExpireTime(redisUtil.getPaymentRemainingTime(platformOrderReq.getPlatformOrder()));


            //优化剩余时间为0 状态还没更新的延迟
            //判断如果订单是匹配中状态, 但是匹配剩余时间低于0 那么将返回前端的订单状态改为匹配超时
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode()) && (sellOrderDetailsVo.getMatchExpireTime() == null || sellOrderDetailsVo.getMatchExpireTime() < 1)) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
            }

            //判断如果订单是待支付状态, 但是支付剩余时间低于0 那么将返回前端的订单状态改为支付超时
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode()) && (sellOrderDetailsVo.getPaymentExpireTime() == null || sellOrderDetailsVo.getPaymentExpireTime() < 1)) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());
            }

            //判断如果订单是确认中状态, 但是确认剩余时间低于0 那么将返回前端的订单状态改为确认超时
            if (sellOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) && (sellOrderDetailsVo.getConfirmExpireTime() == null || sellOrderDetailsVo.getConfirmExpireTime() < 1)) {
                sellOrderDetailsVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
            }

            //是否是子订单
            if (StringUtils.isNotEmpty(paymentOrder.getMatchOrder())) {
                sellOrderDetailsVo.setIsSubOrder(1);
            }

            return RestResult.ok(sellOrderDetailsVo);
        } else {
            //订单号错误
            log.error("查看卖出订单详情失败, 订单号错误, 会员账号: {}, 订单号: {}", memberInfo.getMemberAccount(), platformOrderReq.getPlatformOrder());
            return RestResult.failure(ResultCode.ORDER_NUMBER_ERROR);
        }
    }

    /**
     * 生成申诉订单
     *
     * @return {@link Boolean}
     */
    private Boolean createAppealOrder(MemberInfo complainant, CollectionOrder collectionOrder, PaymentOrder paymentOrder, String appealImage, String appealVideo, String appealReason, String type) {

        //先查询 如果存在申诉订单了 那么只是改变申诉订单状态就可以了
        //查询申诉订单 加上排他行锁
        AppealOrder appealOrderNew = appealOrderMapper.selectAppealOrderBywithdrawOrderNoForUpdate(paymentOrder.getPlatformOrder());

        if (appealOrderNew != null) {
            //已存在申诉订单, 只是改变申诉订单状态即可

            //判断 如果申诉订单是 未支付状态 才改变申诉状态

            String appealStatus = String.valueOf(appealOrderNew.getAppealStatus());

            if (AppealStatusEnum.UNPAID.getCode().equals(appealStatus)) {
                if ("4".equals(type)) {
                    //显示申诉类型 2: 金额错误
                    appealOrderNew.setDisplayAppealType(Integer.valueOf(DisplayAppealTypeEnum.AMOUNT_INCORRECT.getCode()));
                } else {
                    //显示申诉类型 1: 未到账
                    appealOrderNew.setDisplayAppealType(Integer.valueOf(DisplayAppealTypeEnum.PAYMENT_NOT_RECEIVED.getCode()));
                }

                //申诉状态 1为待处理
                appealOrderNew.setAppealStatus(Integer.valueOf(AppealStatusEnum.PENDING.getCode()));

                //申诉类型 1为卖出订单申诉
                appealOrderNew.setAppealType(1);

                //UTR
                appealOrderNew.setUtr(paymentOrder.getUtr());

                //所属商户 这是卖出申诉  所以记录卖家的商户名称
                appealOrderNew.setMerchantName(paymentOrder.getMerchantName());

                //申诉订单金额
                appealOrderNew.setOrderAmount(paymentOrder.getAmount());

                //会员id
                appealOrderNew.setMid(String.valueOf(complainant.getId()));

                //会员账号
                appealOrderNew.setMAccount(complainant.getMemberAccount());

                //买入订单号
                appealOrderNew.setRechargeOrderNo(collectionOrder.getPlatformOrder());

                //卖出订单号
                appealOrderNew.setWithdrawOrderNo(paymentOrder.getPlatformOrder());

                //实际金额
                appealOrderNew.setActualAmount(paymentOrder.getActualAmount());

                //被申诉人id 卖出订单申诉 所以买入订单会员id就是被申诉人id
                appealOrderNew.setAppealedMemberId(collectionOrder.getMemberId());

                //申诉原因
                if (appealReason != null) {
                    appealOrderNew.setReason(appealReason);
                }

                //申诉图片
                appealOrderNew.setPicInfo(appealImage);

                //申诉视频
                if (appealVideo != null) {
                    appealOrderNew.setVideoUrl(appealVideo);
                }


                boolean b = appealOrderService.updateById(appealOrderNew);

                log.info("卖出订单申诉 已存在申诉订单, 申诉订单状态为: 未支付, 更新申诉订单: {}, sql执行结果: {}", appealOrderNew, b);

                return b;
            } else {
                log.info("卖出订单申诉 已存在申诉订单, 申诉订单状态不是未支付, 系统不做处理 申诉订单信息: {}", appealOrderNew);
                return true;
            }
        } else {
            AppealOrder appealOrder = new AppealOrder();

            //UTR
            appealOrder.setUtr(paymentOrder.getUtr());

            //申诉类型 1为卖出订单申诉
            appealOrder.setAppealType(1);

            //所属商户 这是卖出申诉  所以记录卖家的商户名称
            appealOrder.setMerchantName(paymentOrder.getMerchantName());

            if ("4".equals(type)) {
                //显示申诉类型 2: 金额错误
                appealOrder.setDisplayAppealType(Integer.valueOf(DisplayAppealTypeEnum.AMOUNT_INCORRECT.getCode()));
            } else {
                //显示申诉类型 1: 未到账
                appealOrder.setDisplayAppealType(Integer.valueOf(DisplayAppealTypeEnum.PAYMENT_NOT_RECEIVED.getCode()));
            }

            //申诉状态 申诉中
            appealOrder.setAppealStatus(Integer.valueOf(AppealStatusEnum.PENDING.getCode()));

            //申诉订单金额
            appealOrder.setOrderAmount(paymentOrder.getAmount());

            //会员id
            appealOrder.setMid(String.valueOf(complainant.getId()));

            //会员账号
            appealOrder.setMAccount(complainant.getMemberAccount());

            //买入订单号
            appealOrder.setRechargeOrderNo(collectionOrder.getPlatformOrder());

            //卖出订单号
            appealOrder.setWithdrawOrderNo(paymentOrder.getPlatformOrder());

            //实际金额
            appealOrder.setActualAmount(paymentOrder.getActualAmount());

            //被申诉人id 卖出订单申诉 所以买入订单会员id就是被申诉人id
            appealOrder.setAppealedMemberId(collectionOrder.getMemberId());

            //申诉原因
            if (appealReason != null) {
                appealOrder.setReason(appealReason);
            }

            //申诉图片
            appealOrder.setPicInfo(appealImage);

            //申诉视频
            if (appealVideo != null) {
                appealOrder.setVideoUrl(appealVideo);
            }

            boolean save = appealOrderService.save(appealOrder);

            log.info("卖出订单申诉 生成申诉订单: {}, sql执行结果: {}", appealOrder, save);

            return save;
        }
    }

    /**
     * 更新买入订单为 申诉中
     *
     * @param collectionOrder
     * @return {@link Boolean}
     */
    private Boolean updateCollectionOrderToAppealInProgress(CollectionOrder collectionOrder) {

        //更新买入订单状态为: 申诉中
        collectionOrder.setOrderStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新申诉时间
        collectionOrder.setAppealTime(LocalDateTime.now());

        boolean b = collectionOrderService.updateById(collectionOrder);

        log.info("卖出订单申诉 更新买入订单信息: {}, sql执行结果: {}", collectionOrder, b);

        return b;
    }

    /**
     * 更新撮合列表订单为: 申诉中
     *
     * @param matchingOrder
     * @return {@link Boolean}
     */
    private Boolean updateMatchingOrderToAppealInProgress(MatchingOrder matchingOrder) {

        //更新撮合列表订单状态为: 申诉中
        matchingOrder.setStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新撮合列表订单申诉类型为: 未到账
        matchingOrder.setDisplayAppealType(Integer.valueOf(DisplayAppealTypeEnum.PAYMENT_NOT_RECEIVED.getCode()));

        //更新申诉时间
        matchingOrder.setAppealTime(LocalDateTime.now());

        boolean b = matchingOrderService.updateById(matchingOrder);

        log.info("卖出订单申诉 更新撮合列表订单信息: {}, sql执行结果: {}", matchingOrder, b);

        return b;
    }

    /**
     * 更新卖出订单为 申诉中
     *
     * @param paymentOrder
     * @return {@link Boolean}
     */
    private Boolean updatePaymentOrderToAppealInProgress(PaymentOrder paymentOrder) {

        //更新卖出订单状态为 申诉中
        paymentOrder.setOrderStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新申诉时间
        paymentOrder.setAppealTime(LocalDateTime.now());

        boolean b = paymentOrderService.updateById(paymentOrder);

        log.info("卖出订单申诉 更新卖出订单信息: {}, sql执行结果: {}", paymentOrder, b);

        return b;
    }


    /**
     * 取消卖出订单 更新卖出订单状态
     *
     * @param paymentOrder
     * @param cancelOrderReq
     * @return {@link Boolean}
     */
    Boolean cancelSellOrderUpdatePaymentOrder(PaymentOrder paymentOrder, CancelOrderReq cancelOrderReq) {

        //将卖出订单改为已取消状态
        paymentOrder.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());

        //将卖出订单 取消匹配字段 设置为1
        paymentOrder.setCancelMatching(1);

        if (cancelOrderReq != null) {
            //设置取消原因
            paymentOrder.setCancellationReason(cancelOrderReq.getReason());
        }

        //取消时间
        paymentOrder.setCancelTime(LocalDateTime.now());

        //取消人
        paymentOrder.setCancelBy(paymentOrder.getMemberAccount());

        //更新卖出订单信息
        boolean b = paymentOrderService.updateById(paymentOrder);

        log.info("取消卖出订单处理 更新卖出订单状态: {}, sql执行结果: {}", paymentOrder, b);

        return b;
    }

    /**
     * 取消卖出订单 更新匹配池订单状态
     *
     * @param matchPool
     * @param cancelOrderReq
     * @return {@link Boolean}
     */
    Boolean cancelSellOrderUpdateMatchPool(MatchPool matchPool, CancelOrderReq cancelOrderReq) {

        //将卖出订单改为已取消状态
        matchPool.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());

        if (cancelOrderReq != null) {
            //添加取消原因
            matchPool.setCancellationReason(cancelOrderReq.getReason());
        }

        //将剩余金额清空
        matchPool.setRemainingAmount(new BigDecimal(0));

        //将匹配池订单 取消匹配设置为1
        matchPool.setCancelMatching("1");

        //更新匹配池订单信息
        boolean b = matchPoolService.updateById(matchPool);

        log.info("取消卖出订单处理 更新匹配池订单状态: {}, sql执行结果: {}", matchPool, b);

        return b;
    }

    /**
     * 取消卖出订单 更新收款信息
     *
     * @return {@link Boolean}
     */
    Boolean cancelSellOrderUpdateCollectionInfo(CollectionInfo collectionInfo, BigDecimal amount) {

        if (collectionInfo != null) {
            //减去今日收款金额
            collectionInfo.setTodayCollectedAmount(collectionInfo.getTodayCollectedAmount().subtract(amount));

            //今日收款笔数-1
            collectionInfo.setTodayCollectedCount(collectionInfo.getTodayCollectedCount() - 1);

            //更新收款信息
            boolean b = collectionInfoService.updateById(collectionInfo);

            log.info("取消卖出订单处理 更新收款信息: {}, sql执行结果: {}", collectionInfo, b);

            return b;
        }
        return Boolean.TRUE;
    }


    /**
     * 取消卖出订单 更新会员信息
     *
     * @param memberInfo
     * @param amount
     * @return {@link Boolean}
     */
    Boolean cancelSellOrderUpdateMemberInfo(MemberInfo memberInfo, BigDecimal amount) {

        //扣除冻结金额
        memberInfo.setFrozenAmount(memberInfo.getFrozenAmount().subtract(amount));

        //添加会员余额
        memberInfo.setBalance(memberInfo.getBalance().add(amount));

        //将进行中的卖出订单数-1
//        memberInfo.setActiveSellOrderCount(memberInfo.getActiveSellOrderCount() - 1);

        boolean b = memberInfoService.updateById(memberInfo);

        log.info("取消卖出订单处理 更新会员信息: {}, sql执行结果: {}", memberInfo, b);

        return b;
    }


    /**
     * 金额错误 更新卖出订单
     *
     * @param paymentOrder
     * @param amountErrorImage
     * @param amountErrorVideo
     * @param orderActualAmount
     * @return {@link Boolean}
     */
    private Boolean paymentOrderToAmountError(PaymentOrder paymentOrder, String amountErrorImage, String amountErrorVideo, BigDecimal orderActualAmount) {

        //更新卖出订单状态为 申诉中
        paymentOrder.setOrderStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新申诉时间
        paymentOrder.setAppealTime(LocalDateTime.now());

        //设置实际金额
        paymentOrder.setActualAmount(orderActualAmount);

        //更新金额错误图片
        paymentOrder.setAmountErrorImage(amountErrorImage);

        //更新金额错误提交时间
        paymentOrder.setAmountErrorSubmitTime(LocalDateTime.now());

        //更新金额错误视频
        if (amountErrorVideo != null) {
            paymentOrder.setAmountErrorVideo(amountErrorVideo);
        }

        boolean b = paymentOrderService.updateById(paymentOrder);

        log.info("提交金额错误处理 更新卖出订单信息: {}, sql执行结果: {}", paymentOrder, b);

        return b;
    }


    /**
     * 金额错误 更新撮合列表
     *
     * @param matchingOrder
     * @param amountErrorImage
     * @param amountErrorVideo
     * @param orderActualAmount
     * @return {@link Boolean}
     */
    private Boolean matchingOrderToAmountError(MatchingOrder matchingOrder, String amountErrorImage, String amountErrorVideo, BigDecimal orderActualAmount) {

        //更新撮合列表订单状态为: 申诉中
        matchingOrder.setStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新撮合列表订单申诉类型为: 金额错误
        matchingOrder.setDisplayAppealType(Integer.valueOf(DisplayAppealTypeEnum.AMOUNT_INCORRECT.getCode()));

        //更新申诉时间
        matchingOrder.setAppealTime(LocalDateTime.now());

        //设置实际金额
        matchingOrder.setOrderActualAmount(orderActualAmount);

        //更新金额错误图片
        matchingOrder.setAmountErrorImage(amountErrorImage);

        //更新金额错误提交时间
        matchingOrder.setAmountErrorSubmitTime(LocalDateTime.now());

        //更新金额错误视频
        if (amountErrorVideo != null) {
            matchingOrder.setAmountErrorVideo(amountErrorVideo);
        }

        boolean b = matchingOrderService.updateById(matchingOrder);

        log.info("提交金额错误处理 更新撮合列表订单信息: {}, sql执行结果: {}", matchingOrder, b);

        return b;
    }


    /**
     * 金额错误 更新买入订单
     *
     * @param collectionOrder
     * @param amountErrorImage
     * @param amountErrorVideo
     * @param orderActualAmount
     * @return {@link Boolean}
     */
    private Boolean collectionOrderToAmountError(CollectionOrder collectionOrder, String amountErrorImage, String amountErrorVideo, BigDecimal orderActualAmount) {

        //更新买入订单状态为: 申诉中
        collectionOrder.setOrderStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新申诉时间
        collectionOrder.setAppealTime(LocalDateTime.now());

        //设置实际金额
        collectionOrder.setActualAmount(orderActualAmount);

        //更新金额错误图片
        collectionOrder.setAmountErrorImage(amountErrorImage);

        //更新金额错误提交时间
        collectionOrder.setAmountErrorSubmitTime(LocalDateTime.now());

        //更新金额错误视频
        if (amountErrorVideo != null) {
            collectionOrder.setAmountErrorVideo(amountErrorVideo);
        }

        boolean b = collectionOrderService.updateById(collectionOrder);

        log.info("提交金额错误处理 更新买入订单信息: {}, sql执行结果: {}", collectionOrder, b);

        return b;
    }

    /**
     * 查询匹配池订单下面的子订单 并根据子订单状态 更新匹配池订单状态
     *
     * @param matchOrder
     */
    @Override
    public void updateMatchPoolOrderStatus(String matchOrder) {

        //获取匹配池订单
        MatchPool matchPoolOrder = matchPoolService.getMatchPoolOrderByOrderNo(matchOrder);

        //判断匹配池订单 剩余金额是否为0
        if (matchPoolOrder != null && matchPoolOrder.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {

            //查询该匹配池下面所有的子订单
            List<PaymentOrder> subOrders = paymentOrderService.getPaymentOrderByByMatchOrder(matchPoolOrder.getMatchOrder());

            //判断是否有未完成的订单
            boolean anyInProgress = subOrders.stream().anyMatch(order -> isInProgress(order.getOrderStatus()));

            if (anyInProgress) {
                //当前有未完成的订单 将匹配池订单改为 进行中状态
                matchPoolService.updateMatchPoolStatus(matchPoolOrder.getId(), OrderStatusEnum.IN_PROGRESS.getCode());
            } else {
                //当前没有未完成的订单 判断子订单状态是否全部是已取消
                boolean allCanceled = subOrders.stream().allMatch(order -> OrderStatusEnum.WAS_CANCELED.getCode().equals(order.getOrderStatus()));

                if (allCanceled) {
                    //子订单状态全部是已取消 将匹配池订单状态改为已取消
                    boolean updateMatchPoolCancelStatus = matchPoolService.updateMatchPoolStatus(matchPoolOrder.getId(), OrderStatusEnum.WAS_CANCELED.getCode());

                    if (updateMatchPoolCancelStatus) {
                        //匹配池订单改为已取消 尝试减少 该订单收款
                        upiTransactionService.decrementDailyTransactionCountIfApplicable(matchPoolOrder.getUpiId(), matchPoolOrder.getMatchOrder());
                    }
                } else {
                    //子订单状态有订单不是已取消 将匹配池订单状态改为已完成
                    matchPoolService.updateMatchPoolStatus(matchPoolOrder.getId(), OrderStatusEnum.SUCCESS.getCode());
                }
            }
        }
    }


    /**
     * 取消申请 金额错误
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult cancelApplication(PlatformOrderReq platformOrderReq) {

        //分布式锁key ar-wallet-cancelApplication+订单号
        String key = "ar-wallet-cancelApplication" + platformOrderReq.getPlatformOrder();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取当前会员信息
                MemberInfo memberInfo = memberInfoService.getMemberInfo();

                if (memberInfo == null) {
                    log.error("取消申请 金额错误 处理失败: 获取会员信息失败");
                    return RestResult.failure(ResultCode.RELOGIN);
                }

                //获取卖出订单信息 加上排他行锁
                PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrderReq.getPlatformOrder());

                if (paymentOrder == null) {
                    log.error("取消申请 金额错误 处理失败: 获取订单失败");
                    return RestResult.failure(ResultCode.RELOGIN);
                }

                //判断该笔订单是不是金额错误状态 如果不是 则驳回
                if (!paymentOrder.getOrderStatus().equals(OrderStatusEnum.AMOUNT_ERROR.getCode())) {
                    log.error("取消申请 金额错误 处理失败: 订单状态不是金额错误状态, req: {}, 订单状态: {}", platformOrderReq, paymentOrder.getOrderStatus());
                    return RestResult.failure(ORDER_STATUS_VERIFICATION_FAILED);
                }

                String memberId = String.valueOf(memberInfo.getId());

                //判断该比订单是否属于该会员
                if (!paymentOrder.getMemberId().equals(memberId)) {
                    log.error("取消申请 金额错误 处理失败: 订单校验失败");
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(paymentOrder.getMatchingPlatformOrder());

                //获取买入订单 加上排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(matchingOrder.getCollectionPlatformOrder());

                //查看订单确认中剩余时间
                log.info("取消申请金额错误, req: {}, 订单确认中剩余时间: {}", platformOrderReq);
                long confirmRemainingTime = redisUtil.getConfirmRemainingTime(paymentOrder.getPlatformOrder());

                // 确认中剩余时间大于3秒 将订单状态改为 确认中
                if (confirmRemainingTime > 3) {
                    paymentOrder.setOrderStatus(OrderStatusEnum.CONFIRMATION.getCode());
                    matchingOrder.setStatus(OrderStatusEnum.CONFIRMATION.getCode());
                    collectionOrder.setOrderStatus(OrderStatusEnum.CONFIRMATION.getCode());
                    log.info("取消申请 金额错误处理: 确认中剩余时间大于3秒, 将订单状态改为确认中, 确认中剩余时间: {}, 订单号: {}", confirmRemainingTime, platformOrderReq.getPlatformOrder());
                } else {
                    //确认中剩余时间小于3秒, 将订单状态改为 确认超时
                    paymentOrder.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
                    matchingOrder.setStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
                    collectionOrder.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
                    log.info("取消申请 金额错误处理: 确认中剩余时间小于3秒, 将订单状态改为确认中, 确认中剩余时间: {}, 订单号: {}", confirmRemainingTime, platformOrderReq.getPlatformOrder());
                }

                //将订单的实际金额改为订单金额
                paymentOrder.setActualAmount(paymentOrder.getAmount());
                matchingOrder.setOrderActualAmount(matchingOrder.getOrderSubmitAmount());
                collectionOrder.setActualAmount(collectionOrder.getAmount());

                //清除金额错误图片
                paymentOrder.setAmountErrorImage(null);
                matchingOrder.setAmountErrorImage(null);
                collectionOrder.setAmountErrorImage(null);

                //清除金额错误视频
                paymentOrder.setAmountErrorVideo(null);
                matchingOrder.setAmountErrorVideo(null);
                collectionOrder.setAmountErrorVideo(null);

                //更新卖出订单
                paymentOrderService.updateById(paymentOrder);

                //更新买入订单
                collectionOrderService.updateById(collectionOrder);

                //更新撮合列表订单
                matchingOrderService.updateById(matchingOrder);

                //查看是否有申诉订单, 如果有的话 要把申诉订单关掉

                //查询申诉订单 加上排他行锁
                AppealOrder appealOrder = appealOrderMapper.selectAppealOrderBywithdrawOrderNoForUpdate(paymentOrder.getPlatformOrder());

                log.info("取消金额错误申请, 订单是申诉中状态, 将申诉订单改为未支付, 申诉订单信息: {}, 卖出订单信息: {}", appealOrder, paymentOrder);

                String appealOrderStatus = String.valueOf(appealOrder.getAppealStatus());
                // 1是待处理
                if (appealOrder != null) {

                    if ("1".equals(appealOrderStatus) || "4".equals(appealOrderStatus)) {
                        //将申诉订单改为 未支付
                        UpdateWrapper<AppealOrder> appealOrderUpdateWrapper = new UpdateWrapper<>();

                        appealOrderUpdateWrapper.eq("withdraw_order_no", paymentOrder.getPlatformOrder()); // 使用卖出订单号作为更新条件
                        appealOrderUpdateWrapper.set("appeal_status", "3"); // 设置申诉订单的状态为未支付

                        // 执行更新
                        int updateAppealOrder = appealOrderMapper.update(null, appealOrderUpdateWrapper);

                        log.info("取消金额错误申请, 订单是申诉中状态, 将申诉订单改为未支付, 卖出订单信息: {}, sql执行结果: {}", paymentOrder, updateAppealOrder);
                    } else {
                        log.error("取消金额错误申请, 订单是申诉中状态, 获取申诉订单失败, 卖出订单信息: {}", paymentOrder);
                    }
                } else {
                    log.error("取消金额错误申请, 订单是申诉中状态, 获取申诉订单失败, 卖出订单信息: {}", paymentOrder);
                }

                log.info("取消申请 金额错误处理成功: 订单号: {}", platformOrderReq.getPlatformOrder());

                //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                final String finalCollectionplatformOrder = collectionOrder.getPlatformOrder();
                final String finalBuyMemberId = collectionOrder.getMemberId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        //通知买方
                        NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage = new NotifyOrderStatusChangeMessage(finalBuyMemberId, NotificationTypeEnum.NOTIFY_BUYER.getCode(), finalCollectionplatformOrder);

                        notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);
                    }
                });

                return RestResult.ok();
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("取消申请 金额错误处理失败  req: {} e: {}", platformOrderReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        log.error("取消申请 金额错误处理失败  req: {}", platformOrderReq);

        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 取消匹配
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult cancelMatching(PlatformOrderReq platformOrderReq) {


        //获取当前会员id
        Long memberId = UserContext.getCurrentUserId();

        if (memberId == null) {
            log.error("取消匹配处理失败: 获取会员id失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-sell+会员id
        String key = "ar-wallet-sell" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                MatchPool matchPool = null;

                PaymentOrder paymentOrder = null;


                //判断该笔订单是匹配池订单还是卖出订单
                if (platformOrderReq.getPlatformOrder().startsWith("C2C")) {

                    //匹配池订单

                    //查询匹配池订单 加上排他行锁
                    matchPool = matchPoolMapper.selectMatchPoolForUpdate(platformOrderReq.getPlatformOrder());

                    //校验该笔订单是否属于该会员 校验订单是否处于匹配中状态
                    if (matchPool == null || !matchPool.getMemberId().equals(String.valueOf(memberId)) || !OrderStatusEnum.BE_MATCHED.getCode().equals(matchPool.getOrderStatus())) {
                        log.error("取消匹配处理失败: 订单状态必须是匹配中状态才能取消 会员id: {}, 当前订单状态: {}, 订单信息: {}, req: {}", memberId, matchPool.getOrderStatus(), matchPool, platformOrderReq);
                        return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                    }

                    //判断该笔订单如果是已取消状态, 那么直接返回成功
                    if (matchPool.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
                        return RestResult.ok();
                    }

                    //获取会员信息 加上排他行锁
                    MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(matchPool.getMemberId()));

                    //账变前余额
                    BigDecimal previousBalance = memberInfo.getBalance();

                    //账变后余额
                    BigDecimal newBalance = memberInfo.getBalance().add(matchPool.getRemainingAmount());

                    //更新会员信息
                    cancelSellOrderUpdateMemberInfo(memberInfo, matchPool.getRemainingAmount());

                    //记录会员账变信息
                    memberAccountChangeService.recordMemberTransaction(String.valueOf(memberInfo.getId()), matchPool.getRemainingAmount(), MemberAccountChangeEnum.CANCEL_RETURN.getCode(), matchPool.getMatchOrder(), previousBalance, newBalance, "");

                    //获取收款信息 加上排他行锁
                    CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(matchPool.getCollectionInfoId());
                    //更新收款信息
                    cancelSellOrderUpdateCollectionInfo(collectionInfo, matchPool.getRemainingAmount());

                    //更新匹配池订单信息
                    cancelSellOrderUpdateMatchPool(matchPool, null);

                    log.info("取消匹配处理成功 会员账号: {}, 匹配池订单号: {}", memberInfo.getMemberAccount(), platformOrderReq.getPlatformOrder());

                } else if (platformOrderReq.getPlatformOrder().startsWith("MC")) {

                    //卖出订单

                    //查询卖出订单 加上排他行锁
                    paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrderReq.getPlatformOrder());

                    //校验该笔订单是否属于该会员 校验订单是否处于匹配超时状态
                    if (paymentOrder == null || !paymentOrder.getMemberId().equals(String.valueOf(memberId)) || !OrderStatusEnum.BE_MATCHED.getCode().equals(paymentOrder.getOrderStatus())) {
                        log.error("取消匹配处理失败: 订单状态为匹配中才能取消, 会员id: {}, 当前订单状态: {}, 订单信息: {}, req: {}", memberId, paymentOrder.getOrderStatus(), paymentOrder, platformOrderReq);
                        return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                    }

                    //判断该笔订单如果是已取消状态, 那么直接返回成功
                    if (paymentOrder.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
                        return RestResult.ok();
                    }

                    //获取会员信息 加上排他行锁
                    MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(paymentOrder.getMemberId()));

                    //账变前余额
                    BigDecimal previousBalance = memberInfo.getBalance();

                    //账变后余额
                    BigDecimal newBalance = memberInfo.getBalance().add(paymentOrder.getAmount());

                    //更新会员信息
                    cancelSellOrderUpdateMemberInfo(memberInfo, paymentOrder.getAmount());

                    //记录会员账变信息
                    memberAccountChangeService.recordMemberTransaction(String.valueOf(memberInfo.getId()), paymentOrder.getAmount(), MemberAccountChangeEnum.CANCEL_RETURN.getCode(), paymentOrder.getPlatformOrder(), previousBalance, newBalance, "");

                    //获取收款信息 加上排他行锁
                    CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(paymentOrder.getCollectionInfoId());
                    //更新收款信息
                    cancelSellOrderUpdateCollectionInfo(collectionInfo, paymentOrder.getAmount());

                    //更新卖出订单信息
                    cancelSellOrderUpdatePaymentOrder(paymentOrder, null);

                    log.info("取消匹配处理成功 会员账号: {}, 卖出订单号: {}", memberInfo.getMemberAccount(), platformOrderReq.getPlatformOrder());

                } else {
                    log.error("取消匹配处理失败 订单号错误 会员id :{}, 订单号: {}", memberId, platformOrderReq);
                    return RestResult.failure(ResultCode.ORDER_NUMBER_ERROR);
                }

                //注册事务同步回调机制
                final MatchPool finalMatchPool = matchPool;
                final PaymentOrder finalPaymentOrder = paymentOrder;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        //将订单从redis列表里面删除
                        redisUtil.deleteOrder(platformOrderReq.getPlatformOrder());

                        //推送最新的 金额列表给前端
                        memberSendAmountList.send();

                        // 从进行中订单缓存中移除
                        orderChangeEventService.processCancelSellOrder(NotifyOrderStatusChangeMessage.builder().platformOrder(platformOrderReq.getPlatformOrder()).memberId(String.valueOf(memberId)).build());

                        String upiId = (finalMatchPool != null) ? finalMatchPool.getUpiId() : finalPaymentOrder.getUpiId();

                        String cancelSellorderNo = (finalMatchPool != null) ? finalMatchPool.getMatchOrder() : finalPaymentOrder.getPlatformOrder();

                        //符合条件的话 将该收款信息 单日收款次数 -1
                        upiTransactionService.decrementDailyTransactionCountIfApplicable(upiId, cancelSellorderNo);
                    }
                });
                return RestResult.ok();
            } else {
                //没获取到锁 直接返回操作频繁
                return RestResult.failure(ResultCode.TOO_FREQUENT);
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("取消匹配处理失败 会员id: {}, req: {}, e: {}", memberId, platformOrderReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);

    }

    /**
     * 查看订单状态是否在进行中
     *
     * @param statusCode
     * @return boolean
     */
    private boolean isInProgress(String statusCode) {
        return OrderStatusEnum.BE_MATCHED.getCode().equals(statusCode) ||//待匹配
                OrderStatusEnum.MATCH_TIMEOUT.getCode().equals(statusCode) ||//匹配超时
                OrderStatusEnum.BE_PAID.getCode().equals(statusCode) ||//待支付
                OrderStatusEnum.CONFIRMATION.getCode().equals(statusCode) ||//确认中
                OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(statusCode) ||//确认超时
                OrderStatusEnum.COMPLAINT.getCode().equals(statusCode) ||//申诉中
                OrderStatusEnum.AMOUNT_ERROR.getCode().equals(statusCode) ||//金额错误
                OrderStatusEnum.IN_PROGRESS.getCode().equals(statusCode);//进行中
    }
}
