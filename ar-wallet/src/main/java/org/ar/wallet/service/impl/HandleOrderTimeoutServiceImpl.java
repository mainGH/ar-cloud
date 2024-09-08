package org.ar.wallet.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.req.MemberInfoCreditScoreReq;
import org.ar.common.redis.constants.RedisKeys;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.*;
import org.ar.wallet.rabbitmq.RabbitMQService;
import org.ar.wallet.req.CancelOrderReq;
import org.ar.wallet.service.*;
import org.ar.wallet.thirdParty.TelephoneClient;
import org.ar.wallet.thirdParty.TelephoneStatus;
import org.ar.wallet.util.RedisUtil;
import org.ar.wallet.util.StringUtil;
import org.ar.wallet.util.TradeConfigHelperUtil;
import org.ar.wallet.vo.BuyListVo;
import org.ar.wallet.webSocket.MemberSendAmountList;
import org.ar.wallet.webSocket.NotifyOrderStatusChangeSend;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.ar.common.core.result.ResultCode.SUCCESS;
import static org.ar.common.core.result.ResultCode.SYSTEM_EXECUTION_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandleOrderTimeoutServiceImpl implements HandleOrderTimeoutService {


    private final MatchPoolMapper matchPoolMapper;

    private final PaymentOrderMapper paymentOrderMapper;

    private final CollectionOrderMapper collectionOrderMapper;

    private final MatchingOrderMapper matchingOrderMapper;

    private final MemberSendAmountList memberSendAmountList;

    private final ITradeConfigService tradeConfigService;

    private final UsdtBuyOrderMapper usdtBuyOrderMapper;

    private final RedisUtil redisUtil;

    private final RedisTemplate redisTemplate;

    @Autowired
    private IMatchPoolService matchPoolService;

    private final MemberInfoMapper memberInfoMapper;

    private final IMemberInfoService memberInfoService;

    private final IPaymentOrderService paymentOrderService;

    private final RedissonUtil redissonUtil;

    private final RabbitMQService rabbitMQService;

    private final IMemberAccountChangeService memberAccountChangeService;

    private final TelephoneClient telephoneClient;

    @Autowired
    private ICollectionInfoService collectionInfoService;

    @Autowired
    private ISellService sellService;

    @Autowired
    private CollectionInfoMapper collectionInfoMapper;

    @Autowired
    private UpiTransactionService upiTransactionService;

    @Autowired
    private IBuyService buyService;

    @Autowired
    private OrderChangeEventService orderChangeEventService;

    @Autowired
    private TradeConfigHelperUtil tradeConfigHelperUtil;
    @Autowired
    private NotifyOrderStatusChangeSend notifyOrderStatusChangeSend;


    /**
     * 钱包用户确认超时处理
     *
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean handleWalletUserConfirmationTimeout(String platformOrder) {

        log.info("钱包用户确认超时处理: 订单号: {}", platformOrder);

        //分布式锁key ar-wallet-handleWalletUserConfirmationTimeout+订单号
        String key = "ar-wallet-handleWalletUserConfirmationTimeout" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(platformOrder);
                //查看撮合列表订单是不是确认中状态
                if (matchingOrder != null && OrderStatusEnum.CONFIRMATION.getCode().equals(matchingOrder.getStatus()) && matchingOrder.getAuditDelayTime() == null) {

                    //将撮合列表订单状态改为确认超时
                    matchingOrderMapper.updateStatus(matchingOrder.getId(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());

                    //获取买入订单 加上排他行锁
                    CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(matchingOrder.getCollectionPlatformOrder());
                    //将买入订单状态改为确认超时
                    collectionOrderMapper.updateStatus(collectionOrder.getId(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());

                    //获取卖出订单 加上排他行锁
                    PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());
                    //将卖出订单状态改为确认超时
                    paymentOrderMapper.updateStatus(paymentOrder.getId(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());

                    //获取该笔订单的收款upi  然后发送短信通知upi绑定的号码
                    CollectionInfo paymentDetailsByUpiIdAndUpiName = collectionInfoService.getPaymentDetailsByUpiIdAndUpiName(paymentOrder.getUpiId(), paymentOrder.getUpiName());

                    String mobileNumber = null;

                    if (paymentDetailsByUpiIdAndUpiName != null) {
                        mobileNumber = paymentDetailsByUpiIdAndUpiName.getMobileNumber();
                    }

                    //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                    final String finalMobileNumber = mobileNumber;
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {

                            if (finalMobileNumber != null) {
                                ArrayList<String> objects = new ArrayList<>();

                                String telephone = StringUtil.startsWith91(finalMobileNumber) ? finalMobileNumber : "91" + finalMobileNumber;

                                objects.add(telephone);

                                //确认超时 打电话通知卖方
                                List<TelephoneStatus> telephoneStatuses = telephoneClient.sendVoice(objects);

                                for (TelephoneStatus telephoneStatus : telephoneStatuses) {
                                    if (telephoneStatus != null && telephoneStatus.getStatus() == true) {
                                        log.info("钱包用户确认超时, 语音通知卖出会员成功, 通知手机号: {}", telephone);
                                    } else {
                                        log.error("钱包用户确认超时, 语音通知卖出会员失败, 通知手机号: {}", telephone);
                                    }
                                }

                                //发送延时3分钟的MQ 3分钟后如果该笔订单还是确认超时状态, 再次语音通知卖方
                                //发送3分钟后语音通知卖方的MQ消息
                                long millis = TimeUnit.MINUTES.toMillis(3);
                                TaskInfo taskInfo = new TaskInfo(paymentOrder.getPlatformOrder(), TaskTypeEnum.NOTIFY_SELLER_BY_VOICE.getCode(), System.currentTimeMillis());

                                rabbitMQService.sendTimeoutTask(taskInfo, millis);
                            } else {
                                log.error("钱包用户确认超时, 语音通知卖出会员失败, 获取手机号失败");
                            }

                            // 确认超时一定时间后标记订单为风控超时订单
                            TradeConfig tradeConfig = tradeConfigService.getById(1);
                            // 从配置表获取 获取会员确认超时风控标记时长 并将分钟转为毫秒
                            long millis = TimeUnit.MINUTES.toMillis(tradeConfig.getWarningConfirmOvertimeNotOperated());
                            // 发送会员确认超时风控标记的MQ消息
                            TaskInfo taskInfo = new TaskInfo(paymentOrder.getPlatformOrder() + "|" + paymentOrder.getMemberId(), TaskTypeEnum.RISK_TAG_ON_MEMBER_CONFIRMATION_TIMEOUT.getCode(), System.currentTimeMillis());
                            rabbitMQService.sendTimeoutTask(taskInfo, millis);

                            // 发送会员确认超时自动取消订单的MQ消息
                            /*long cancelOrderMillis = TimeUnit.MINUTES.toMillis(arProperty.getConfirmTimeoutCancelOrderTime());
                            TaskInfo cancleOrdertaskInfo = new TaskInfo(collectionOrder.getPlatformOrder() + "|" + paymentOrder.getMemberId(), TaskTypeEnum.AUTO_CANCEL_ORDER_ON_WALLET_MEMBER_CONFIRM_TIMEOUT.getCode(), System.currentTimeMillis());
                            rabbitMQService.sendTimeoutTask(cancleOrdertaskInfo, cancelOrderMillis);*/
                        }
                    });

                    return Boolean.TRUE;
                } else {
                    //订单不是确认中状态 直接消费成功
                    return Boolean.TRUE;
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("钱包用户确认超时处理失败: {}", e);
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
     * 商户会员确认超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean handleMerchantMemberConfirmationTimeout(String platformOrder) {

        log.info("商户会员确认超时处理: 订单号: {}", platformOrder);

        //分布式锁key ar-wallet-handleMerchantMemberConfirmationTimeout+订单号
        String key = "ar-wallet-handleMerchantMemberConfirmationTimeout" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(platformOrder);
                //查看撮合列表订单是不是确认中状态
                if (matchingOrder != null && OrderStatusEnum.CONFIRMATION.getCode().equals(matchingOrder.getStatus())) {

                    //将撮合列表订单状态改为确认超时
                    matchingOrderMapper.updateStatus(matchingOrder.getId(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());

                    //获取买入订单 加上排他行锁
                    CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(matchingOrder.getCollectionPlatformOrder());
                    //将买入订单状态改为确认超时
                    collectionOrderMapper.updateStatus(collectionOrder.getId(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());

                    //获取卖出订单 加上排他行锁
                    PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());
                    //将卖出订单状态改为确认超时
                    paymentOrderMapper.updateStatus(paymentOrder.getId(), OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());


                    //获取该笔订单的收款upi  然后发送短信通知upi绑定的号码
                    CollectionInfo paymentDetailsByUpiIdAndUpiName = collectionInfoService.getPaymentDetailsByUpiIdAndUpiName(paymentOrder.getUpiId(), paymentOrder.getUpiName());

                    String mobileNumber = null;

                    if (paymentDetailsByUpiIdAndUpiName != null) {
                        mobileNumber = paymentDetailsByUpiIdAndUpiName.getMobileNumber();
                    }

                    //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                    final String finalMobileNumber = mobileNumber;
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {

                            if (finalMobileNumber != null) {
                                ArrayList<String> objects = new ArrayList<>();

                                String telephone = StringUtil.startsWith91(finalMobileNumber) ? finalMobileNumber : "91" + finalMobileNumber;

                                objects.add(telephone);

                                //确认超时 打电话通知卖方
                                List<TelephoneStatus> telephoneStatuses = telephoneClient.sendVoice(objects);

                                for (TelephoneStatus telephoneStatus : telephoneStatuses) {
                                    if (telephoneStatus != null && telephoneStatus.getStatus() == true) {
                                        log.info("商户会员确认超时, 语音通知卖出会员成功");
                                    } else {
                                        log.error("商户会员确认超时, 语音通知卖出会员失败");
                                    }
                                }
                            } else {
                                log.error("商户会员确认超时, 语音通知卖出会员失败, 获取手机号失败");
                            }
                        }
                    });

                    return Boolean.TRUE;
                } else {
                    //订单不是确认中状态 直接消费成功
                    return Boolean.TRUE;
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("商户会员确认超时处理失败: {}", e);
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
     * 钱包用户卖出匹配超时处理
     *
     * @param platformOrder
     * @param lastUpdateTimestamp
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean handleWalletUserSaleMatchTimeout(String platformOrder, Long lastUpdateTimestamp) {

        log.info("处理钱包用户匹配超时: 订单号: {}", platformOrder);

        //分布式锁key ar-wallet-handleWalletUserSaleMatchTimeout+订单号
        String key = "ar-wallet-handleWalletUserSaleMatchTimeout" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        BuyListVo orderDetails = null;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取redis订单信息 以便事务执行失败了进行补偿性操作
                orderDetails = redisUtil.getOrderDetails(platformOrder);

                //判断是否拆单
                if (platformOrder.startsWith("C2C")) {
                    //拆单
                    //查询匹配池订单
                    MatchPool matchPool = matchPoolMapper.selectMatchPoolForUpdate(platformOrder);

                    //查看订单的匹配时间戳是否和消息的匹配时间戳一致 如果一致 才做处理
                    if (matchPool != null && String.valueOf(matchPool.getLastUpdateTimestamp()).equals(String.valueOf(lastUpdateTimestamp))) {
                        //如果该笔订单状态处于匹配中 那么就改为匹配超时
                        if (matchPool != null && OrderStatusEnum.BE_MATCHED.getCode().equals(matchPool.getOrderStatus())) {

                            //将订单从redis列表里面删除
                            redisUtil.deleteOrder(platformOrder);

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                            //将匹配池订单改为 匹配超时 并且将匹配超时字段设置为1
                            matchPoolMapper.updateStatusAndMatchTimeout(matchPool.getId(), OrderStatusEnum.MATCH_TIMEOUT.getCode(), "1");

                            //获取配置信息
                            TradeConfig tradeConfig = tradeConfigService.getById(1);

                            //从配置表获取 获取匹配超时自动取消时长 并将分钟转为毫秒
                            long millis = TimeUnit.MINUTES.toMillis(tradeConfig.getMatchOverTimeAutoCancelDuration());

                            //发送匹配超时自动取消的MQ消息
                            TaskInfo taskInfo = new TaskInfo(platformOrder + "|" + matchPool.getMemberId(), TaskTypeEnum.AUTO_CANCEL_ORDER_ON_MATCH_TIMEOUT.getCode(), System.currentTimeMillis());
                            rabbitMQService.sendTimeoutTask(taskInfo, millis);

                            return Boolean.TRUE;
                        } else {
                            //该笔订单状态不处于匹配中 直接消费成功
                            return Boolean.TRUE;
                        }
                    } else {
                        //匹配时间戳不一致 直接消费成功
                        return Boolean.TRUE;
                    }
                } else if (platformOrder.startsWith("MC")) {
                    //1对1
                    //获取卖出订单
                    PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrder);

                    //查看订单的匹配时间戳是否和消息的匹配时间戳一致 如果一致 才做处理
                    if (paymentOrder != null && String.valueOf(paymentOrder.getLastUpdateTimestamp()).equals(String.valueOf(lastUpdateTimestamp))) {
                        //如果该笔订单状态处于匹配中 那么就改为匹配超时
                        if (paymentOrder != null && OrderStatusEnum.BE_MATCHED.getCode().equals(paymentOrder.getOrderStatus())) {

                            //将订单从redis列表里面删除
                            redisUtil.deleteOrder(platformOrder);

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                            //将卖出订单改为 匹配超时 并且将匹配超时字段设置为1
                            paymentOrderMapper.updateStatusAndMatchTimeout(paymentOrder.getId(), OrderStatusEnum.MATCH_TIMEOUT.getCode(), "1");

                            //获取配置信息
                            TradeConfig tradeConfig = tradeConfigService.getById(1);

                            //从配置表获取 获取匹配超时自动取消时长 并将分钟转为毫秒
                            long millis = TimeUnit.MINUTES.toMillis(tradeConfig.getMatchOverTimeAutoCancelDuration());

                            //发送匹配超时自动取消的MQ消息
                            TaskInfo taskInfo = new TaskInfo(platformOrder + "|" + paymentOrder.getMemberId(), TaskTypeEnum.AUTO_CANCEL_ORDER_ON_MATCH_TIMEOUT.getCode(), System.currentTimeMillis());
                            rabbitMQService.sendTimeoutTask(taskInfo, millis);

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                            return Boolean.TRUE;
                        } else {
                            //该笔订单状态不处于匹配中 直接消费成功
                            return Boolean.TRUE;
                        }
                    } else {
                        //匹配时间戳不一致 直接消费成功
                        return Boolean.TRUE;
                    }
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("钱包用户卖出匹配超时处理失败: ", e);
            //匹配超时处理失败了 将卖出订单信息添加回redis订单列表
            addOrderIdToList(orderDetails);
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
     * 商户会员卖出匹配超时处理
     *
     * @param platformOrder
     * @param lastUpdateTimestamp
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean handleMerchantMemberSaleMatchTimeout(String platformOrder, Long lastUpdateTimestamp) {

        log.info("处理商户用户匹配超时: 订单号: {}", platformOrder);

        //分布式锁key ar-wallet-handleMerchantMemberSaleMatchTimeout+订单号
        String key = "ar-wallet-handleMerchantMemberSaleMatchTimeout" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        BuyListVo orderDetails = null;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取redis订单信息 以便事务执行失败了进行补偿性操作
                orderDetails = redisUtil.getOrderDetails(platformOrder);

                //判断是否拆单
                if (platformOrder.startsWith("C2C")) {
                    //拆单
                    //查询匹配池订单
                    MatchPool matchPool = matchPoolMapper.selectMatchPoolForUpdate(platformOrder);

                    //查看订单的匹配时间戳是否和消息的匹配时间戳一致 如果一致 才做处理
                    if (matchPool != null && String.valueOf(matchPool.getLastUpdateTimestamp()).equals(String.valueOf(lastUpdateTimestamp))) {
                        //如果该笔订单状态处于匹配中 那么就改为匹配超时
                        if (matchPool != null && OrderStatusEnum.BE_MATCHED.getCode().equals(matchPool.getOrderStatus())) {

                            //将订单从redis列表里面删除
                            redisUtil.deleteOrder(platformOrder);

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                            //将匹配池订单改为 匹配超时 并且将匹配超时字段设置为1
                            matchPoolMapper.updateStatusAndMatchTimeout(matchPool.getId(), OrderStatusEnum.MATCH_TIMEOUT.getCode(), "1");

                            //获取配置信息
                            TradeConfig tradeConfig = tradeConfigService.getById(1);

                            //从配置表获取 获取匹配超时自动取消时长 并将分钟转为毫秒
                            long millis = TimeUnit.MINUTES.toMillis(tradeConfig.getMatchOverTimeAutoCancelDuration());

                            //发送匹配超时自动取消的MQ消息
                            TaskInfo taskInfo = new TaskInfo(platformOrder + "|" + matchPool.getMemberId(), TaskTypeEnum.AUTO_CANCEL_ORDER_ON_MATCH_TIMEOUT.getCode(), System.currentTimeMillis());
                            rabbitMQService.sendTimeoutTask(taskInfo, millis);


                            return Boolean.TRUE;
                        } else {
                            //该笔订单状态不处于匹配中 直接消费成功
                            return Boolean.TRUE;
                        }
                    } else {
                        //匹配时间戳不一致 直接消费成功
                        return Boolean.TRUE;
                    }

                } else if (platformOrder.startsWith("MC")) {
                    //1对1
                    //获取卖出订单
                    PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrder);

                    if (paymentOrder != null && String.valueOf(paymentOrder.getLastUpdateTimestamp()).equals(String.valueOf(lastUpdateTimestamp))) {
                        //如果该笔订单状态处于匹配中 那么就改为匹配超时
                        if (paymentOrder != null && OrderStatusEnum.BE_MATCHED.getCode().equals(paymentOrder.getOrderStatus())) {

                            //将订单从redis列表里面删除
                            redisUtil.deleteOrder(platformOrder);

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                            //将卖出订单改为 匹配超时 并且将匹配超时字段设置为1
                            paymentOrderMapper.updateStatusAndMatchTimeout(paymentOrder.getId(), OrderStatusEnum.MATCH_TIMEOUT.getCode(), "1");

                            //获取配置信息
                            TradeConfig tradeConfig = tradeConfigService.getById(1);

                            //从配置表获取 获取匹配超时自动取消时长 并将分钟转为毫秒
                            long millis = TimeUnit.MINUTES.toMillis(tradeConfig.getMatchOverTimeAutoCancelDuration());

                            //发送匹配超时自动取消的MQ消息
                            TaskInfo taskInfo = new TaskInfo(platformOrder + "|" + paymentOrder.getMemberId(), TaskTypeEnum.AUTO_CANCEL_ORDER_ON_MATCH_TIMEOUT.getCode(), System.currentTimeMillis());
                            rabbitMQService.sendTimeoutTask(taskInfo, millis);

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                            return Boolean.TRUE;
                        } else {
                            //该笔订单状态不处于匹配中 直接消费成功
                            return Boolean.TRUE;
                        }
                    } else {
                        //匹配时间戳不一致 直接消费成功
                        return Boolean.TRUE;
                    }
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("商户会员卖出匹配超时处理失败: {}", e);
            //匹配超时处理失败了 将卖出订单信息添加回redis订单列表
            addOrderIdToList(orderDetails);
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
     * 匹配超时处理 将卖出订单信息添加回redis订单列表
     *
     * @param orderDetails
     */
    public void addOrderIdToList(BuyListVo orderDetails) {
        if (orderDetails != null) {
            log.error("匹配超时处理失败 将卖出订单信息添加回redis订单列表, 订单信息: {}", orderDetails);
            redisUtil.addOrderIdToList(orderDetails, "1");

            //推送最新的 金额列表给前端
            memberSendAmountList.send();
        }
    }

    /**
     * 支付超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean handlePaymentTimeout(String platformOrder) {

        log.info("处理支付超时处理: 订单号: {}", platformOrder);

        //分布式锁key ar-wallet-buyCompleted+订单号
        String key = "ar-wallet-buyCompleted" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                PaymentOrder paymentOrder = null;
                MemberInfo memberInfo1 = null;
                //是否需要注册事务同步回调
                AtomicBoolean shouldRegisterTransactionCallback = new AtomicBoolean(false);

                boolean isSplitOrder = false;
                MatchPool matchPool = null;
                BigDecimal rAmout = null;

                //获取买入订单 加排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(platformOrder);

                //判断买入订单是否是待支付状态
                if (collectionOrder != null && OrderStatusEnum.BE_PAID.getCode().equals(collectionOrder.getOrderStatus())) {

                    //将支付订单状态改为支付超时
                    collectionOrderMapper.updateStatus(collectionOrder.getId(), OrderStatusEnum.PAYMENT_TIMEOUT.getCode());

                    //获取撮合列表订单
                    MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(collectionOrder.getMatchingPlatformOrder());
                    //将撮合列表订单状态改为支付超时
                    matchingOrderMapper.updateStatus(matchingOrder.getId(), OrderStatusEnum.PAYMENT_TIMEOUT.getCode());

                    //查询卖出订单 加上排他行锁
                    paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());

                    //获取会员信息
                    memberInfo1 = memberInfoService.getById(paymentOrder.getMemberId());

                    //判断卖出订单是否是子订单
                    if (StringUtils.isEmpty(paymentOrder.getMatchOrder())) {
                        //非拆单

                        //需要注册事务同步回调(将订单信息存入到Redis订单列表)
                        shouldRegisterTransactionCallback.set(true);

                        Long lastUpdateTimestamp = System.currentTimeMillis();

                        //将卖出订单改为待匹配 并且更新匹配时间戳
                        paymentOrderMapper.updateToReassignMatch(paymentOrder.getId(), OrderStatusEnum.BE_MATCHED.getCode(), lastUpdateTimestamp);

                        //根据会员标签获取对应配置信息
                        TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo1);

                        //从配置表获取 钱包用户卖出匹配时长 并将分钟转为毫秒
                        long millis = TimeUnit.MINUTES.toMillis(schemeConfigByMemberTag.getSchemeSellMatchingDuration());


                        //将最后匹配时间存储到Redis 过期时间为12小时
                        String redisLastMatchTimeKey = RedisKeys.ORDER_LASTMATCHTIME + platformOrder;
                        redisTemplate.opsForValue().set(redisLastMatchTimeKey, lastUpdateTimestamp);
                        redisTemplate.expire(redisLastMatchTimeKey, 12, TimeUnit.HOURS);


                        //发送匹配超时的MQ消息
                        TaskInfo taskInfo = new TaskInfo(paymentOrder.getPlatformOrder(), TaskTypeEnum.WALLET_MEMBER_SALE_MATCH_TIMEOUT.getCode(), lastUpdateTimestamp);
                        rabbitMQService.sendTimeoutTask(taskInfo, millis);


//                        QueueInfo collectQueueInfo = new QueueInfo(RabbitMqConstants.AR_WALLET_DELAYED_ORDER_TIMEOUT_QUEUE_NAME, paymentOrder.getPlatformOrder(), OrderTimeOutEnum.WALLET_MEMBER_SALE_MATCH_TIMEOUT.getCode(), lastUpdateTimestamp);
//                        rabbitMQUtil.sendDelayedMessage(paymentOrder.getPlatformOrder(), Integer.parseInt(String.valueOf(millis)), new CorrelationData(JSON.toJSONString(collectQueueInfo)));

                        //将匹配倒计时记录到redis 卖出订单
                        redisUtil.setMatchExpireTime(paymentOrder.getPlatformOrder(), schemeConfigByMemberTag.getSchemeSellMatchingDuration());
                    } else {

                        //拆单
                        log.info("支付超时处理, 拆单: {}", platformOrder);

                        isSplitOrder = true;

                        //查询母订单 加上排他行锁
                        matchPool = matchPoolMapper.selectMatchPoolForUpdate(paymentOrder.getMatchOrder());

                        if (matchPool != null) {

                            //判断母订单状态如果是匹配中 那么将订单金额退回到母订单
                            if (matchPool.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode())) {

                                log.info("支付超时处理, 母订单状态是匹配中 将订单金额退回到母订单: {}", platformOrder);

                                //事务操作成功后  更新金额列表匹配池的信息

                                //是否需要注册事务同步回调
                                shouldRegisterTransactionCallback.set(true);

                                //剩余金额
                                rAmout = matchPool.getRemainingAmount();

                                //将订单金额退回到母订单剩余金额
                                matchPool.setRemainingAmount(matchPool.getRemainingAmount().add(paymentOrder.getAmount()));

                                //将匹配池进行中订单数 - 1
                                matchPool.setInProgressOrderCount(matchPool.getInProgressOrderCount() - 1);

                                // 已完成订单数 + 1
                                matchPool.setCompletedOrderCount(matchPool.getCompletedOrderCount() + 1);

                                //更新匹配池订单信息
                                matchPoolService.updateById(matchPool);

                                log.info("支付超时处理, 对应的卖出订单拆单 注册事务同步回调: {}", platformOrder);

                            } else {
                                //母订单状态不是匹配中  那么将订单金额退回到用户余额

                                //将匹配池进行中订单数 - 1
                                matchPool.setInProgressOrderCount(matchPool.getInProgressOrderCount() - 1);

                                // 已完成订单数 + 1
                                matchPool.setCompletedOrderCount(matchPool.getCompletedOrderCount() + 1);

                                //更新匹配池订单信息
                                matchPoolService.updateById(matchPool);

                                //获取用户信息 加上排他行锁
                                MemberInfo memberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(matchPool.getMemberId()));

                                BigDecimal previousBalance = memberInfo.getBalance();

                                //将订单金额退回到用户余额
                                memberInfo.setBalance(memberInfo.getBalance().add(paymentOrder.getAmount()));

                                //将冻结金额 减去该笔订单金额
                                memberInfo.setFrozenAmount(memberInfo.getFrozenAmount().subtract(paymentOrder.getAmount()));

                                //更新会员信息
                                memberInfoService.updateById(memberInfo);

                                BigDecimal newBalance = memberInfo.getBalance();

                                //记录会员账变信息
                                memberAccountChangeService.recordMemberTransaction(
                                        String.valueOf(memberInfo.getId()), paymentOrder.getAmount(),
                                        MemberAccountChangeEnum.CANCEL_RETURN.getCode(), paymentOrder.getPlatformOrder(), previousBalance, newBalance, "");

                                log.info("订单超时 母订单状态不是匹配中 将订单金额退回到用户余额 记录会员账变信息 会员账号: {}, 账变前余额: {}, 账变金额: {}, 账变后余额: {}", memberInfo.getMemberAccount(), previousBalance, paymentOrder.getAmount(), newBalance);
                            }

                            //将卖出订单状态改为 支付超时
                            paymentOrder.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());

                            paymentOrderService.updateById(paymentOrder);
                        } else {
                            //匹配池订单不存在 直接消费成功
                            return Boolean.TRUE;
                        }
                    }
                    // 更新信用分
                    log.info("支付超时处理, 更新信用分, 买家: {}", collectionOrder.getMemberId());
                    memberInfoService.updateCreditScore(MemberInfoCreditScoreReq.builder().id(Long.valueOf(collectionOrder.getMemberId())).eventType(CreditEventTypeEnum.PAYMENT_TIMEOUT.getCode()).tradeType(1).build());
                    //记录失败次数
                    redisUtil.recordMemberBuyFailure(collectionOrder.getMemberId());

                    // 注册事务同步回调
                    final PaymentOrder finalPaymentOrder = paymentOrder;
                    final MemberInfo finalMemberInfo = memberInfo1;
                    final boolean finalShouldRegisterTransactionCallback = shouldRegisterTransactionCallback.get();
                    final boolean finalIsSplitOrder = isSplitOrder;
                    final MatchPool finalMatchPool = matchPool;
                    final BigDecimal finalRAmout = rAmout;

                    if (finalPaymentOrder != null && finalMemberInfo != null) {

                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {

                                if (finalShouldRegisterTransactionCallback) {

                                    log.info("支付超时处理, 执行事务同步回调: {}", platformOrder);

                                    // 事务提交后执行的Redis操作
                                    BuyListVo buyListVo = new BuyListVo();

                                    if (!finalIsSplitOrder) {
                                        //非拆单

                                        log.info("支付超时处理, 对应的卖出订单非拆单 执行事务同步回调: {}", platformOrder);

                                        //订单号
                                        buyListVo.setPlatformOrder(finalPaymentOrder.getPlatformOrder());
                                        //订单金额
                                        buyListVo.setAmount(finalPaymentOrder.getAmount());
                                        //最小限额 非拆单 就是订单金额
                                        buyListVo.setMinimumAmount(finalPaymentOrder.getAmount());
                                        //最大限额 非拆单 就是订单金额
                                        buyListVo.setMaximumAmount(finalPaymentOrder.getAmount());
                                    } else {

                                        log.info("支付超时处理, 对应的卖出订单拆单 执行事务同步回调: {}", platformOrder);

                                        //拆单

                                        //删除之前的Redis订单
                                        redisUtil.deleteOrder(finalMatchPool.getMatchOrder());
                                        //订单号
                                        buyListVo.setPlatformOrder(finalMatchPool.getMatchOrder());

                                        //拆单 最大限额 是 之前匹配池剩余金额 + 当前支付超时对应的卖出订单金额
                                        buyListVo.setMaximumAmount(finalRAmout.add(finalPaymentOrder.getAmount()));
                                        //订单金额 是 之前匹配池剩余金额 + 当前支付超时对应的卖出订单金额
                                        buyListVo.setAmount(finalRAmout.add(finalPaymentOrder.getAmount()));
                                        //最小限额 匹配池的最小限额
                                        buyListVo.setMinimumAmount(finalMatchPool.getMinimumAmount());

                                    }

                                    //支付方式 目前只有UPI 先写死
                                    buyListVo.setPayType(PayTypeEnum.INDIAN_UPI.getCode());
                                    //头像
                                    buyListVo.setAvatar(finalMemberInfo.getAvatar());
                                    //会员id
                                    buyListVo.setMemberId(String.valueOf(finalMemberInfo.getId()));
                                    //会员类型
                                    buyListVo.setMemberType(finalMemberInfo.getMemberType());
                                    //信用分
                                    buyListVo.setCreditScore(finalMemberInfo.getCreditScore());
                                    //存入redis买入金额列表
                                    redisUtil.addOrderIdToList(buyListVo, "2");

                                    //推送最新的 金额列表给前端
                                    memberSendAmountList.send();
                                    //通知买方
                                    NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage = new NotifyOrderStatusChangeMessage(collectionOrder.getMemberId(), NotificationTypeEnum.NOTIFY_BUYER.getCode(), collectionOrder.getPlatformOrder());
                                    notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);

                                } else {
                                    log.info("支付超时处理, 事务同步回调为false: {}", platformOrder);
                                }

                                //拆单才进行下面操作
                                if (finalIsSplitOrder && finalPaymentOrder != null && StringUtils.isNotEmpty(finalPaymentOrder.getMatchOrder())) {
                                    //匹配池订单有子订单 查询全部子订单 并根据子订单状态更新匹配池订单状态
                                    sellService.updateMatchPoolOrderStatus(finalPaymentOrder.getMatchOrder());
                                }
                            }
                        });
                    }
                    return Boolean.TRUE;
                } else {
                    //订单不是待支付状态 直接消费成功
                    return Boolean.TRUE;
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("支付超时处理失败: {}", e);
            redisUtil.deleteOrder(platformOrder);
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
     * USDT支付超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean handleUsdtPaymentTimeout(String platformOrder) {

        log.info("处理USDT支付超时处理: 订单号: {}", platformOrder);

        //分布式锁key ar-wallet-handleUsdtPaymentTimeout+订单号
        String key = "ar-wallet-handleUsdtPaymentTimeout" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取USDT买入订单 加上排他行锁
                UsdtBuyOrder usdtBuyOrder = usdtBuyOrderMapper.selectUsdtBuyOrderForUpdate(platformOrder);

                //判断买入订单是否是待支付状态
                if (usdtBuyOrder != null && OrderStatusEnum.BE_PAID.getCode().equals(usdtBuyOrder.getStatus())) {

                    //将USDT订单状态改为支付超时
                    usdtBuyOrderMapper.updateStatus(usdtBuyOrder.getId(), OrderStatusEnum.PAYMENT_TIMEOUT.getCode());

                    //记录失败次数
                    redisUtil.recordMemberBuyFailure(usdtBuyOrder.getMemberId());

                    return Boolean.TRUE;
                } else {
                    //订单不是待支付状态 直接消费成功
                    return Boolean.TRUE;
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("USDT支付超时处理失败: {}", e);
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
     * 匹配超时时自动取消订单
     *
     * @param taskInfo
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean autoCancelOrderOnMatchTimeout(String taskInfo) {

        // 检查字符串是否包含分隔符"|"
        if (!taskInfo.contains("|")) {
            // 没有分隔符 直接消费成功
            log.info("匹配超时后自动取消订单, MQ订单号没有包含| 直接消费成功, taskInfo: {}", taskInfo);
            return true;
        }

        // 分割字符串
        String[] parts = taskInfo.split("\\|");

        //获取任务订单号
        String platformOrder = parts[0];

        //获取会员id
        String memberId = parts[1];


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
                if (platformOrder.startsWith("C2C")) {

                    //匹配池订单

                    //查询匹配池订单 加上排他行锁
                    matchPool = matchPoolMapper.selectMatchPoolForUpdate(platformOrder);

                    //校验该笔订单是否属于该会员 校验订单是否处于匹配超时状态
                    if (matchPool == null || !matchPool.getMemberId().equals(memberId) || !OrderStatusEnum.MATCH_TIMEOUT.getCode().equals(matchPool.getOrderStatus())) {
                        log.error("匹配超时时自动取消订单处理失败: 订单状态必须是匹配超时状态才能取消 会员id: {}, 当前订单状态: {}, 订单信息: {}, req: {}", memberId, matchPool.getOrderStatus(), matchPool, platformOrder);
                        return true;
                    }

                    //判断该笔订单如果是已取消状态, 那么直接返回成功
                    if (matchPool.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
                        return true;
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
                    cancelSellOrderUpdateMatchPool(matchPool);

                    log.info("匹配超时时自动取消订单处理成功 会员账号: {}, 匹配池订单号: {}", memberInfo.getMemberAccount(), platformOrder);

                    //推送最新的 金额列表给前端
                    memberSendAmountList.send();

                } else if (platformOrder.startsWith("MC")) {

                    //卖出订单

                    //查询卖出订单 加上排他行锁
                    paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrder);

                    //校验该笔订单是否属于该会员 校验订单是否处于匹配超时状态
                    if (paymentOrder == null || !paymentOrder.getMemberId().equals(memberId) || !OrderStatusEnum.MATCH_TIMEOUT.getCode().equals(paymentOrder.getOrderStatus())) {
                        log.error("匹配超时时自动取消订单处理失败: 订单状态为匹配超时才能取消, 会员id: {}, 当前订单状态: {}, 订单信息: {}, req: {}", memberId, paymentOrder.getOrderStatus(), paymentOrder, platformOrder);
                        return true;
                    }

                    //判断该笔订单如果是已取消状态, 那么直接返回成功
                    if (paymentOrder.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
                        return true;
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
                    cancelSellOrderUpdatePaymentOrder(paymentOrder);

                    log.info("匹配超时时自动取消订单处理成功 会员账号: {}, 卖出订单号: {}", memberInfo.getMemberAccount(), platformOrder);

                } else {
                    log.error("匹配超时时自动取消订单处理失败 订单号错误 会员id :{}, 订单号: {}", memberId, platformOrder);
                    return true;
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
                        orderChangeEventService.processCancelSellOrder(NotifyOrderStatusChangeMessage.builder().platformOrder(platformOrder).memberId(memberId).build());

                        // 发送订单取消通知给前端
                        NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage = new NotifyOrderStatusChangeMessage(memberId, NotificationTypeEnum.NOTIFY_SELLER.getCode(), platformOrder);
                        notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);
                    }
                });

                return true;
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("取消卖出订单处理失败 订单号错误 会员id :{}, 订单号: {}, e: {}", memberId, platformOrder, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
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
     * 取消卖出订单 更新卖出订单状态
     *
     * @param paymentOrder
     * @return {@link Boolean}
     */
    Boolean cancelSellOrderUpdatePaymentOrder(PaymentOrder paymentOrder) {

        //将卖出订单改为已取消状态
        paymentOrder.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());

        //将卖出订单 取消匹配字段 设置为1
        paymentOrder.setCancelMatching(1);

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
     * @return {@link Boolean}
     */
    Boolean cancelSellOrderUpdateMatchPool(MatchPool matchPool) {

        //将卖出订单改为已取消状态
        matchPool.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());

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
     * 会员确认超时风控标记订单
     *
     * @param taskInfo
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public Boolean taggingOrderOnMemberConfirmTimeout(String taskInfo) {

        // 检查字符串是否包含分隔符"|"
        if (!taskInfo.contains("|")) {
            // 没有分隔符 直接消费成功
            log.info("会员确认超时风控标记订单, MQ订单号没有包含| 直接消费成功, taskInfo: {}", taskInfo);
            return true;
        }

        // 分割字符串
        String[] parts = taskInfo.split("\\|");

        //获取任务订单号
        String platformOrder = parts[0];

        //获取会员id
        String memberId = parts[1];


        //分布式锁key ar-wallet-sell+订单号
        String key = "ar-wallet-sell" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                PaymentOrder paymentOrder;

                if (!platformOrder.startsWith("MC")) {
                    log.error("会员确认超时风控标记订单处理失败 订单号错误 会员id :{}, 订单号: {}", memberId, platformOrder);
                    return true;
                }
                //查询卖出订单 加上排他行锁
                paymentOrder = paymentOrderMapper.selectPaymentForUpdate(platformOrder);

                //校验该笔订单是否属于该会员 校验订单是否处于匹配超时状态
                if (paymentOrder == null || !paymentOrder.getMemberId().equals(memberId) || !OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(paymentOrder.getOrderStatus())) {
                    log.error("会员确认超时风控标记订单处理失败: 订单状态为会员确认超时才能取消, 会员id: {}, 当前订单状态: {}, 订单信息: {}, req: {}", memberId, paymentOrder.getOrderStatus(), paymentOrder, platformOrder);
                    return true;
                }

                //判断该笔订单如果是已取消状态, 那么直接返回成功
                if (paymentOrder.getRiskTagTimeout() == 1) {
                    log.info("会员确认超时风控标记订单处理 订单已经标记为风控超时状态, 卖出订单号: {}", platformOrder);
                    return true;
                }
                paymentOrder.setRiskTagTimeout(1);
                //更新卖出订单信息
                boolean b = paymentOrderService.updateById(paymentOrder);
                log.info("会员确认超时风控标记订单处理成功 更新卖出订单风控超时状态: {}, sql执行结果: {}", paymentOrder, b);

                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(paymentOrder.getMatchingPlatformOrder());
                //查看撮合列表订单是不是确认中状态
                if (matchingOrder != null && matchingOrder.getRiskTagTimeout() != 1) {
                    //将撮合列表订单状态改为确认超时
                    matchingOrder.setRiskTagTimeout(1);
                    matchingOrder.setRiskOrderType(Integer.valueOf(RiskOrderTypeEnum.PAYMENT.getCode()));
                    int rowCount = matchingOrderMapper.updateById(matchingOrder);
                    log.info("会员确认超时风控标记订单处理成功 更新撮合订单风控超时状态: {}, sql执行结果: {}", matchingOrder, rowCount);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("会员确认超时风控标记订单处理失败 会员id :{}, 订单号: {}, e: ", memberId, platformOrder, e);
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    @Transactional
    @Override
    public Boolean autoCancelOrderOnMemberConfirmTimeout(String taskInfo) {

        // 检查字符串是否包含分隔符"|"
        if (!taskInfo.contains("|")) {
            // 没有分隔符 直接消费成功
            log.info("会员确认超时自动取消订单, MQ订单号没有包含| 直接消费成功, taskInfo: {}", taskInfo);
            return true;
        }

        // 分割字符串
        String[] parts = taskInfo.split("\\|");

        //获取任务订单号
        String buyPlatformOrder = parts[0];

        //获取会员id
        String sellMemberId = parts[1];

        CollectionOrder collectionOrder = collectionOrderMapper.getOrderByOrderNo(buyPlatformOrder);
        if (collectionOrder == null) {
            log.error("会员确认超时自动取消订单, 未查询到订单, 订单号: {}", buyPlatformOrder);
            return true;
        }

        // 取消订单
        CancelOrderReq cancelOrderReq = new CancelOrderReq();
        cancelOrderReq.setPlatformOrder(buyPlatformOrder);
        cancelOrderReq.setReason("确认超时超过48小时, 系统自动取消订单");
        MemberInfo memberInfo = memberInfoMapper.getMemberInfoById(collectionOrder.getMemberId());
        if (memberInfo == null) {
            log.error("会员确认超时自动取消订单, 未查询买家会员, 会员ID: {}", collectionOrder.getMemberId());
            return true;
        }
        log.info("会员确认超时自动取消订单, 订单号: {}", taskInfo);
        RestResult result = buyService.cancelPurchaseOrder(cancelOrderReq, OrderStatusEnum.BUY_FAILED, OrderStatusEnum.FAIL, OrderStatusEnum.MANUAL_COMPLETION, memberInfo);

        if(result.getCode().equals(SYSTEM_EXECUTION_ERROR.getCode())){
            return false;
        }
        if(result.getCode().equals(SUCCESS.getCode())){
            // 更新信用分
            log.info("会员确认超时自动取消订单, 更新信用积分, 卖出会员:{}", sellMemberId);
            memberInfoService.updateCreditScore(MemberInfoCreditScoreReq.builder().id(Long.valueOf(sellMemberId)).eventType(CreditEventTypeEnum.OVERTIME.getCode()).tradeType(2).build());
        }

        return true;
    }
}
