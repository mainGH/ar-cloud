package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.common.redis.constants.RedisKeys;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.*;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.rabbitmq.RabbitMQService;
import org.ar.wallet.req.*;
import org.ar.wallet.service.*;
import org.ar.wallet.thirdParty.MessageClient;
import org.ar.wallet.thirdParty.MessageStatus;
import org.ar.wallet.util.*;
import org.ar.wallet.vo.*;
import org.ar.wallet.webSocket.MemberSendAmountList;
import org.ar.wallet.webSocket.NotifyOrderStatusChangeSend;
import org.redisson.api.RLock;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IBuyServiceImpl implements IBuyService {

    private final IPaymentOrderService paymentOrderService;
    private final IMatchPoolService matchPoolService;
    private final RedissonUtil redissonUtil;
    private final ICollectionOrderService collectionOrderService;
    private final IMatchingOrderService matchingOrderService;
    private final IUsdtBuyOrderService usdtBuyOrderService;
    private final IUsdtConfigService usdtConfigService;
    private final IMemberInfoService memberInfoService;
    private final ICollectionInfoService collectionInfoService;

    private final MemberInfoMapper memberInfoMapper;
    private final TradeConfigMapper tradeConfigMapper;
    private final MatchPoolMapper matchPoolMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final CollectionOrderMapper collectionOrderMapper;
    private final MatchingOrderMapper matchingOrderMapper;
    private final CollectionInfoMapper collectionInfoMapper;
    private final IAppealOrderService appealOrderService;
    private final RedisUtil redisUtil;
    private final ITradeConfigService tradeConfigService;
    private final IMemberGroupService memberGroupService;
    private final IMemberAccountChangeService memberAccountChangeService;
    private final ICancellationRechargeService cancellationRechargeService;
    private final RedisTemplate redisTemplate;
    //从nacos获取配置
    private final ArProperty arProperty;
    private final RabbitMQService rabbitMQService;

    @Autowired
    private MemberSendAmountList memberSendAmountList;

    @Autowired
    private NotifyOrderStatusChangeSend notifyOrderStatusChangeSend;

    private final ISellService sellService;
    private final OrderNumberGeneratorUtil orderNumberGenerator;
    private final MessageClient messageClient;

    @Autowired
    private IMerchantInfoService merchantInfoService;

    @Autowired
    private AppealOrderMapper appealOrderMapper;

    @Autowired
    private ITradeIpBlacklistService tradeIpBlacklistService;

    @Value("${oss.baseUrl}")
    private String baseUrl;

    @Autowired
    private UpiTransactionService upiTransactionService;

    @Autowired
    private TradeConfigHelperUtil tradeConfigHelperUtil;
    @Autowired
    private ImageRecognitionService imageRecognitionService;

    @Autowired
    private IControlSwitchService controlSwitchService;

    /**
     * 获取买入金额列表
     *
     * @param buyListReq
     * @return {@link List}<{@link BuyListVo}>
     */
    @Override
    public PageReturn<BuyListVo> getBuyList(BuyListReq buyListReq, String memberId) {

//        if (buyListReq == null) {
//            buyListReq = new BuyListReq();
//        }
//
//        //固定30条记录
//        Integer pageSize = 30;
//
//        Page<PaymentOrder> pagePaymentOrder = new Page<>();
//        pagePaymentOrder.setCurrent(buyListReq.getPageNo());
//        //固定30条记录
//        pagePaymentOrder.setSize(pageSize);
//
//        //分页查询卖出订单
//        PageReturn<PaymentOrder> paymentOrderList = new PageReturn<>();
//        paymentOrderList = paymentOrderService.getOldestOrders(buyListReq, memberId, pagePaymentOrder);
//
//        ArrayList<BuyListVo> resList = new ArrayList<>();
//
//        for (PaymentOrder paymentOrder : paymentOrderList.getList()) {
//
//            BuyListVo buyListVo = new BuyListVo();
//            //订单金额
//            buyListVo.setAmount(paymentOrder.getAmount());
//            //平台订单号
//            buyListVo.setPlatformOrder(paymentOrder.getPlatformOrder());
//            //最小限额 不拆单 所以最小限额是订单金额
//            buyListVo.setMinimumAmount(paymentOrder.getAmount());
//            //最大限额 不拆掉 所以最大限额就是订单金额
//            buyListVo.setMaximumAmount(paymentOrder.getAmount());
//            //用户头像
//            buyListVo.setAvatar(paymentOrder.getAvatar());
//            //支付类型
//            buyListVo.setPayType(paymentOrder.getPayType());
//
//            resList.add(buyListVo);
//        }
//
//        log.info("获取买入金额列表 分页查询卖出订单: {}", paymentOrderList);
//
//        Page<MatchPool> pageMatchPool = new Page<>();
//        pageMatchPool.setCurrent(buyListReq.getPageNo());
//        //固定30条记录
//        pageMatchPool.setSize(pageSize);
//
//        //分页查询匹配池订单
//        PageReturn<MatchPool> matchPoolList = new PageReturn<>();
//
//        //查看是否够数量 不够的话就查询匹配池订单
//        if (resList.size() < pageSize) {
//
//            //分页查询匹配池订单
//            matchPoolList = matchPoolService.getOldestOrders(buyListReq, memberId, pageMatchPool);
//
//            log.info("获取买入金额列表 分页查询匹配池订单: {}", matchPoolList);
//
//            for (MatchPool matchPool : matchPoolList.getList()) {
//
//                BuyListVo buyListVo = new BuyListVo();
//                //订单金额
//                buyListVo.setAmount(matchPool.getAmount());
//                //平台订单号
//                buyListVo.setPlatformOrder(matchPool.getMatchOrder());
//                //最小限额
//                buyListVo.setMinimumAmount(matchPool.getMinimumAmount());
//                //最大限额
//                buyListVo.setMaximumAmount(matchPool.getMaximumAmount());
//                //用户头像
//                buyListVo.setAvatar(matchPool.getAvatar());
//                //支付类型
//                buyListVo.setPayType(matchPool.getPayType());
//
//                resList.add(buyListVo);
//
//                //判断条数是否足够
//                if (resList.size() >= pageSize) {
//                    break;
//                }
//            }
//        }
//
//        //返回数据
//        PageReturn<BuyListVo> buyListVoPageReturn = new PageReturn<>();
//        //数据列表
//        buyListVoPageReturn.setList(resList);
//        //每页显示条数
//        buyListVoPageReturn.setPageSize(Long.valueOf(pageSize));
//        //当前页码
//        buyListVoPageReturn.setPageNo(buyListReq.getPageNo());
//        //总记录数
//        buyListVoPageReturn.setTotal(paymentOrderList.getTotal() + matchPoolList.getTotal());
//
//

        //        return buyListVoPageReturn;

        if (buyListReq == null) {
            buyListReq = new BuyListReq();
        }

        buyListReq.setMemberId(memberId);

        //从redis里面获取买入金额列表
        PageReturn<BuyListVo> buyList = redisUtil.getBuyList(buyListReq);

        log.info("获取买入金额列表: {}, 会员id: {}", buyList, memberId);

        return buyList;
    }

    /**
     * 买入处理
     *
     * @param buyReq
     * @return {@link Boolean}
     */
    @Override
    @Transactional
    public RestResult buyProcessor(BuyReq buyReq, HttpServletRequest request) {

        //获取当前会员id
        Long memberId = UserContext.getCurrentUserId();
        if (memberId == null) {
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-buy+买入订单号
        String key1 = "ar-wallet-buy" + buyReq.getPlatformOrder();
        RLock lock1 = redissonUtil.getLock(key1);

        //分布式锁key ar-wallet-buy+会员id
        String key = "ar-wallet-sell" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req1 = false;
        boolean req = false;

        try {
            req1 = lock1.tryLock(10, TimeUnit.SECONDS);

            if (req1) {

                //获取配置信息
                TradeConfig tradeConfig = tradeConfigService.getById(1);

                BuyListVo orderDetails = null;

                req = lock.tryLock(10, TimeUnit.SECONDS);

                if (req) {

                    String sellMemberId = null;

                    //获取redis订单信息 以便事务执行失败了进行补偿性操作
                    orderDetails = redisUtil.getOrderDetails(buyReq.getPlatformOrder());
                    if (orderDetails == null) {
                        log.error("买入下单失败, 从缓存中查不到订单详情, 订单已被其他人买入或状态异常, 会员id: {}, buyReq: {}", memberId, buyReq);
                        return RestResult.failure(ResultCode.ORDER_ALREADY_USED_BY_OTHERS);
                    }

                    //检查当前会员是否处于买入冷却期
                    if (!redisUtil.canMemberBuy(String.valueOf(memberId))) {

                        //会员处于冷却期 不能购买
                        log.error("买入下单失败, 当前会员处于买入冷却期, 会员id: {}, buyReq: {}", memberId, buyReq);

                        DisableBuyingVo disableBuyingVo = new DisableBuyingVo();

                        //获取会员被禁用的时间
                        Integer memberBuyBlockRemainingTime = redisUtil.getMemberBuyBlockRemainingTime(String.valueOf(memberId));

                        if (memberBuyBlockRemainingTime == null) {
                            memberBuyBlockRemainingTime = tradeConfig.getDisabledTime();
                        }

                        //禁止买入小时数
                        disableBuyingVo.setBuyDisableHours(memberBuyBlockRemainingTime);
                        //剩余时间(秒)
                        disableBuyingVo.setRemainingSeconds(redisUtil.getMemberBuyBlockedExpireTime(String.valueOf(memberId)));
                        // 失败次数
                        disableBuyingVo.setNumberFailures(tradeConfig.getNumberFailures());

                        return RestResult.failure(ResultCode.BUY_FAILED_OVER_TIMES, disableBuyingVo);
                    }

                    //收款信息
                    CollectionInfo collectionInfo;

                    //获取当前买入会员信息
                    MemberInfo buyMemberInfo = memberInfoService.getById(memberId);

                    //卖出会员信息
                    MemberInfo sellMemberInfo = null;

                    if (buyMemberInfo == null) {
                        log.error("买入处理失败: 获取当前会员信息失败");
                        return RestResult.failure(ResultCode.RELOGIN);
                    }
                    /*// 获取配置信息
                    BigDecimal tradeCreditScoreLimit = tradeConfig.getTradeCreditScoreLimit();
                    if(buyMemberInfo.getCreditScore().compareTo(tradeCreditScoreLimit) < 0) {
                        log.error("买入处理失败: 会员信用分过低, 当前信用分:{}", buyMemberInfo.getCreditScore());
                        return RestResult.failure(ResultCode.LOW_CREDIT_SCORE);
                    }*/

                    //获取买入 ip
                    String realIP = IpUtil.getRealIP(request);

                    //获取环境信息
                    String appEnv = arProperty.getAppEnv();
                    boolean isTestEnv = "sit".equals(appEnv) || "dev".equals(appEnv);

                    if (!isTestEnv) {
                        //线上环境 校验ip是否在交易黑名单中
                        if (tradeIpBlacklistService.isIpBlacklisted(realIP)) {
                            log.error("买入处理失败, 该交易ip处于黑名单列表中, 会员id: {}, 会员账号: {}, 会员信息: {}, 交易ip: {}", memberId, buyMemberInfo.getMemberAccount(), buyMemberInfo, realIP);
                            return RestResult.failure(ResultCode.IP_BLACKLISTED);
                        }
                    }

                    //校验会员有没有实名认证
//                    if (StringUtils.isEmpty(buyMemberInfo.getRealName()) || StringUtils.isEmpty(buyMemberInfo.getIdCardNumber())) {
//                        log.error("买入下单处理失败: 该会员没有实名认证 req: {}, 会员信息: {}", buyReq, buyMemberInfo);
//                        return RestResult.failure(ResultCode.MEMBER_NOT_VERIFIED);
//                    }

                    //校验会员是否有买入权限
                    if (!MemberPermissionCheckerUtil.hasPermission(memberGroupService.getAuthListById(buyMemberInfo.getMemberGroup()), MemberPermissionEnum.BUY)) {
                        log.error("买入处理失败, 当前会员所在分组没有买入权限, 会员账号: {}", buyMemberInfo.getMemberAccount());
                        return RestResult.failure(ResultCode.NO_PERMISSION);
                    }

                    //查看当前会员是否有未完成的买入订单
                    CollectionOrder collectionOrder = collectionOrderService.countActiveBuyOrders(String.valueOf(memberId));
                    if (collectionOrder != null) {
                        log.error("买入处理失败, 当前有未完成的订单: {}, buyReq: {}, 会员账号: {}", collectionOrder, buyReq, buyMemberInfo.getMemberAccount());
                        PendingOrderVo pendingOrderVo = new PendingOrderVo();
                        pendingOrderVo.setPlatformOrder(collectionOrder.getPlatformOrder());
                        pendingOrderVo.setOrderStatus(collectionOrder.getOrderStatus());
                        // 需要返回人工审核状态,否则会影响前端页面跳转
                        if(OrderStatusEnum.isAuditing(collectionOrder.getOrderStatus(), collectionOrder.getAuditDelayTime())){
                            pendingOrderVo.setOrderStatus(OrderStatusEnum.AUDITING.getCode());
                        }
                        return RestResult.failure(ResultCode.UNFINISHED_ORDER_EXISTS, pendingOrderVo);
                    }

                    //订单校验
                    RestResult restResult = orderValidation(buyReq, buyMemberInfo, tradeConfig);
                    if (restResult != null) {
                        log.error("买入处理失败, 订单校验失败, 会员账号: {}, 错误信息: {}", buyMemberInfo.getMemberAccount(), restResult);
                        return restResult;
                    }

                    //买入订单号
                    String buyplatformOrder = orderNumberGenerator.generateOrderNo("MR");

                    //卖出订单号
                    String sellplatformOrder = orderNumberGenerator.generateOrderNo("MC");

                    //撮合列表订单号
                    String matchingPlatformOrder = orderNumberGenerator.generateOrderNo("CH");

                    //根据匹配到的卖出订单号前缀判断该笔订单是否拆单
                    if (buyReq.getPlatformOrder().startsWith("C2C")) {
                        //拆单

                        //获取匹配池订单信息 加上排他行锁
                        MatchPool matchPoolOrder = matchPoolMapper.selectMatchPoolForUpdate(buyReq.getPlatformOrder());

                        if (!isTestEnv) {
                            //线上环境 限制同IP交易
                            //查看卖出订单ip是否和买家一致, 如一致 则驳回
                            if (StringUtils.isNotEmpty(realIP) && realIP.equals(matchPoolOrder.getClientIp())) {
                                log.error("买入处理失败, 买家ip和卖出订单ip一致: 买家ip: {}, 卖出订单ip: {}, 会员id: {}, 会员账号: {}, 匹配到的订单: {}, ", realIP, matchPoolOrder.getClientIp(), buyMemberInfo.getId(), buyMemberInfo.getMemberAccount(), matchPoolOrder);
                                return RestResult.failure(ResultCode.IP_BLACKLISTED);
                            }
                        }


                        //匹配的订单不能为自己的订单
                        if (matchPoolOrder == null || matchPoolOrder.getMemberId().equals(String.valueOf(memberId))) {
                            log.error("买入处理失败, 匹配的订单不能为自己的订单: 会员账号: {}, 匹配到的订单: {}, ", buyMemberInfo.getMemberAccount(), matchPoolOrder);
                            return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                        }

                        //判断匹配订单状态是否为 待匹配  剩余金额是否小于订单金额
                        if (!OrderStatusEnum.BE_MATCHED.getCode().equals(matchPoolOrder.getOrderStatus()) || matchPoolOrder.getRemainingAmount().compareTo(buyReq.getAmount()) < 0) {

                            log.warn("买入处理失败, 选中到的订单已被其他人使用, 会员账号: {}, 订单信息: {}", buyMemberInfo.getMemberAccount(), matchPoolOrder);

                            //该笔订单已不是待匹配状态了 将它从Redis里面删除
                            redisUtil.deleteOrder(matchPoolOrder.getMatchOrder());

                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();

                            return RestResult.failure(ResultCode.ORDER_ALREADY_USED_BY_OTHERS);
                        }

                        //拆单 用最大限额 减去 购买金额 如果剩余金额少于 最小限额 那么就把这笔订单从Redis里面删除掉
                        BigDecimal subtract = orderDetails.getMaximumAmount().subtract(buyReq.getAmount());
                        if (subtract.compareTo(orderDetails.getMinimumAmount()) < 0) {
                            //剩余金额小于 最小限额了 把这笔订单从Redis列表里面删除
                            redisUtil.deleteOrder(buyReq.getPlatformOrder());
                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();
                        } else {
                            //剩余金额不小于最小限额 只是更新订单最大限额
                            //操作Redis 将订单最大限额 - 买入的金额
                            BuyListVo buyListVo = new BuyListVo();
                            BeanUtils.copyProperties(orderDetails, buyListVo);
                            //将订单最大限额(剩余金额) - 买入的金额
                            buyListVo.setMaximumAmount(buyListVo.getMaximumAmount().subtract(buyReq.getAmount()));
                            //更新Redis
                            redisUtil.updateOrderDetails(buyListVo);
                            //推送最新的 金额列表给前端
                            memberSendAmountList.send();
                        }

                        //获取卖出订单的会员信息
                        sellMemberInfo = memberInfoService.getById(matchPoolOrder.getMemberId());

                        sellMemberId = String.valueOf(sellMemberInfo.getId());

                        //获取收款信息 加上排他行锁
                        collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(matchPoolOrder.getCollectionInfoId());

                        //校验买入金额是否在最小限额和剩余金额之间
                        if (buyReq.getAmount().compareTo(matchPoolOrder.getMinimumAmount()) >= 0 && buyReq.getAmount().compareTo(matchPoolOrder.getRemainingAmount()) <= 0) {

                            //生成卖出订单----------------------------------
                            createSellOrderSplit(buyReq, sellMemberInfo, matchPoolOrder, tradeConfig, sellplatformOrder, matchingPlatformOrder, collectionInfo, matchPoolOrder.getClientIp());

                            //更新匹配池订单数据----------------------------------
                            updateMatchingPoolOrderData(buyReq, matchPoolOrder);
                        } else {
                            log.error("买入处理失败, 订单金额错误, 会员账号: {}, buyReq: {}, 匹配池信息: {}", buyMemberInfo.getMemberAccount(), buyReq, matchPoolOrder);
                            //买入失败了 将卖出订单信息添加回redis订单列表
                            addOrderIdToList(orderDetails);
                            return RestResult.failure(ResultCode.ORDER_AMOUNT_ERROR);
                        }
                    } else {
                        //1对1

                        //获取匹配到的卖出订单 加上排他行锁
                        PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(buyReq.getPlatformOrder());

                        //匹配的订单不能为自己的订单
                        if (paymentOrder == null || paymentOrder.getMemberId().equals(String.valueOf(memberId))) {
                            log.error("买入处理失败, 选择的卖出的订单不能为自己的订单, 会员账号: {}", buyMemberInfo.getMemberAccount());
                            //买入失败了 将卖出订单信息添加回redis订单列表
                            return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                        }

                        if (!isTestEnv) {
                            //查看卖出订单ip是否和买家一致, 如一致 则驳回
                            if (StringUtils.isNotEmpty(realIP) && realIP.equals(paymentOrder.getClientIp())) {
                                log.error("买入处理失败, 买家ip和卖出订单ip一致: 买家ip: {}, 卖出订单ip: {}, 会员id: {}, 会员账号: {}, 匹配到的订单: {}, ", realIP, paymentOrder.getClientIp(), buyMemberInfo.getId(), buyMemberInfo.getMemberAccount(), paymentOrder);
                                //买入失败了 将卖出订单信息添加回redis订单列表
                                return RestResult.failure(ResultCode.IP_BLACKLISTED);
                            }
                        }


                        //非拆单 直接将订单信息从redis里面删除
                        redisUtil.deleteOrder(buyReq.getPlatformOrder());
                        //推送最新的 金额列表给前端
                        memberSendAmountList.send();

                        //获取卖出订单的会员信息
                        sellMemberInfo = memberInfoService.getById(paymentOrder.getMemberId());

                        sellMemberId = String.valueOf(sellMemberInfo.getId());

                        //获取收款信息 加上排他行锁
                        collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(paymentOrder.getCollectionInfoId());

                        //判断匹配订单状态是否为 待匹配
                        if (!OrderStatusEnum.BE_MATCHED.getCode().equals(paymentOrder.getOrderStatus())) {

                            log.warn("买入处理失败, 选中到的订单已被其他人使用, 会员账号: {}, 订单信息: {}", buyMemberInfo.getMemberAccount(), paymentOrder);

                            //该笔订单已不是待匹配状态了 将它从Redis里面删除
                            redisUtil.deleteOrder(paymentOrder.getPlatformOrder());

                            //订单已经不是待匹配状态了, 所以删除了Redis 也不做补偿性操作了
                            return RestResult.failure(ResultCode.ORDER_ALREADY_USED_BY_OTHERS);
                        }

                        //校验买入金额 是否和 卖出金额相等
                        if (buyReq.getAmount().compareTo(paymentOrder.getAmount()) != 0) {
                            //买入失败了 将卖出订单信息添加回redis订单列表
                            addOrderIdToList(orderDetails);
                            return RestResult.failure(ResultCode.ORDER_AMOUNT_ERROR);
                        }

                        //更新卖出订单数据----------------------------------
                        updateSellOrder(buyReq, paymentOrder, matchingPlatformOrder);

                        //1对1  卖出订单号是前端传过来的订单号
                        sellplatformOrder = buyReq.getPlatformOrder();
                    }

                    //生成买入订单----------------------------------
                    createBuyOrder(buyReq, buyMemberInfo, buyplatformOrder, matchingPlatformOrder, collectionInfo, realIP);

                    //生成撮合列表订单----------------------------------
                    createMatchedOrder(buyReq, buyplatformOrder, sellplatformOrder, matchingPlatformOrder, collectionInfo, buyMemberInfo, sellMemberInfo);

                    //更新会员买入统计信息: 累计买入次数
                    memberInfoService.updateAddBuyInfo(String.valueOf(buyMemberInfo.getId()));

                    //更新收款信息
                    //增加今日收款金额
                    collectionInfo.setTodayCollectedAmount(collectionInfo.getTodayCollectedAmount().add(buyReq.getAmount()));
                    //今日收款笔数+1
                    collectionInfo.setTodayCollectedCount(collectionInfo.getTodayCollectedCount() + 1);
                    //更新收款信息
                    collectionInfoService.updateById(collectionInfo);

                    //从配置表获取 支付超时时间(分钟)
                    long millis = TimeUnit.MINUTES.toMillis(tradeConfig.getRechargeExpirationTime());
                    Long lastUpdateTimestamp = System.currentTimeMillis();
                    //发送支付超时的MQ
                    TaskInfo taskInfo = new TaskInfo(buyplatformOrder, TaskTypeEnum.PAYMENT_TIMEOUT.getCode(), lastUpdateTimestamp);
                    rabbitMQService.sendTimeoutTask(taskInfo, millis);


//                QueueInfo collectQueueInfo = new QueueInfo(RabbitMqConstants.AR_WALLET_DELAYED_ORDER_TIMEOUT_QUEUE_NAME, buyplatformOrder, OrderTimeOutEnum.PAYMENT_TIMEOUT.getCode(), null);
//                rabbitMQUtil.sendDelayedMessage(buyplatformOrder, Integer.parseInt(String.valueOf(millis)), new CorrelationData(JSON.toJSONString(collectQueueInfo)));

                    //将支付倒计时记录到redis 买入订单
                    redisUtil.setPaymentExpireTime(buyplatformOrder, tradeConfig.getRechargeExpirationTime());
                    //将支付倒计时记录到redis 卖出订单
                    redisUtil.setPaymentExpireTime(sellplatformOrder, tradeConfig.getRechargeExpirationTime());
                    //将支付倒计时记录到redis 撮合列表订单
                    redisUtil.setPaymentExpireTime(matchingPlatformOrder, tradeConfig.getRechargeExpirationTime());

                    log.info("买入处理成功, 会员账号: {}, 买入订单号: {}, 卖出订单号: {}, 撮合列表订单号: {}, 支付过期时间(分钟): {}", buyMemberInfo.getMemberAccount(), buyplatformOrder, sellplatformOrder, matchingPlatformOrder, tradeConfig.getRechargeExpirationTime());


                    //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                    final String finalSellplatformOrder = sellplatformOrder;
                    final String finalSellMemberId = sellMemberId;

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            //发送匹配成功的通知给前端
                            NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage
                                    = new NotifyOrderStatusChangeMessage(finalSellMemberId, NotificationTypeEnum.NOTIFY_SELLER.getCode(), finalSellplatformOrder);

                            notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);

                            // 发送计算会员等级消息
                            rabbitMQService.sendMemberUpgradeMessage(String.valueOf(memberId));
                        }
                    });
                    return RestResult.ok();
                }
                log.error("买入下单失败, 未获取到买入会员ID锁, 会员id: {}, buyReq: {}", memberId, buyReq);
                //买入失败了 将卖出订单信息添加回redis订单列表
                addOrderIdToList(orderDetails);
                return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
            } else {
                //没获取到锁 直接失败
                log.error("买入下单失败, 未获取到买入订单锁, 会员id: {}, buyReq: {}", memberId, buyReq);
                return RestResult.failure(ResultCode.ORDER_ALREADY_USED_BY_OTHERS);
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("买入处理失败, req: {} e: ", buyReq, e);
        } finally {
            //释放锁
            if (req1 && lock1.isHeldByCurrentThread()) {
                lock1.unlock();
            }
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }


    /**
     * 买入失败 将卖出订单信息添加回redis订单列表
     *
     * @param orderDetails
     */
    public void addOrderIdToList(BuyListVo orderDetails) {
        if (orderDetails != null) {
            log.error("买入处理失败 将卖出订单信息添加回redis订单列表, 订单信息: {}", orderDetails);
            redisUtil.addOrderIdToList(orderDetails, "2");

            //推送最新的 金额列表给前端
            memberSendAmountList.send();
        }
    }

    /**
     * 买入订单校验
     *
     * @param buyReq
     * @param buyMemberInfo
     * @param tradeConfig
     * @return {@link RestResult}
     */
    @Override
    public RestResult orderValidation(BuyReq buyReq, MemberInfo buyMemberInfo, TradeConfig tradeConfig) {

        //校验金额是否是整百
//        if (!AmountVerifyUtil.isMultipleOfHundred(buyReq.getAmount())) {
//            log.error("买入订单校验失败 金额不为整百 会员信息: {}, req: {}", buyMemberInfo, buyReq);
//            return RestResult.failure(ResultCode.ORDER_AMOUNT_MUST_BE_INTEGER);
//        }

        //查看是否开启实名认证交易限制
        if (controlSwitchService.isSwitchEnabled(SwitchIdEnum.REAL_NAME_VERIFICATION.getSwitchId())) {
            if (MemberAuthenticationStatusEnum.UNAUTHENTICATED.getCode().equals(buyMemberInfo.getAuthenticationStatus())) {
                log.error("买入订单校验失败 当前会员未实名认证 会员信息: {}, req: {}", buyMemberInfo, buyReq);
                return RestResult.failure(ResultCode.MEMBER_NOT_VERIFIED);
            }
        }

        //判断当前会员状态和买入状态是否可用
        if (MemberStatusEnum.DISABLE.getCode().equals(buyMemberInfo.getStatus())) {
            log.error("买入订单校验失败 当前会员状态不可用 会员信息: {}, req: {}", buyMemberInfo, buyReq);
            return RestResult.failure(ResultCode.MEMBER_STATUS_NOT_AVAILABLE);
        }

        if (BuyStatusEnum.DISABLE.getCode().equals(buyMemberInfo.getBuyStatus())) {
            log.error("买入订单校验失败 当前会员状态和买入状态不可用 会员信息: {}, req: {}", buyMemberInfo, buyReq);
            return RestResult.failure(ResultCode.MEMBER_BUY_STATUS_NOT_AVAILABLE);
        }

        //根据会员标签获取对应配置信息
        TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(buyMemberInfo);

        //判断买入金额 是否在 最小买入金额 和最大买入金额之间
        OrderAmountValidationResult orderAmountValid = TradeValidationUtil.isOrderAmountValid(buyReq.getAmount(), schemeConfigByMemberTag.getSchemeMinPurchaseAmount(), schemeConfigByMemberTag.getSchemeMaxPurchaseAmount());

        if (orderAmountValid == OrderAmountValidationResult.TOO_LOW){
            //订单金额太低
            log.error("买入订单校验失败 买入金额低于最小买入金额 会员信息: {}, req: {}, 最小买入金额: {}", buyMemberInfo, buyReq, schemeConfigByMemberTag.getSchemeMinPurchaseAmount());
            return RestResult.failure(ResultCode.ORDER_AMOUNT_TOO_LOW);
        }

        if (orderAmountValid == OrderAmountValidationResult.TOO_HIGH){
            //订单金额超过最大限制
            log.error("买入订单校验失败 买入金额超过最大买入金额 会员信息: {}, req: {}, 最大买入金额: {}", buyMemberInfo, buyReq, schemeConfigByMemberTag.getSchemeMaxPurchaseAmount());
            return RestResult.failure(ResultCode.ORDER_AMOUNT_EXCEEDS_LIMIT);
        }

        return null;
    }

    /**
     * 生成买入订单
     *
     * @param buyReq
     * @param buyMemberInfo
     * @param buyplatformOrder
     * @param matchingPlatformOrder
     * @return {@link Boolean}
     */
    @Override
    public Boolean createBuyOrder(BuyReq buyReq, MemberInfo buyMemberInfo, String buyplatformOrder, String matchingPlatformOrder, CollectionInfo collectionInfo, String realIP) {

        //生成买入订单----------------------------------

        CollectionOrder collectionOrder = new CollectionOrder();


        //判断如果是商户会员 那么加上商户名称
        if (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(buyMemberInfo.getMemberType())) {
            MerchantInfo merchantInfoByCode = merchantInfoService.getMerchantInfoByCode(buyMemberInfo.getMerchantCode());
            if (merchantInfoByCode != null) {
                collectionOrder.setMerchantCode(merchantInfoByCode.getCode());
                collectionOrder.setMerchantName(merchantInfoByCode.getUsername());
            }
        }

        //设置平台订单号
        collectionOrder.setPlatformOrder(buyplatformOrder);

        //设置会员id
        collectionOrder.setMemberId(String.valueOf(buyMemberInfo.getId()));

        //设置会员账号
        collectionOrder.setMemberAccount(buyMemberInfo.getMemberAccount());

        //设置订单金额
        collectionOrder.setAmount(buyReq.getAmount());

        //设置订单实际金额 默认就是订单金额
        collectionOrder.setActualAmount(collectionOrder.getAmount());

        //设置撮合列表订单号
        collectionOrder.setMatchingPlatformOrder(matchingPlatformOrder);

        //设置UPI_Id
        collectionOrder.setUpiId(collectionInfo.getUpiId());

        //设置UPI_Name
        collectionOrder.setUpiName(collectionInfo.getUpiName());

        //设置会员手机号
        collectionOrder.setMobileNumber(buyMemberInfo.getMobileNumber());

        //设置交易ip
        collectionOrder.setClientIp(realIP);

        // 支付随机码
        collectionOrder.setRandomCode(RandomStringUtils.randomNumeric(6));

        //设置奖励 如果会员单独配置了买入奖励 那么才加上买入奖励 默认是没有奖励的
        if (buyMemberInfo.getBuyBonusProportion() != null && buyMemberInfo.getBuyBonusProportion().compareTo(new BigDecimal(0)) > 0) {
            collectionOrder.setBonus(buyReq.getAmount().multiply((buyMemberInfo.getBuyBonusProportion().divide(BigDecimal.valueOf(100)))));
            log.info("添加会员买入奖励: 会员信息{}, 买入奖励金额: {}", buyMemberInfo, collectionOrder.getBonus());
        } else {
            //判断如果是商户会员 并且专门配置了买入奖励 那么才有买入奖励
            if (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(buyMemberInfo.getMemberType())) {

                //商户会员
                //获取商户信息
                MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(buyMemberInfo.getMerchantCode());

                if (merchantInfo != null) {
                    //判断该商户是否配置了买入奖励
                    if (merchantInfo.getRechargeReward() != null && merchantInfo.getRechargeReward().compareTo(new BigDecimal(0)) > 0) {
                        collectionOrder.setBonus(buyReq.getAmount().multiply((merchantInfo.getRechargeReward().divide(BigDecimal.valueOf(100)))));
                        log.info("添加会员买入奖励, 商户单独配置了买入奖励, 商户信息: {}, 会员信息{}, 买入奖励金额: {}", merchantInfo, buyMemberInfo, collectionOrder.getBonus());
                    }
                }
            }
        }


        boolean save = collectionOrderService.save(collectionOrder);

        log.info("买入处理: 生成买入订单, 买入会员信息: {}, req: {}, 买入订单信息: {}, sql执行结果: {}", buyMemberInfo, buyReq, collectionOrder, save);

        return save;
    }

    /**
     * 生成卖出订单-拆单
     *
     * @param buyReq
     * @param sellMemberInfo
     * @param matchPoolOrder
     * @param tradeConfig
     * @param sellplatformOrder
     * @param matchingPlatformOrder
     * @param collectionInfo
     * @return {@link Boolean}
     */
    @Override
    public Boolean createSellOrderSplit(BuyReq buyReq, MemberInfo sellMemberInfo, MatchPool matchPoolOrder, TradeConfig tradeConfig, String sellplatformOrder, String matchingPlatformOrder, CollectionInfo collectionInfo, String matchPoolOrderClientIp) {

        //生成卖出订单-拆单----------------------------------

        PaymentOrder paymentOrder = new PaymentOrder();

        //判断如果是商户会员 那么加上商户名称
        if (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(sellMemberInfo.getMemberType())) {
            MerchantInfo merchantInfoByCode = merchantInfoService.getMerchantInfoByCode(sellMemberInfo.getMerchantCode());
            if (merchantInfoByCode != null) {
                paymentOrder.setMerchantCode(merchantInfoByCode.getCode());
                paymentOrder.setMerchantName(merchantInfoByCode.getUsername());
            }
        }

        //会员id
        paymentOrder.setMemberId(String.valueOf(sellMemberInfo.getId()));

        //会员账号
        paymentOrder.setMemberAccount(sellMemberInfo.getMemberAccount());

        //匹配订单号
        paymentOrder.setMatchOrder(buyReq.getPlatformOrder());

        //平台订单号
        paymentOrder.setPlatformOrder(sellplatformOrder);

        //订单金额
        paymentOrder.setAmount(buyReq.getAmount());

        //实际金额 默认就是订单金额
        paymentOrder.setActualAmount(buyReq.getAmount());

        //撮合列表订单号
        paymentOrder.setMatchingPlatformOrder(matchingPlatformOrder);

        //设置会员手机号
        paymentOrder.setMobileNumber(sellMemberInfo.getMobileNumber());

        //设置交易ip (母订单的交易ip)
        paymentOrder.setClientIp(matchPoolOrderClientIp);

        //根据会员标签获取对应配置
        TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(sellMemberInfo);

        //生成奖励
        //查看会员如果有单独配置卖出奖励 那么就读取单独配置的卖出奖励
        if (sellMemberInfo.getSellBonusProportion() != null && sellMemberInfo.getSellBonusProportion().compareTo(new BigDecimal(0)) > 0) {
            paymentOrder.setBonus(buyReq.getAmount().multiply((new BigDecimal(sellMemberInfo.getSellBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
        } else {
            //判断该会员是钱包会员还是商户会员
            if (MemberTypeEnum.WALLET_MEMBER.getCode().equals(sellMemberInfo.getMemberType())) {
                //钱包会员
                //会员没有单独配置卖出奖励, 获取配置表奖励比例 并计算出改笔订单奖励值
                if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                    paymentOrder.setBonus(buyReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                }
            } else {
                //判断该商户是否单独配置了奖励比例 如果是的话 就直接取该商户单独配置的奖励

                //获取商户信息
                MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(sellMemberInfo.getMerchantCode());

                if (merchantInfo != null) {
                    //该商户单独配置的卖出奖励不为null并且大于0
                    if (merchantInfo.getWithdrawalRewards() != null && merchantInfo.getWithdrawalRewards().compareTo(new BigDecimal(0)) > 0) {
                        paymentOrder.setBonus(buyReq.getAmount().multiply((new BigDecimal(merchantInfo.getWithdrawalRewards().toString()).divide(BigDecimal.valueOf(100)))));
                    } else {
                        //商户会员 该商户没有单独配置卖出奖励 那么读取默认奖励
                        if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                            paymentOrder.setBonus(buyReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                        }
                    }
                } else {
                    //商户会员
                    //就算商户不存在 也要按默认配置取计算奖励
                    //会员没有单独配置卖出奖励, 获取配置表奖励比例 并计算出改笔订单奖励值
                    if (schemeConfigByMemberTag.getSchemeSalesBonusProportion() != null && schemeConfigByMemberTag.getSchemeSalesBonusProportion().compareTo(new BigDecimal(0)) > 0) {
                        paymentOrder.setBonus(buyReq.getAmount().multiply((new BigDecimal(schemeConfigByMemberTag.getSchemeSalesBonusProportion().toString()).divide(BigDecimal.valueOf(100)))));
                    }
                }
            }
        }

        //匹配时长 单位: 秒 匹配池订单创建时间 当前时间
        paymentOrder.setMatchDuration(DurationCalculatorUtil.secondsBetween(matchPoolOrder.getCreateTime(), LocalDateTime.now()));

        //匹配时间
        paymentOrder.setMatchTime(LocalDateTime.now());

        //订单状态 -待支付
        paymentOrder.setOrderStatus(OrderStatusEnum.BE_PAID.getCode());

        //生成UPI_Id
        paymentOrder.setUpiId(collectionInfo.getUpiId());

        //生成UPI_Name
        paymentOrder.setUpiName(collectionInfo.getUpiName());

        //收款信息id
        paymentOrder.setCollectionInfoId(collectionInfo.getId());

        boolean save = paymentOrderService.save(paymentOrder);

        log.info("买入处理: 生成卖出订单, 卖出会员信息: {}, req: {}, 卖出订单信息: {}, sql执行结果: {}", sellMemberInfo, buyReq, paymentOrder, save);

        return save;
    }

    /**
     * 生成撮合列表订单
     *
     * @param buyReq
     * @param buyplatformOrder
     * @param sellplatformOrder
     * @param matchingPlatformOrder
     * @param collectionInfo
     * @param buyMemberInfo
     * @param sellMemberInfo
     * @return {@link Boolean}
     */
    @Override
    public Boolean createMatchedOrder(BuyReq buyReq, String buyplatformOrder, String sellplatformOrder, String matchingPlatformOrder, CollectionInfo collectionInfo, MemberInfo buyMemberInfo, MemberInfo sellMemberInfo) {

        //生成撮合列表订单----------------------------------
        MatchingOrder matchingOrder = new MatchingOrder();

        //买入订单号
        matchingOrder.setCollectionPlatformOrder(buyplatformOrder);

        //卖出订单号
        matchingOrder.setPaymentPlatformOrder(sellplatformOrder);

        //订单金额
        matchingOrder.setOrderSubmitAmount(buyReq.getAmount());

        //实际金额
        matchingOrder.setOrderActualAmount(matchingOrder.getOrderSubmitAmount());

        //订单状态
        matchingOrder.setStatus(OrderStatusEnum.BE_PAID.getCode());

        //撮合列表订单号
        matchingOrder.setPlatformOrder(matchingPlatformOrder);

        //设置UPI_ID
        matchingOrder.setUpiId(collectionInfo.getUpiId());

        //设置UPI_Name
        matchingOrder.setUpiName(collectionInfo.getUpiName());

        //充值会员id
        matchingOrder.setCollectionMemberId(String.valueOf(buyMemberInfo.getId()));

        //充值会员账号
        matchingOrder.setCollectionMemberAccount(buyMemberInfo.getMemberAccount());

        //提现会员id
        matchingOrder.setPaymentMemberId(String.valueOf(sellMemberInfo.getId()));

        //提现会员账号
        matchingOrder.setPaymentMemberAccount(sellMemberInfo.getMemberAccount());

        boolean save = matchingOrderService.save(matchingOrder);

        log.info("买入处理: 生成撮合列表订单, 买入订单号: {}, 卖出订单号: {}, req: {}, 生成撮合列表订单: {}, sql执行结果: {}", buyplatformOrder, sellplatformOrder, buyReq, matchingOrder, save);

        return save;
    }

    /**
     * 更新匹配池订单数据
     *
     * @param buyReq
     * @param matchPoolOrder
     * @return {@link Boolean}
     */
    @Override
    public Boolean updateMatchingPoolOrderData(BuyReq buyReq, MatchPool matchPoolOrder) {

        //拆单成功 更新匹配池订单数据----------------------------------

        //已匹配订单数
        matchPoolOrder.setOrderMatchCount(matchPoolOrder.getOrderMatchCount() + 1);

        //进行中订单数
        matchPoolOrder.setInProgressOrderCount(matchPoolOrder.getInProgressOrderCount() + 1);

        //已卖出金额
        matchPoolOrder.setSoldAmount(matchPoolOrder.getSoldAmount().add(buyReq.getAmount()));

        //剩余金额
        matchPoolOrder.setRemainingAmount(matchPoolOrder.getRemainingAmount().subtract(buyReq.getAmount()));

        //最大限额
        matchPoolOrder.setMaximumAmount(matchPoolOrder.getRemainingAmount());

        //判断该笔订单剩余金额是否为0 如果剩余金额为0 就将该笔订单状态改为 进行中
        if (matchPoolOrder.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            //剩余金额等于0 将该笔匹配池订单改为 进行中
            matchPoolOrder.setOrderStatus(OrderStatusEnum.IN_PROGRESS.getCode());
        }

        boolean b = matchPoolService.updateById(matchPoolOrder);

        log.info("买入处理 更新匹配池订单数据: req: {}, 匹配池订单数据: {}, sql执行结果: {}", buyReq, matchPoolOrder, b);

        return b;
    }

    /**
     * 更新卖出订单数据
     *
     * @param buyReq
     * @param paymentOrder
     * @param matchingPlatformOrder
     * @return {@link Boolean}
     */
    @Override
    public Boolean updateSellOrder(BuyReq buyReq, PaymentOrder paymentOrder, String matchingPlatformOrder) {

        //更新卖出订单状态
        paymentOrder.setOrderStatus(OrderStatusEnum.BE_PAID.getCode());
        //更新卖出订单匹配时长 卖出订单创建时间 - 当前时间
        paymentOrder.setMatchDuration(DurationCalculatorUtil.secondsBetween(paymentOrder.getCreateTime(), LocalDateTime.now()));
        //更新卖出订单匹配时间
        paymentOrder.setMatchTime(LocalDateTime.now());
        //更新撮合列表订单号
        paymentOrder.setMatchingPlatformOrder(matchingPlatformOrder);

        boolean b = paymentOrderService.updateById(paymentOrder);

        log.info("买入处理 更新卖出订单数据 req: {}, 卖出订单:{}, 撮合列表订单号: {}, sql执行结果: {}", buyReq, paymentOrder, matchingPlatformOrder, b);

        return b;
    }

    /**
     * USDT买入处理
     *
     * @param usdtBuyReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult usdtBuyProcessor(UsdtBuyReq usdtBuyReq) {

        //获取当前会员id
        Long memberId = UserContext.getCurrentUserId();

        if (memberId == null) {
            log.error("USDT买入处理失败 会员id为null");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-buy+会员id
        String key = "ar-wallet-usdt-buy" + memberId;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取当前买入会员信息 加上排他行锁
                MemberInfo usdtBuyMemberInfo = memberInfoService.getById(memberId);

                if (usdtBuyMemberInfo == null) {
                    log.error("USDT买入处理失败 会员信息为null 会员id: {}", memberId);
                    return RestResult.failure(ResultCode.RELOGIN);
                }

                //校验会员有没有实名认证
//                if (StringUtils.isEmpty(usdtBuyMemberInfo.getRealName()) || StringUtils.isEmpty(usdtBuyMemberInfo.getIdCardNumber())) {
//                    log.error("USDT买入下单处理失败: 该会员没有实名认证 req: {}, 会员信息: {}", usdtBuyReq, usdtBuyMemberInfo);
//                    return RestResult.failure(ResultCode.MEMBER_NOT_VERIFIED);
//                }

                //校验当前会员买入状态是否为开启
                if (!usdtBuyMemberInfo.getBuyStatus().equals(BuyStatusEnum.ENABLE.getCode())) {
                    log.error("USDT买入失败, 会员买入状态未开启 会员账号: {}, usdtBuyReq: {}", usdtBuyMemberInfo.getMemberAccount(), usdtBuyReq);
                    return RestResult.failure(ResultCode.MEMBER_BUY_STATUS_NOT_ENABLED);
                }

                //查看当前会员是否有未完成的USDT订单
                UsdtBuyOrder usdtBuyOrder = usdtBuyOrderService.countActiveUsdtBuyOrders(String.valueOf(usdtBuyMemberInfo.getId()));

                if (usdtBuyOrder != null) {
                    log.error("USDT买入失败 当前有未完成的订单 会员账号: {}, usdtBuyReq: {}, 当前未完成的USDT订单: {}", usdtBuyMemberInfo.getMemberAccount(), usdtBuyReq, usdtBuyOrder);

                    PendingOrderVo pendingOrderVo = new PendingOrderVo();
                    pendingOrderVo.setPlatformOrder(usdtBuyOrder.getPlatformOrder());
                    pendingOrderVo.setOrderStatus(usdtBuyOrder.getStatus());

                    return RestResult.failure(ResultCode.UNFINISHED_ORDER_EXISTS, pendingOrderVo);
                }

                //检查当前会员是否处于买入冷却期
                if (!redisUtil.canMemberBuy(String.valueOf(usdtBuyMemberInfo.getId()))) {
                    //会员处于冷却期 不能购买

                    //获取配置信息
                    TradeConfig tradeConfig = tradeConfigService.getById(1);

                    //获取会员冷却期剩余时间
                    long memberBuyBlockedExpireTime = redisUtil.getMemberBuyBlockedExpireTime(String.valueOf(usdtBuyMemberInfo.getId()));

                    log.error("USDT买入下单失败, 当前会员处于买入冷却期, 会员账号: {}, buyReq: {}, 冷却期剩余时间 (秒): {}", usdtBuyMemberInfo.getMemberAccount(), usdtBuyReq, memberBuyBlockedExpireTime);

                    DisableBuyingVo disableBuyingVo = new DisableBuyingVo();

                    //获取会员被禁用的时间
                    Integer memberBuyBlockRemainingTime = redisUtil.getMemberBuyBlockRemainingTime(String.valueOf(memberId));

                    if (memberBuyBlockRemainingTime == null) {
                        memberBuyBlockRemainingTime = tradeConfig.getDisabledTime();
                    }

                    //禁止买入小时数
                    disableBuyingVo.setBuyDisableHours(memberBuyBlockRemainingTime);
                    //剩余时间(秒)
                    disableBuyingVo.setRemainingSeconds(redisUtil.getMemberBuyBlockedExpireTime(String.valueOf(memberId)));

                    return RestResult.failure(ResultCode.BUY_COOLDOWN_PERIOD, disableBuyingVo);
                }

                //获取配置信息
                TradeConfig tradeConfig = tradeConfigMapper.selectById(1);

                //根据前端传过来的USDT金额 计算出对应的ARB金额 USDT金额 * 汇率 保留两位小数 后面的小数四舍五入
                BigDecimal calculatedArbAmount = usdtBuyReq.getUsdtAmount().multiply(tradeConfig.getUsdtCurrency()).setScale(2, RoundingMode.DOWN);
                usdtBuyReq.setArbAmount(calculatedArbAmount);

                //校验
                RestResult restResult = usdtOrderValidation(usdtBuyReq, usdtBuyMemberInfo, tradeConfig);
                if (restResult != null) {
                    log.error("USDT买入处理失败 会员账号: {}, 汇率错误: {}", usdtBuyMemberInfo.getMemberAccount(), restResult);
                    return restResult;
                }

                //匹配USDT收款信息
                UsdtConfig usdtInfo = usdtConfigService.matchUsdtReceiptInfo(usdtBuyReq.getNetworkProtocol());

                if (usdtInfo == null) {
                    log.error("USDT买入处理 匹配失败: {}, 会员账号: {}", usdtBuyReq, usdtBuyMemberInfo.getMemberAccount());
                    return RestResult.failure(ResultCode.MATCHING_FAILED);
                }

                //生成USDT买入订单
                String platformOrder = orderNumberGenerator.generateOrderNo("USDT");
                createUsdtOrder(usdtBuyReq, usdtBuyMemberInfo, usdtInfo, platformOrder);

                log.info("USDT买入 会员账号: {}, 订单号: {}, USDT买入金额: {}, ARB买入金额: {}, 汇率: {}", usdtBuyMemberInfo.getMemberAccount(), platformOrder, usdtBuyReq.getUsdtAmount(), usdtBuyReq.getArbAmount(), tradeConfig.getUsdtCurrency());


                //更新会员买入统计信息: 累计买入次数
                memberInfoService.updateAddBuyInfo(String.valueOf(usdtBuyMemberInfo.getId()));

                //从配置表获取 支付超时时间(分钟)
                long millis = TimeUnit.MINUTES.toMillis(tradeConfig.getRechargeExpirationTime());
                //发送USDT支付超时的MQ
                Long lastUpdateTimestamp = System.currentTimeMillis();
                TaskInfo taskInfo = new TaskInfo(platformOrder, TaskTypeEnum.USDT_PAYMENT_TIMEOUT.getCode(), lastUpdateTimestamp);
                rabbitMQService.sendTimeoutTask(taskInfo, millis);

//                QueueInfo collectQueueInfo = new QueueInfo(RabbitMqConstants.AR_WALLET_DELAYED_ORDER_TIMEOUT_QUEUE_NAME, platformOrder, OrderTimeOutEnum.USDT_PAYMENT_TIMEOUT.getCode(), null);
//                rabbitMQUtil.sendDelayedMessage(platformOrder, Integer.parseInt(String.valueOf(millis)), new CorrelationData(JSON.toJSONString(collectQueueInfo)));

                //将USDT支付倒计时记录到redis USDT买入订单
                redisUtil.setUsdtPaymentExpireTime(platformOrder, tradeConfig.getRechargeExpirationTime());

                log.info("USDT买入处理 成功 会员账号: {}, req: {}, 支付超时时间(分钟): {}", usdtBuyMemberInfo.getMemberAccount(), usdtBuyReq, tradeConfig.getRechargeExpirationTime());
                return RestResult.ok();
            }
        } catch (Exception e) {
            log.error("USDT买入处理 失败 会员id: {}, req: {}, e: {}", memberId, usdtBuyReq, e);
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * USDT买入订单校验
     *
     * @param usdtBuyReq
     * @param usdtBuyMemberInfo
     * @param tradeConfig
     * @return {@link RestResult}
     */
    @Override
    public RestResult usdtOrderValidation(UsdtBuyReq usdtBuyReq, MemberInfo usdtBuyMemberInfo, TradeConfig tradeConfig) {

        //校验汇率是否正确
        // 计算usdtAmount乘以汇率后的值，并保留两位小数，直接去掉多余的小数位
//        BigDecimal calculatedArbAmount = usdtBuyReq.getUsdtAmount()
//                .multiply(tradeConfig.getUsdtCurrency())
//                .setScale(2, RoundingMode.DOWN);
//
//        if (usdtBuyReq.getArbAmount().compareTo(calculatedArbAmount) != 0) {
//            log.error("USDT买入失败 汇率错误 req: {}, 会员信息: {}, 配置信息: {}", usdtBuyReq, usdtBuyMemberInfo, tradeConfig);
//            return RestResult.failure(ResultCode.USDT_RATE_ERROR);
//        }

        //校验USDT数量是否低于100
        if (usdtBuyReq.getUsdtAmount().compareTo(new BigDecimal(100)) < 0) {
            log.error("USDT买入失败 USDT数量是否低于100 req: {}, 会员信息: {}, 配置信息: {}", usdtBuyReq, usdtBuyMemberInfo, tradeConfig);
            return RestResult.failure(ResultCode.USDT_AMOUNT_TOO_LOW);
        }

        //判断当前会员状态和买入状态是否可用
        if (MemberStatusEnum.DISABLE.getCode().equals(usdtBuyMemberInfo.getStatus()) || BuyStatusEnum.DISABLE.getCode().equals(usdtBuyMemberInfo.getBuyStatus())) {
            log.error("USDT买入失败 当前会员状态和买入状态不可用 req: {}, 会员信息: {}, 配置信息: {}", usdtBuyReq, usdtBuyMemberInfo, tradeConfig);
            return RestResult.failure(ResultCode.MEMBER_STATUS_NOT_AVAILABLE);
        }

        //校验买入金额是否超过钱包用户最大买入金额
//        if (usdtBuyReq.getArbAmount().compareTo(tradeConfig.getMemberMaxPurchaseAmount()) > 0) {
//            log.error("USDT买入失败 买入金额超过钱包用户最大买入金额 req: {}, 会员信息: {}, 配置信息: {}", usdtBuyReq, usdtBuyMemberInfo, tradeConfig);
//            return RestResult.failure(ResultCode.ORDER_AMOUNT_EXCEEDS_LIMIT);
//        }

        return null;
    }

    /**
     * 生成USDT买入订单
     *
     * @param usdtBuyReq
     * @param usdtBuyMemberInfo
     * @param usdtInfo
     * @param platformOrder
     * @return {@link Boolean}
     */
    @Override
    public Boolean createUsdtOrder(UsdtBuyReq usdtBuyReq, MemberInfo usdtBuyMemberInfo, UsdtConfig usdtInfo, String platformOrder) {

        //生成USDT买入订单
        UsdtBuyOrder usdtBuyOrder = new UsdtBuyOrder();


        //判断如果是商户会员 那么加上商户名称
        if (!MemberTypeEnum.WALLET_MEMBER.getCode().equals(usdtBuyMemberInfo.getMemberType())) {
            MerchantInfo merchantInfoByCode = merchantInfoService.getMerchantInfoByCode(usdtBuyMemberInfo.getMerchantCode());
            if (merchantInfoByCode != null) {
                usdtBuyOrder.setMerchantCode(merchantInfoByCode.getCode());
                usdtBuyOrder.setMerchantName(merchantInfoByCode.getUsername());
            }
        }

        //设置会员ID
        usdtBuyOrder.setMemberId(String.valueOf(usdtBuyMemberInfo.getId()));

        //设置会员账号
        usdtBuyOrder.setMemberAccount(usdtBuyMemberInfo.getMemberAccount());

        //设置订单号
        usdtBuyOrder.setPlatformOrder(platformOrder);

        //设置收款地址
        usdtBuyOrder.setUsdtAddr(usdtInfo.getUsdtAddr());

        //设置USDT数量
        usdtBuyOrder.setUsdtNum(usdtBuyReq.getUsdtAmount());

        //设置ARB数量
        usdtBuyOrder.setArbNum(usdtBuyReq.getArbAmount());

        boolean save = usdtBuyOrderService.save(usdtBuyOrder);

        log.info("USDT买入处理 生成USDT买入订单 会员信息: {}, req: {}, USDT买入订单: {}, sql执行结果: {}", usdtBuyMemberInfo, usdtBuyReq, usdtBuyOrder, save);

        return save;
    }


    /**
     * 更新卖出订单 支付时间
     *
     * @param paymentOrder
     * @param fileName
     * @param utr
     * @return {@link Boolean}
     */
    private Boolean updatePaymentOrderPaymentTime(PaymentOrder paymentOrder, String fileName, String utr) {

        //更新订单状态为确认中
        paymentOrder.setOrderStatus(OrderStatusEnum.CONFIRMATION.getCode());

        //设置支付时间
        paymentOrder.setPaymentTime(LocalDateTime.now());

        //设置支付凭证
        paymentOrder.setVoucher(fileName);

        //设置UTR
        paymentOrder.setUtr(utr);

        //更新卖出订单
        boolean b = paymentOrderService.updateById(paymentOrder);

        log.info("完成支付处理 更新卖出订单信息: {}, sql执行结果: {}", paymentOrder, b);

        return b;
    }


    /**
     * 更新撮合列表支付时间
     *
     * @param matchingOrder
     * @param fileName
     * @param utr
     * @return {@link Boolean}
     */
    private Boolean updateMatchingOrderPaymentTime(MatchingOrder matchingOrder, String fileName, String utr) {

        //更新订单状态为: 确认中
        matchingOrder.setStatus(OrderStatusEnum.CONFIRMATION.getCode());

        //设置订单支付时间
        matchingOrder.setPaymentTime(LocalDateTime.now());

        //设置支付凭证
        matchingOrder.setVoucher(fileName);

        //设置UTR
        matchingOrder.setUtr(utr);

        //更新撮合列表订单
        boolean b = matchingOrderService.updateById(matchingOrder);

        log.info("完成支付处理 更新撮合列表订单信息: {}, sql执行结果: {}", matchingOrder, b);

        return b;
    }


    /**
     * 更新买入订单为确认中
     *
     * @param collectionOrder
     * @param fileName
     * @param utr
     * @return {@link Boolean}
     */
    private Boolean updatecollectionOrderToConfirmation(CollectionOrder collectionOrder, String fileName, String utr) {

        //更新支付凭证
        collectionOrder.setVoucher(fileName);

        //设置订单状态为: 确认中
        collectionOrder.setOrderStatus(OrderStatusEnum.CONFIRMATION.getCode());

        //更新支付时间
        collectionOrder.setPaymentTime(LocalDateTime.now());

        //更新UTR
        collectionOrder.setUtr(utr);

        //更新买入订单
        boolean b = collectionOrderService.updateById(collectionOrder);

        log.info("完成支付处理 更新买入订单信息: {}, sql执行结果: {}", collectionOrder, b);

        return b;
    }


    /**
     * 取消买入订单处理
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult cancelPurchaseOrder(CancelOrderReq cancelOrderReq) {
        return cancelPurchaseOrder(cancelOrderReq, OrderStatusEnum.WAS_CANCELED, OrderStatusEnum.WAS_CANCELED, OrderStatusEnum.WAS_CANCELED, null);
    }

    /**
     * 取消买入订单处理
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult cancelPurchaseOrder(CancelOrderReq cancelOrderReq,
                                          OrderStatusEnum buyStatus,
                                          OrderStatusEnum sellStatus,
                                          OrderStatusEnum matchStatus,
                                          MemberInfo memberInfoParam) {

        //对取消原因表单进行HTML清洗
        cancelOrderReq.setReason(JsoupUtil.clean(cancelOrderReq.getReason()));

        //获取当前会员信息

        MemberInfo memberInfo;
        if (ObjectUtils.isNotEmpty(memberInfoParam)) {
            memberInfo = memberInfoParam;
        } else {
            memberInfo = memberInfoService.getMemberInfo();
        }
        if (memberInfo == null) {
            log.error("取消买入订单处理失败, 该会员不存在: {}", memberInfo);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //分布式锁key ar-wallet-cancelPurchaseOrder+订单号
        String key = "ar-wallet-cancelPurchaseOrder" + cancelOrderReq.getPlatformOrder();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                boolean isSplitOrder = false;

                //查询买入订单 加上排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(cancelOrderReq.getPlatformOrder());

                String memberId = String.valueOf(memberInfo.getId());

                //校验该笔订单是否属于当前会员
                if (collectionOrder == null || !collectionOrder.getMemberId().equals(memberId)) {
                    log.error("取消买入订单处理失败 该笔订单不存在或该笔订单不属于该会员, 会员账号: {}, 订单信息: {}", memberInfo.getMemberAccount(), collectionOrder);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //校验订单如果是已取消状态 那么直接返回成功(防止重复点击)
                if (collectionOrder.getOrderStatus().equals(buyStatus.getCode())) {
                    log.error("取消买入订单失败, 当前订单状态为已取消: {}, cancelOrderReq: {}, 会员账号: {}", collectionOrder.getOrderStatus(), cancelOrderReq, memberInfo.getMemberAccount());
                    return RestResult.failure(ResultCode.ORDER_STATUS_VERIFICATION_FAILED);
                }

                //校验当前订单状态
                //4  确认中
                //5  确认超时
                //6  申诉中
                //11 金额错误
                //13  支付超时
                //只有这几种状态才能进行取消买入订单
                if (!(OrderStatusEnum.CONFIRMATION.getCode().equals(collectionOrder.getOrderStatus()) || OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(collectionOrder.getOrderStatus()) || OrderStatusEnum.COMPLAINT.getCode().equals(collectionOrder.getOrderStatus()) || OrderStatusEnum.PAYMENT_TIMEOUT.getCode().equals(collectionOrder.getOrderStatus())
                        || OrderStatusEnum.AMOUNT_ERROR.getCode().equals(collectionOrder.getOrderStatus()) || (cancelOrderReq.getSourceType() == 2 && OrderStatusEnum.BE_PAID.getCode().equals(collectionOrder.getOrderStatus())))) {
                    log.error("取消买入订单失败, 订单状态必须为: 4确认中  5确认超时  6申诉中 13支付超时 才能够进行取消 当前订单状态为: {}, cancelOrderReq: {}, 会员账号: {}", collectionOrder.getOrderStatus(), cancelOrderReq, memberInfo.getMemberAccount());
                    return RestResult.failure(ResultCode.ORDER_STATUS_VERIFICATION_FAILED);
                }
                // 人工审核状态,客户端不能取消
                if (cancelOrderReq.getSourceType() == 1 && OrderStatusEnum.isAuditing(collectionOrder.getOrderStatus(), collectionOrder.getAuditDelayTime())) {
                    log.error("取消买入订单失败, 当前订单状态为人工审核, cancelOrderReq: {}, 会员账号: {}", cancelOrderReq, memberInfo.getMemberAccount());
                    return RestResult.failure(ResultCode.ORDER_STATUS_VERIFICATION_FAILED);
                }

                //查看是否有申诉订单, 如果有的话 要把申诉订单关掉

                //如果买入订单是申诉中状态, 那么要把申诉订单进行关闭 (改为1: 未支付)
                if (OrderStatusEnum.COMPLAINT.getCode().equals(collectionOrder.getOrderStatus())) {

                    //查询申诉订单 加上排他行锁
                    AppealOrder appealOrder = appealOrderMapper.selectAppealOrderByRechargeOrderNoForUpdate(collectionOrder.getPlatformOrder());

                    // 1是待处理
                    if (appealOrder != null) {
                        String appealOrderStatus = String.valueOf(appealOrder.getAppealStatus());

                        if ("1".equals(appealOrderStatus) || "4".equals(appealOrderStatus)) {
                            //将申诉订单改为 已支付
                            UpdateWrapper<AppealOrder> appealOrderUpdateWrapper = new UpdateWrapper<>();

                            appealOrderUpdateWrapper.eq("recharge_order_no", collectionOrder.getPlatformOrder()); // 使用卖出订单号作为更新条件
                            appealOrderUpdateWrapper.set("appeal_status", "3"); // 设置申诉订单的状态为未支付

                            // 执行更新
                            appealOrderMapper.update(null, appealOrderUpdateWrapper);
                        }
                    }
                }
                //设置取消状态
                collectionOrder.setCancelType(CollectionOrderCancelTypeEnum.CancelOrder.getCode());
                //更新买入订单状态为: 已取消 并填写取消原因
                collectionOrderToWasCanceled(collectionOrder, cancelOrderReq, buyStatus);

                //查询撮合列表 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(collectionOrder.getMatchingPlatformOrder());
                //更新撮合列表状态为已取消 并填写取消原因
                matchingOrderToWasCanceled(collectionOrder, matchingOrder, cancelOrderReq, matchStatus);

                //查询卖出订单 加上排他行锁
                PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());

                //获取卖出会员信息 加上排他行锁
                MemberInfo sellMemberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(paymentOrder.getMemberId()));

                String sellMemberId = String.valueOf(sellMemberInfo.getId());


                //分布式锁key ar-wallet-sell+会员id
                String key2 = "ar-wallet-sell" + sellMemberId;
                RLock lock2 = redissonUtil.getLock(key2);

                boolean req2 = false;

                try {
                    req2 = lock2.tryLock(10, TimeUnit.SECONDS);

                    if (req2) {

                        //查看卖出订单是否拆单
                        if (StringUtils.isNotEmpty(paymentOrder.getMatchOrder())) {
                            //拆单


                            //获取匹配池订单 加上排他行锁
                            MatchPool matchPool = matchPoolMapper.selectMatchPoolForUpdate(paymentOrder.getMatchOrder());

                            //更新匹配池订单信息
                            //进行中订单 -1
                            matchPool.setInProgressOrderCount(matchPool.getInProgressOrderCount() - 1);

                            //已完成订单 +1
                            matchPool.setCompletedOrderCount(matchPool.getCompletedOrderCount() + 1);

                            //更新匹配池订单
                            matchPoolService.updateById(matchPool);

                            // 将订单金额退回至用户余额

                            //账变前余额
                            BigDecimal previousBalance = sellMemberInfo.getBalance();

                            //将订单金额退回到会员余额里面
                            sellMemberInfo.setBalance(sellMemberInfo.getBalance().add(paymentOrder.getAmount()));

                            //将用户冻结金额 减去 该笔订单金额
                            sellMemberInfo.setFrozenAmount(sellMemberInfo.getFrozenAmount().subtract(paymentOrder.getAmount()));

                            //账变后余额
                            BigDecimal newBalance = sellMemberInfo.getBalance();

                            //记录会员账变信息
                            memberAccountChangeService.recordMemberTransaction(sellMemberId, paymentOrder.getAmount(), MemberAccountChangeEnum.CANCEL_RETURN.getCode(), paymentOrder.getPlatformOrder(), previousBalance, newBalance, "");

                            //更新会员信息
                            boolean b = memberInfoService.updateById(sellMemberInfo);

                            log.info("取消买入订单处理成功 买入订单号: {}, 卖出订单号(拆单): {}, 会员信息: {}, 账变前余额: {}, 订单金额: {}, 账变后余额: {}, sql执行结果: {}",
                                    cancelOrderReq.getPlatformOrder(), paymentOrder.getPlatformOrder(), memberInfo, previousBalance, paymentOrder.getAmount(), newBalance, b);

                            isSplitOrder = true;
                        } else {

                            //账变前余额
                            BigDecimal previousBalance = sellMemberInfo.getBalance();

                            //将订单金额退回到会员余额里面
                            sellMemberInfo.setBalance(sellMemberInfo.getBalance().add(paymentOrder.getAmount()));

                            //将用户冻结金额 减去 该笔订单金额
                            sellMemberInfo.setFrozenAmount(sellMemberInfo.getFrozenAmount().subtract(paymentOrder.getAmount()));

                            //账变后余额
                            BigDecimal newBalance = sellMemberInfo.getBalance();

                            //记录会员账变信息
                            memberAccountChangeService.recordMemberTransaction(sellMemberId, paymentOrder.getAmount(), MemberAccountChangeEnum.CANCEL_RETURN.getCode(), paymentOrder.getPlatformOrder(), previousBalance, newBalance, "");

                            //将进行中的卖出订单数-1
//                    sellMemberInfo.setActiveSellOrderCount(sellMemberInfo.getActiveSellOrderCount() - 1);

                            //更新会员信息
                            boolean b = memberInfoService.updateById(sellMemberInfo);

                            log.info("取消买入订单处理成功 买入订单号: {}, 卖出订单号(非拆单): {}, 买入会员信息: {}, 卖出会员信息: {}, 账变前余额: {}, 订单金额: {}, 账变后余额: {}, sql执行结果: {}",
                                    cancelOrderReq.getPlatformOrder(), paymentOrder.getPlatformOrder(), memberInfo, sellMemberInfo, previousBalance, paymentOrder.getAmount(), newBalance, b);
                        }

                        //将卖出订单状态改为: 已取消
                        paymentOrder.setOrderStatus(sellStatus.getCode());

                        //更新卖出订单 取消原因
                        paymentOrder.setCancellationReason(cancelOrderReq.getReason());

                        //取消人 (前台取消买入订单 那买入方就是取消人)
                        paymentOrder.setCancelBy(collectionOrder.getMemberAccount());

                        //取消时间
                        paymentOrder.setCancelTime(LocalDateTime.now());

                        //更新卖出订单
                        paymentOrderService.updateById(paymentOrder);

                        //获取收款信息 加上排他行锁
                        CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(paymentOrder.getCollectionInfoId());
                        //更新收款信息
                        updateCollectionInfo(collectionInfo, paymentOrder);

                        //记录会员买入失败次数
                        redisUtil.recordMemberBuyFailure(String.valueOf(memberInfo.getId()));

                        log.info("取消买入订单处理成功 :{}, 买入会员信息: {}, 卖出会员信息: {}, 买入订单号: {}, 卖出订单号: {}", cancelOrderReq, memberInfo, sellMemberInfo, collectionOrder.getPlatformOrder(), paymentOrder.getPlatformOrder());

                        //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                        final boolean finalIsSplitOrder = isSplitOrder;
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {

                                //非拆单 尝试将收款次数-1
                                if (!finalIsSplitOrder) {
                                    String cancelSellorderNo = StringUtils.isNotEmpty(paymentOrder.getMatchOrder()) ? paymentOrder.getMatchOrder() : paymentOrder.getPlatformOrder();

                                    //符合条件的话 将该收款信息 单日收款次数 -1
                                    upiTransactionService.decrementDailyTransactionCountIfApplicable(paymentOrder.getUpiId(), cancelSellorderNo);
                                }

                                //买方取消订单了, WebSocket通知卖方
                                //发送交易成功的通知给前端
                                NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage
                                        = new NotifyOrderStatusChangeMessage(paymentOrder.getMemberId(), NotificationTypeEnum.NOTIFY_SELLER.getCode(), paymentOrder.getPlatformOrder());

                                notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);

                                if (finalIsSplitOrder && paymentOrder != null && StringUtils.isNotEmpty(paymentOrder.getMatchOrder())) {
                                    //匹配池订单有子订单 查询全部子订单 并根据子订单状态更新匹配池订单状态
                                    sellService.updateMatchPoolOrderStatus(paymentOrder.getMatchOrder());
                                }
                            }
                        });

                        return RestResult.ok();

                    }
                } catch (Exception e) {
                    //手动回滚
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    log.error("取消买入订单处理失败 :{}, 会员信息: {}, e: {}", cancelOrderReq, memberInfo, e);
                } finally {
                    //释放锁
                    if (req && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }

                    if (req2 && lock2.isHeldByCurrentThread()) {
                        lock2.unlock();
                    }
                }
            }
        } catch (Exception e) {
            log.error("取消买入订单处理失败 :{}, 会员信息: {}, e: {}", cancelOrderReq, memberInfo, e);
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }


    /**
     * 取消买入订单 更新买入订单状态为: 已取消 添加取消原因
     *
     * @param collectionOrder
     * @param cancelOrderReq
     * @return {@link Boolean}
     */
    public Boolean collectionOrderToWasCanceled(CollectionOrder collectionOrder, CancelOrderReq cancelOrderReq, OrderStatusEnum orderStatusEnum) {
        //更新订单状态为: 已取消
        collectionOrder.setOrderStatus(orderStatusEnum.getCode());
        // 添加取消原因
        collectionOrder.setCancellationReason(cancelOrderReq.getReason());

        //取消人 (前台取消买入订单 那买入方就是取消人)
        collectionOrder.setCancelBy(collectionOrder.getMemberAccount());

        //取消时间
        collectionOrder.setCancelTime(LocalDateTime.now());

        boolean b = collectionOrderService.updateById(collectionOrder);

        log.info("取消买入订单处理 更新买入订单信息: {}, req: {}, sql执行结果: {}", collectionOrder, cancelOrderReq, b);

        return b;
    }

    /**
     * 取消支付 更新买入订单状态为: 取消支付 添加取消原因
     *
     * @param collectionOrder
     * @param cancelOrderReq
     * @return {@link Boolean}
     */
    public Boolean collectionOrderToCancelPayment(CollectionOrder collectionOrder, CancelOrderReq cancelOrderReq) {
        //更新订单状态为: 已取消
        collectionOrder.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());
        // 添加取消原因
        collectionOrder.setCancellationReason(cancelOrderReq.getReason());
        //添加取消支付标识
        collectionOrder.setIsPaymentCancelled("1");

        //取消时间
        collectionOrder.setCancelTime(LocalDateTime.now());

        //取消人 (前台取消买入订单 那买入方就是取消人)
        collectionOrder.setCancelBy(collectionOrder.getMemberAccount());


        boolean b = collectionOrderService.updateById(collectionOrder);

        log.info("取消支付处理 更新买入订单信息: {}, sql执行结果: {}", collectionOrder, b);

        return b;
    }

    /**
     * 取消买入订单 更新撮合列表状态为: 取消支付
     *
     * @param collectionOrder
     * @param matchingOrder
     * @param cancelOrderReq
     * @param orderStatusEnum
     * @return {@link Boolean}
     */
    public Boolean matchingOrderToWasCanceled(CollectionOrder collectionOrder, MatchingOrder matchingOrder, CancelOrderReq cancelOrderReq, OrderStatusEnum orderStatusEnum) {
        //更新撮合列表订单状态为: 已取消
        matchingOrder.setStatus(orderStatusEnum.getCode());

        //填写取消原因
        matchingOrder.setCancellationReason(cancelOrderReq.getReason());

        //取消人 (前台取消买入订单 那买入方就是取消人)
        matchingOrder.setCancelBy(collectionOrder.getMemberAccount());

        //取消时间
        matchingOrder.setCancelTime(LocalDateTime.now());

        boolean b = matchingOrderService.updateById(matchingOrder);

        log.info("取消买入订单 更新撮合列表订单: {}, sql执行结果: {}", matchingOrder, b);

        return b;
    }

    /**
     * 取消买入订单 更新匹配池
     *
     * @param matchPool
     * @return {@link Boolean}
     */
    public Boolean matchPoolToWasCanceled(MatchPool matchPool, PaymentOrder paymentOrder, CancelOrderReq cancelOrderReq) {

        //更新前匹配池订单剩余金额
        BigDecimal matchPoolRemainingAmount = matchPool.getRemainingAmount();

        //将该笔订单金额添加到匹配池订单剩余金额
        matchPool.setRemainingAmount(matchPool.getRemainingAmount().add(paymentOrder.getActualAmount()));

        //设置最大金额
        matchPool.setMaximumAmount(matchPool.getRemainingAmount());

        //取消原因
        matchPool.setCancellationReason(cancelOrderReq.getReason());

        //减去已卖出金额
        matchPool.setSoldAmount(matchPool.getSoldAmount().subtract(paymentOrder.getActualAmount()));

        //进行中订单 -1
        matchPool.setInProgressOrderCount(matchPool.getInProgressOrderCount() - 1);

        //已完成订单 +1
        matchPool.setCompletedOrderCount(matchPool.getCompletedOrderCount() + 1);

        //更新匹配池订单
        boolean b = matchPoolService.updateById(matchPool);

        log.info("取消支付 更新匹配池订单信息: {}, sql执行结果: {}, 匹配池订单处于匹配中状态: 将订单金额添加到匹配池订单剩余金额, 更新前匹配池订单剩余金额: {}, 更新后匹配池订单剩余金额", matchPool, b, matchPoolRemainingAmount, matchPool.getRemainingAmount());

        return b;
    }

    /**
     * 取消买入订单 更新收款信息
     *
     * @param collectionInfo
     * @return {@link Boolean}
     */
    public Boolean updateCollectionInfo(CollectionInfo collectionInfo, PaymentOrder paymentOrder) {

        //减去今日收款金额
        collectionInfo.setTodayCollectedAmount(collectionInfo.getTodayCollectedAmount().subtract(paymentOrder.getAmount()));

        //今日收款笔数-1
        collectionInfo.setTodayCollectedCount(collectionInfo.getTodayCollectedCount() - 1);

        //更新收款信息
        return collectionInfoService.updateById(collectionInfo);
    }


//    /**
//     * 提交UTR处理
//     *
//     * @param submitUtrReq
//     * @return {@link RestResult}
//     */
//    @Override
//    @Transactional
//    public RestResult submitUtr(SubmitUtrReq submitUtrReq) {
//
//        //获取当前会员信息
//        MemberInfo memberInfo = memberInfoService.getMemberInfo();
//        if (memberInfo == null) {
//            log.error("提交UTR处理失败, 会员不存在: {}", memberInfo);
//            return RestResult.failure(ResultCode.RELOGIN);
//        }
//
//        try {
//
//            //获取买入订单 加上排他行锁
//            CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(submitUtrReq.getPlatformOrder());
//
//            if (collectionOrder == null) {
//                log.error("提交UTR处理失败: 订单号错误: {}, 会员信息: {}", submitUtrReq, memberInfo);
//                return RestResult.failure(ResultCode.ORDER_NUMBER_ERROR);
//            }
//
//            //更新买入订单号的UTR
//            collectionOrder.setUtr(submitUtrReq.getUtr());
//            collectionOrderService.updateById(collectionOrder);
//
//
//            //获取撮合列表订单 加上排他行锁
//            MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(collectionOrder.getMatchingPlatformOrder());
//
//            //更新撮合列表的UTR
//            matchingOrder.setUtr(submitUtrReq.getUtr());
//            matchingOrderService.updateById(matchingOrder);
//
//
//            //获取卖出订单 加上排他行锁
//            PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());
//
//            //更新卖出订单的UTR
//            paymentOrder.setUtr(submitUtrReq.getUtr());
//            paymentOrderService.updateById(paymentOrder);
//
//            log.info("提交UTR处理成功: {}, 会员账号: {}", submitUtrReq, memberInfo.getMemberAccount());
//            return RestResult.ok();
//        } catch (Exception e) {
//            log.error("提交UTR处理失败: {}, 会员账号: {}, e: {}", submitUtrReq, memberInfo.getMemberAccount(), e);
//            //手动回滚
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
//        }
//    }

    /**
     * 完成支付 处理
     *
     * @param platformOrder
     * @param voucherImage
     * @param utr
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult buyCompletedProcessor(String platformOrder, String voucherImage, String utr) {

        //分布式锁key ar-wallet-buyCompleted+订单号
        String key = "ar-wallet-buyCompleted" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {

            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                if (!FileUtil.isValidImageExtension(voucherImage)) {
                    // 如果有文件不符合规茨，则返回错误
                    log.error("完成支付处理失败: 会员上传图片文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, voucherImage);
                    return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
                }

                MemberInfo memberInfo = memberInfoService.getMemberInfo();

                if (memberInfo == null) {
                    log.error("完成支付处理失败: 获取会员信息失败");
                    return RestResult.failure(ResultCode.RELOGIN);
                }

                //文件校验
//                RestResult validateFile = FileUtil.validateFile(voucherImage, arProperty.getMaxImageFileSize(), "image");
//                if (validateFile != null) {
//                    log.error("完成支付 处理失败 订单号: {}, 文件校验失败: {}, 会员账号: {}", platformOrder, validateFile.getMsg(), memberInfo.getMemberAccount());
//                    return validateFile;
//                }

                //获取买入订单 加上排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(platformOrder);

                //判断当前订单如果是支付超时状态 那么直接返回订单支付超时
                if (OrderStatusEnum.PAYMENT_TIMEOUT.getCode().equals(collectionOrder.getOrderStatus())) {
                    log.error("完成支付处理失败: 当前订单为支付超时 会员账号: {}, 订单号: {}", memberInfo.getMemberAccount(), platformOrder);
                    return RestResult.failure(ResultCode.ORDER_EXPIRED);
                }

                //判断当前订单如果是确认中状态 那么直接返回成功
                if (OrderStatusEnum.CONFIRMATION.getCode().equals(collectionOrder.getOrderStatus())) {
                    log.error("完成支付处理失败: 当前订单为确认中状态 会员账号: {}, 订单号: {}", memberInfo.getMemberAccount(), platformOrder);
                    return RestResult.ok();
                }

                String memberId = String.valueOf(memberInfo.getId());

                //校验该笔订单是否属于该会员
                if (collectionOrder == null || !collectionOrder.getMemberId().equals(memberId)) {
                    log.error("完成支付处理失败: 非法操作 该笔订单不存在或该笔订单不属于该会员 会员信息: {}, 订单号: {}", memberInfo, platformOrder);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //判断当前订单如果不是待支付状态 则不做处理
                if (!OrderStatusEnum.BE_PAID.getCode().equals(collectionOrder.getOrderStatus())) {
                    log.error("完成支付处理失败: 当前订单不是待支付状态 会员账号: {}, 订单号: {}, 当前订单状态: {}, 订单信息: {}", memberInfo.getMemberAccount(), platformOrder, collectionOrder.getOrderStatus(), collectionOrder);
                    return RestResult.failure(ResultCode.ORDER_EXPIRED);
                }

                //调用阿里云存储服务 将图片上传上去 并获取到文件名
//                String fileName = ossService.uploadFile(voucherImage);
//                if (fileName == null) {
//                    log.error("完成支付 上传文件至阿里云失败 会员账号: {}, 订单号: {}", memberInfo.getMemberAccount(), platformOrder);
//                    return RestResult.failure(ResultCode.FILE_UPLOAD_FAILED);
//                }

                //查看是否开启 支付凭证识别
                if (controlSwitchService.isSwitchEnabled(SwitchIdEnum.PAYMENT_VOUCHER_RECOGNITION.getSwitchId())) {
                    if (redisUtil.getOsrOrderPayFailCount(platformOrder) >= 3) {
                        log.error("完成支付处理, 订单号: {}, 支付凭证识别错误超过3次", platformOrder);
                        return RestResult.failure(ResultCode.PAY_OSR_FAIL_OVER_TIMES);
                    }
                    TestImageRecognitionVo recognitionResult = imageRecognitionService.isPaymentVoucher(baseUrl + voucherImage);
                    log.info("完成支付处理, 订单号: {}, 支付凭证识别结果: {}", platformOrder, recognitionResult);
                    if (recognitionResult == null || !"Ads".equals(recognitionResult.getRiskLabel1())) {

                        //支付凭证识别失败后, 将图片路径和订单号存储到redis
                        storeImagePath(platformOrder, baseUrl + voucherImage);

                        Long failCount = redisUtil.countOsrOrderPayFail(platformOrder);
                        if (failCount == 3) {
                            // 取消订单
                            CancelOrderReq cancelOrderReq = new CancelOrderReq();
                            cancelOrderReq.setPlatformOrder(platformOrder);
                            cancelOrderReq.setReason("支付凭证识别连续三次失败关闭订单");
                            log.info("完成支付处理, 订单号: {}, 支付凭证识别连续三次失败关闭订单", platformOrder);
                            cancelPurchaseOrder(cancelOrderReq);
                            return RestResult.failure(ResultCode.PAY_OSR_FAIL_OVER_TIMES);
                        }
                        return RestResult.failure(ResultCode.NOT_VOUCHER_IMAGE);
                    }
                } else {
                    log.info("完成支付处理 支付凭证识别未开启");
                }

                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(collectionOrder.getMatchingPlatformOrder());

                //获取卖出订单 加上排他行锁
                PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());

                //获取卖出会员信息
                MemberInfo sellMemberInfo = memberInfoService.getById(paymentOrder.getMemberId());

                voucherImage = baseUrl + voucherImage;

                //更新买入订单 订单状态: 确认中 支付时间 支付凭证 UTR
                updatecollectionOrderToConfirmation(collectionOrder, voucherImage, utr);

                //更新撮合列表订单 订单状态: 确认中 支付时间 支付凭证 UTR
                updateMatchingOrderPaymentTime(matchingOrder, voucherImage, utr);

                //更新卖出订单 订单状态: 确认中 支付时间 支付凭证 UTR
                updatePaymentOrderPaymentTime(paymentOrder, voucherImage, utr);


                //根据会员标签获取对应配置信息 确认到账超时时间 根据卖家会员标签
                TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(sellMemberInfo);

                //从配置表获取 钱包用户确认超时时间 并转换为毫秒
                long millis = TimeUnit.MINUTES.toMillis(schemeConfigByMemberTag.getSchemeConfirmExpirationTime());

                //发送确认超时MQ(撮合列表订单号)
                Long lastUpdateTimestamp = System.currentTimeMillis();
                TaskInfo taskInfo = new TaskInfo(matchingOrder.getPlatformOrder(), TaskTypeEnum.WALLET_MEMBER_CONFIRMATION_TIMEOUT.getCode(), lastUpdateTimestamp);
                rabbitMQService.sendTimeoutTask(taskInfo, millis);

                // 确认超时时长
                Integer durationMinutes = schemeConfigByMemberTag.getSchemeConfirmExpirationTime();

                //将确认倒计时记录到redis 买入订单
                redisUtil.setConfirmExpireTime(collectionOrder.getPlatformOrder(), durationMinutes);
                //将确认倒计时记录到redis 卖出订单
                redisUtil.setConfirmExpireTime(paymentOrder.getPlatformOrder(), durationMinutes);
                //将确认倒计时记录到redis 撮合列表订单
                redisUtil.setConfirmExpireTime(matchingOrder.getPlatformOrder(), durationMinutes);

                log.info("完成支付处理成功 会员账号: {}, 买入订单号: {}", memberInfo.getMemberAccount(), platformOrder);

                //注册事务同步回调(事务提交成功后 同步回调执行的操作)
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        //买方完成支付 WebSocket通知卖方
                        NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage
                                = new NotifyOrderStatusChangeMessage(paymentOrder.getMemberId(), NotificationTypeEnum.NOTIFY_SELLER.getCode(), paymentOrder.getPlatformOrder());

                        notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);

                        //获取该笔订单的收款upi  然后发送短信通知upi绑定的号码
                        CollectionInfo paymentDetailsByUpiIdAndUpiName = collectionInfoService.getPaymentDetailsByUpiIdAndUpiName(paymentOrder.getUpiId(), paymentOrder.getUpiName());

                        log.info("完成支付处理成功 会员账号: {}, 买入订单号: {}, 执行事务同步回调: 该笔订单的收款UPI: {}", memberInfo.getMemberAccount(), platformOrder, paymentDetailsByUpiIdAndUpiName);

                        if (paymentDetailsByUpiIdAndUpiName != null) {

                            //如果不是91开头的手机号 自动补上91前缀
                            String telephone = StringUtil.startsWith91(paymentDetailsByUpiIdAndUpiName.getMobileNumber()) ? paymentDetailsByUpiIdAndUpiName.getMobileNumber() : "91" + paymentDetailsByUpiIdAndUpiName.getMobileNumber();

                            JSONObject jsonObject = new JSONObject();

                            //发送短信通知卖方
                            MessageStatus messageStatus = messageClient.sendMessage(telephone, arProperty.getConfirmationTimeoutTemplateId(), jsonObject);

                            if (messageStatus != null && messageStatus.getStatus() == true) {
                                log.info("完成支付, 发送短信通知卖方成功, 订单号: {}, 手机号: {}, 被通知会员信息: {}", paymentOrder, telephone, sellMemberInfo);
                            } else {
                                log.error("完成支付, 发送短信通知卖方失败, 订单号: {}, 手机号: {}, 被通知会员信息: {}", paymentOrder, telephone, sellMemberInfo);
                            }
                        }

                        String appEnv = arProperty.getAppEnv();
                        boolean isTestEnv = "sit".equals(appEnv) || "dev".equals(appEnv);

                        if (isTestEnv) {
                            //测试环境

                            //发送自动获取 KYC交易记录的MQ  如果UPI收到了款项, 那么就自动完成订单
                            KycTransactionMessage kycTransactionMessage = new KycTransactionMessage();

                            //买方会员ID
                            kycTransactionMessage.setBuyerMemberId(memberInfo.getId());

                            //买方会员账号
                            kycTransactionMessage.setBuyerMemberAccount(memberInfo.getMemberAccount());

                            //卖方会员ID
                            kycTransactionMessage.setSellerMemberId(sellMemberInfo.getId());

                            //卖方会员账号
                            kycTransactionMessage.setSellerMemberAccount(sellMemberInfo.getMemberAccount());

                            //付款人UPI
//                        kycTransactionMessage.setPayerUPI();

                            //收款人UPI
                            kycTransactionMessage.setRecipientUPI(paymentOrder.getUpiId());

                            //订单金额
                            kycTransactionMessage.setAmount(paymentOrder.getAmount());

                            //交易 UTR
                            kycTransactionMessage.setTransactionUTR(utr);

                            //交易时间
                            kycTransactionMessage.setTransactionTime(LocalDateTime.now());

                            //买入订单号
                            kycTransactionMessage.setBuyerOrderId(collectionOrder.getPlatformOrder());

                            //卖出订单号
                            kycTransactionMessage.setSellerOrderId(paymentOrder.getPlatformOrder());

                            rabbitMQService.sendKycTransactionMessage(kycTransactionMessage);
                        }

                    }
                });

                return RestResult.ok();

            }
        } catch (DataIntegrityViolationException e) {
            //UTR重复
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.info("完成支付处理失败, UTR校验失败, 买入订单号: {}, e: {}", platformOrder, e);
            return RestResult.failure(ResultCode.UTR_VALIDATION_FAILED);
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.info("完成支付处理失败, 买入订单号: {}, e: {}", platformOrder, e);
            return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 生成存储 识别失败图片的 redis Key
     *
     * @return {@link String}
     */
    public String generateKey() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "failedPaymentVoucherPaths:" + date;
    }


    public void storeImagePath(String platformOrder, String imagePath) {
        String key = generateKey();
        redisTemplate.opsForHash().put(key, imagePath, platformOrder);
    }

    /**
     * 买入订单申诉处理
     *
     * @param platformOrder
     * @param appealReason
     * @param images
     * @param video
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult buyOrderAppealProcess(String platformOrder, String appealReason, List<String> images, String video) {


        if (images == null) {
            log.error("买入订单申诉处理失败: 会员没有上传图片文件 直接驳回, 订单号: {}, 文件名: {}", platformOrder, images);
            return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
        }

        // 检查images列表中的每个文件名是否符合规茨
        for (String image : images) {
            if (!FileUtil.isValidImageExtension(image)) {
                // 如果有文件不符合规茨，则返回错误
                log.error("买入订单申诉处理失败: 会员上传图片文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, images);
                return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
            }
        }

        if (StringUtils.isNotEmpty(video)) {
            //视频文件名不为空才进行校验视频文件名
            if (!FileUtil.isValidVideoExtension(video)) {
                // 如果有文件不符合规茨，则返回错误
                log.error("买入订单申诉处理失败: 会员上传视频文件不符合规范 直接驳回, 订单号: {}, 文件名: {}", platformOrder, video);
                return RestResult.failure(ResultCode.FILE_UPLOAD_REQUIRED);
            }
        }

        //分布式锁key ar-wallet-buyOrderAppealProcess+订单号
        String key = "ar-wallet-buyOrderAppealProcess" + platformOrder;
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //对申诉原因表单进行HTML清洗
                appealReason = JsoupUtil.clean(appealReason);

                //获取当前会员id
                Long memberId = UserContext.getCurrentUserId();
                AssertUtil.notEmpty(memberId, ResultCode.RELOGIN);

                //获取当前会员信息
                MemberInfo complainant = memberInfoService.getById(memberId);

                //获取买入订单 加上排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(platformOrder);

                //校验该订单是否属于当前会员
                if (collectionOrder == null || !collectionOrder.getMemberId().equals(String.valueOf(memberId))) {
                    log.error("买入订单申诉 提交失败 订单不存在或订单不属于该会员 订单号: {}, 订单信息: {}, 会员信息: {}", platformOrder, collectionOrder, complainant);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }

                //判断当前订单状态如果是申诉中 直接返回成功
                if (collectionOrder.getOrderStatus().equals(OrderStatusEnum.COMPLAINT.getCode())) {
                    return RestResult.ok();
                }

                //判断当前订单状态 是否为: 确认超时  只有确认超时的订单才能进行买入申诉
                if (!OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(collectionOrder.getOrderStatus())) {
                    log.error("买入订单申诉 提交失败 订单校验失败: 超时订单才能进行申诉: 会员账号:{} 当前订单状态: {}, 订单号: {}, 订单信息: {}", complainant.getMemberAccount(), collectionOrder.getOrderStatus(), platformOrder, collectionOrder);
                    return RestResult.failure(ResultCode.ORDER_STATUS_VERIFICATION_FAILED);
                }

                if (OrderStatusEnum.isAuditing(collectionOrder.getOrderStatus(), collectionOrder.getAuditDelayTime())) {
                    log.error("买入订单申诉 提交失败 人工审核状态订单不能进行申诉, 会员账号:{}, 订单信息: {}", complainant.getMemberAccount(), collectionOrder);
                    return RestResult.failure(ResultCode.ORDER_STATUS_VERIFICATION_FAILED);
                }

                //文件处理
//                JsonObject saveFile = appealOrderService.saveFile(images, video);
//                if (saveFile.getString("errMsg") != null) {
//                    log.error("买入申诉 文件校验失败: {}, msg: {}", platformOrder, saveFile);
//                    return RestResult.failure(ResultCode.FILE_VERIFICATION_FAILED);
//                }

//                String buyOrderAppealImage = saveFile.getString("appealImage");
//
//                String buyOrderAppealVideo = saveFile.getString("appealVideo");


                String buyOrderAppealImage;
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

                    buyOrderAppealImage = sb.toString();
                } else {
                    // 处理null情况，例如赋予一个默认值或者执行其他逻辑
                    buyOrderAppealImage = ""; // 或者根据需要进行处理
                }

                String buyOrderAppealVideo = (StringUtils.isNotEmpty(video)) ? baseUrl + video : null;

                //更新买入订单为申诉中
                updateCollectionOrderToAppealInProgress(collectionOrder);

                //获取撮合列表订单 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(collectionOrder.getMatchingPlatformOrder());
                //更新撮合列表订单为申诉中
                updateMatchingOrderToAppealInProgress(matchingOrder);

                //获取卖出订单 加上排他行锁
                PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());
                //更新卖出订单为申诉中
                updatePaymentOrderToAppealInProgress(paymentOrder);

                //更新被申诉人信息 (被申诉次数)
                memberInfoService.updateAddAppealCount(String.valueOf(paymentOrder.getId()));

                //生成申诉订单
                createAppealOrder(complainant, collectionOrder, paymentOrder, buyOrderAppealImage, buyOrderAppealVideo, appealReason);

                log.info("买入订单申诉, 提交成功: {}, 会员账号: {}", platformOrder, complainant.getMemberAccount());


                //注册事务同步回调
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        //买方提交买入申诉了, WebSocket通知卖方
                        //发送交易成功的通知给前端
                        NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage
                                = new NotifyOrderStatusChangeMessage(paymentOrder.getMemberId(), NotificationTypeEnum.NOTIFY_SELLER.getCode(), paymentOrder.getPlatformOrder());

                        notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);
                    }
                });

                return RestResult.ok();
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("卖出下单接口处理失败 订单号: {}, e: {}", platformOrder, e);
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
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    @Override
    @Transactional
    public RestResult cancelPayment(CancelOrderReq cancelOrderReq) {


        //对取消原因表单进行HTML清洗
        cancelOrderReq.setReason(JsoupUtil.clean(cancelOrderReq.getReason()));

        //分布式锁key ar-wallet-cancelPayment+订单号
        String key = "ar-wallet-cancelPayment" + cancelOrderReq.getPlatformOrder();
        RLock lock = redissonUtil.getLock(key);

        boolean req = false;

        //是否需要注册事务同步回调
        boolean shouldRegisterTransactionCallback = false;
        MemberInfo sellMemberInfo = null;
        Boolean isSplitOrder = false;
        BigDecimal rAmout = null;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);

            if (req) {

                //获取当前会员信息
                MemberInfo memberInfo = memberInfoService.getMemberInfo();

                if (memberInfo == null) {
                    log.error("取消支付处理失败: 获取会员信息失败");
                    return RestResult.failure(ResultCode.RELOGIN);
                }

                //查询买入订单 加上排他行锁
                CollectionOrder collectionOrder = collectionOrderMapper.selectCollectionOrderForUpdate(cancelOrderReq.getPlatformOrder());

                String memberId = String.valueOf(memberInfo.getId());

                //校验该笔订单是否属于当前会员
                if (collectionOrder == null || !collectionOrder.getMemberId().equals(memberId)) {
                    log.error("取消支付处理失败 订单不存在或该订单不属于该会员 会员信息: {} 订单信息: {}", memberInfo, collectionOrder);
                    return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
                }


                //校验订单如果是已取消状态 那么直接返回成功(防止重复点击)
                if (collectionOrder.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
                    log.error("取消支付订单失败, 当前订单状态为已取消: {}, cancelOrderReq: {}, 会员账号: {}", collectionOrder.getOrderStatus(), cancelOrderReq, memberInfo.getMemberAccount());
                    return RestResult.ok();
                }

                //校验当前订单状态
                //3  待支付
                //只有待支付状态才能进行取消支付订单
                if (!OrderStatusEnum.BE_PAID.getCode().equals(collectionOrder.getOrderStatus())) {
                    log.error("取消支付失败, 订单状态必须为: 3 待支付 才能够进行取消 当前订单状态为: {}, 订单信息: {}, cancelOrderReq: {}, 会员账号: {}", collectionOrder.getOrderStatus(), collectionOrder, cancelOrderReq, memberInfo.getMemberAccount());
                    return RestResult.failure(ResultCode.ORDER_STATUS_VERIFICATION_FAILED);
                }
                //设置取消类型
                collectionOrder.setCancelType(CollectionOrderCancelTypeEnum.CancelPay.getCode());
                //更新买入订单状态为: 已取消 并添加取消支付标识 并填写取消原因
                collectionOrderToCancelPayment(collectionOrder, cancelOrderReq);

                //查询撮合列表 加上排他行锁
                MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderForUpdate(collectionOrder.getMatchingPlatformOrder());
                //更新撮合列表状态为: 已取消 并填写取消原因
                matchingOrderToWasCanceled(collectionOrder, matchingOrder, cancelOrderReq, OrderStatusEnum.WAS_CANCELED);

                //查询卖出订单 加上排他行锁
                PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());


                //分布式锁key ar-wallet-sell+会员id
                String key2 = "ar-wallet-sell" + paymentOrder.getMemberId();
                RLock lock2 = redissonUtil.getLock(key2);

                boolean req2 = false;

                try {
                    req2 = lock2.tryLock(10, TimeUnit.SECONDS);

                    if (req2) {


                        //匹配时间戳
                        Long lastUpdateTimestamp = System.currentTimeMillis();

                        MatchPool matchPool = null;

                        //查看卖出订单是否拆单
                        if (StringUtils.isNotEmpty(paymentOrder.getMatchOrder())) {
                            //拆单

                            //获取匹配池订单 加上排他行锁
                            matchPool = matchPoolMapper.selectMatchPoolForUpdate(paymentOrder.getMatchOrder());

                            //剩余金额
                            rAmout = matchPool.getRemainingAmount();

                            //判断匹配池订单如果处于匹配中状态 那么就将金额退回到匹配池订单剩余金额
                            if (OrderStatusEnum.BE_MATCHED.getCode().equals(matchPool.getOrderStatus())) {

                                //更新匹配池订单 将该笔订单金额 添加到 匹配池剩余金额 并填写取消原因 减去已卖出金额
                                matchPoolToWasCanceled(matchPool, paymentOrder, cancelOrderReq);

                                //将最大限额加上订单金额
                                //需要注册事务同步回调(将订单信息存入到Redis订单列表)
                                shouldRegisterTransactionCallback = true;
                                isSplitOrder = true;
                                sellMemberInfo = memberInfoService.getById(paymentOrder.getMemberId());

                            } else {

                                //如果匹配订单不处于匹配中状态 那么就将金额退回到用户余额

                                //进行中订单 -1
                                matchPool.setInProgressOrderCount(matchPool.getInProgressOrderCount() - 1);

                                //已完成订单 +1
                                matchPool.setCompletedOrderCount(matchPool.getCompletedOrderCount() + 1);

                                //更新匹配池订单
                                matchPoolService.updateById(matchPool);

                                //获取卖出会员信息 加上排他行锁
                                sellMemberInfo = memberInfoMapper.selectMemberInfoForUpdate(Long.valueOf(paymentOrder.getMemberId()));

                                //账变前余额
                                BigDecimal previousBalance = sellMemberInfo.getBalance();

                                //将订单金额退回到会员余额里面
                                sellMemberInfo.setBalance(sellMemberInfo.getBalance().add(paymentOrder.getAmount()));

                                //将用户冻结金额 减去 该笔订单金额
                                sellMemberInfo.setFrozenAmount(sellMemberInfo.getFrozenAmount().subtract(paymentOrder.getAmount()));

                                //账变后余额
                                BigDecimal newBalance = sellMemberInfo.getBalance();

                                //记录会员账变信息
                                memberAccountChangeService.recordMemberTransaction(String.valueOf(sellMemberInfo.getId()), paymentOrder.getAmount(), MemberAccountChangeEnum.CANCEL_RETURN.getCode(), paymentOrder.getPlatformOrder(), previousBalance, newBalance, "");

                                //更新会员信息
                                boolean b = memberInfoService.updateById(sellMemberInfo);

                                log.info("取消支付 匹配池订单不处于匹配中状态 将金额退回到用户余额, 卖出会员信息: {}, 账变前余额: {}, 账变后余额: {}, sql执行结果: {}", sellMemberInfo, previousBalance, newBalance, b);

                            }
                            //将卖出订单状态改为: 已取消
                            paymentOrder.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());

                            //卖出订单 填写取消原因
                            paymentOrder.setCancellationReason(cancelOrderReq.getReason());

                            //取消时间
                            paymentOrder.setCancelTime(LocalDateTime.now());

                            //取消人
                            paymentOrder.setCancelBy(memberInfo.getMemberAccount());

                        } else {

                            //非拆单

                            //需要注册事务同步回调(将订单信息存入到Redis订单列表)
                            shouldRegisterTransactionCallback = true;

                            sellMemberInfo = memberInfoService.getById(paymentOrder.getMemberId());

                            //非拆单 将卖出订单状态改为 匹配中
                            paymentOrder.setOrderStatus(OrderStatusEnum.BE_MATCHED.getCode());
                            //更新匹配时间戳
                            paymentOrder.setLastUpdateTimestamp(lastUpdateTimestamp);

                            //根据会员标签获取对应配置信息
                            TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo);

                            //从配置表获取 钱包用户卖出匹配时长 并将分钟转为毫秒
                            long millis = TimeUnit.MINUTES.toMillis(schemeConfigByMemberTag.getSchemeSellMatchingDuration());

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

                            log.info("取消支付 匹配到的订单为非拆单 将卖出订单为匹配中, 买入会员账号: {}, 卖出会员id: {}, 卖出订单信息: {}, 更新匹配时间戳: {}, 匹配剩余时间(分钟): {}", memberInfo.getMemberAccount(), paymentOrder.getMemberId(), paymentOrder, lastUpdateTimestamp, schemeConfigByMemberTag.getSchemeSellMatchingDuration());
                        }
                        //更新卖出订单
                        paymentOrderService.updateById(paymentOrder);

                        //获取收款信息 加上排他行锁
                        CollectionInfo collectionInfo = collectionInfoMapper.selectCollectionInfoForUpdate(paymentOrder.getCollectionInfoId());
                        //更新收款信息
                        updateCollectionInfo(collectionInfo, paymentOrder);

                        //记录会员买入失败次数
                        redisUtil.recordMemberBuyFailure(String.valueOf(memberInfo.getId()));

                        log.info("取消支付处理成功 会员账号: {}, req: {}", memberInfo.getMemberAccount(), cancelOrderReq);

                        // 注册事务同步回调
                        final PaymentOrder finalPaymentOrder = paymentOrder;
                        final MemberInfo finalMemberInfo = sellMemberInfo;
                        final boolean finalShouldRegisterTransactionCallback = shouldRegisterTransactionCallback;
                        final MatchPool finalMatchPool = matchPool;
                        final Boolean finalIsSplitOrder = isSplitOrder;
                        final BigDecimal finalRAmout = rAmout;
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {

                                //买方取消支付了 WebSocket通知卖方
                                NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage
                                        = new NotifyOrderStatusChangeMessage(String.valueOf(finalMemberInfo.getId()), NotificationTypeEnum.NOTIFY_SELLER.getCode(), finalPaymentOrder.getPlatformOrder());

                                notifyOrderStatusChangeSend.send(notifyOrderStatusChangeMessage);

                                if (finalPaymentOrder != null && finalMemberInfo != null && finalShouldRegisterTransactionCallback) {

                                    // 事务提交后执行的Redis操作

                                    BuyListVo buyListVo = new BuyListVo();


                                    //判断是否拆单
                                    if (finalIsSplitOrder) {
                                        //删除之前的Redis订单
                                        redisUtil.deleteOrder(finalMatchPool.getMatchOrder());
                                        //订单号
                                        buyListVo.setPlatformOrder(finalMatchPool.getMatchOrder());

                                        //拆单 最大限额 是 之前匹配池剩余金额 + 当前取消的订单金额
                                        buyListVo.setMaximumAmount(finalRAmout.add(finalPaymentOrder.getAmount()));
                                        //订单金额 是 之前匹配池剩余金额 + 当前取消的订单金额
                                        buyListVo.setAmount(finalRAmout.add(finalPaymentOrder.getAmount()));
                                        //最小限额 匹配池的最小限额
                                        buyListVo.setMinimumAmount(finalMatchPool.getMinimumAmount());
                                    } else {
                                        //非拆单

                                        //将upi今日收款次数-1
//                                        upiTransactionService.decrementDailyTransactionCountIfExist(collectionInfo.getUpiId());

                                        //订单号
                                        buyListVo.setPlatformOrder(finalPaymentOrder.getPlatformOrder());
                                        //最大限额 非拆单就是订单金额
                                        buyListVo.setMaximumAmount(finalPaymentOrder.getAmount());
                                        //订单金额
                                        buyListVo.setAmount(finalPaymentOrder.getAmount());
                                        //最小限额 非拆单就是订单金额
                                        buyListVo.setMinimumAmount(finalPaymentOrder.getAmount());
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
                                }

                                if (finalMatchPool != null) {
                                    //匹配池订单有子订单 查询全部子订单 并根据子订单状态更新匹配池订单状态
                                    sellService.updateMatchPoolOrderStatus(finalMatchPool.getMatchOrder());
                                }
                            }
                        });

                        return RestResult.ok();

                    }
                } catch (Exception e) {
                    //手动回滚
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    log.error("取消支付处理失败 req: {}, e: {}", cancelOrderReq, e);
                } finally {
                    //释放锁
                    if (req && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }

                    if (req2 && lock2.isHeldByCurrentThread()) {
                        lock2.unlock();
                    }
                }
            }
        } catch (Exception e) {
            //手动回滚
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("取消支付处理失败 req: {}, e: {}", cancelOrderReq, e);
        } finally {
            //释放锁
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        log.error("取消支付处理失败 req: {}", cancelOrderReq);
        return RestResult.failure(ResultCode.SYSTEM_EXECUTION_ERROR);
    }

    /**
     * 获取支付页面数据
     *
     * @return {@link RestResult}<{@link BuyVo}>
     */
    @Override
    public RestResult<BuyVo> getPaymentPageData() {

        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取支付页面数据失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //查询会员待支付的买入订单
        CollectionOrder pendingBuyOrder = collectionOrderService.getPendingBuyOrder(String.valueOf(memberInfo.getId()));

        if (pendingBuyOrder == null) {
            log.error("获取支付页面数据失败 订单已超时, 会员账号: {}", memberInfo.getMemberAccount());
            return RestResult.failure(ResultCode.ORDER_EXPIRED);
        }

        //创建返回数据
        BuyVo buyVo = new BuyVo();
        BeanUtils.copyProperties(pendingBuyOrder, buyVo);

        //获取支付剩余时间
        buyVo.setPaymentExpireTime(redisUtil.getPaymentRemainingTime(buyVo.getPlatformOrder()));

        log.info("获取支付页面数据成功, 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), buyVo);

        return RestResult.ok(buyVo);
    }

    /**
     * 获取USDT支付页面数据
     *
     * @return {@link RestResult}<{@link UsdtBuyVo}>
     */
    @Override
    public RestResult<UsdtBuyVo> getUsdtPaymentPageData() {

        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取USDT支付页面数据失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //查询会员待支付的USDT买入订单
        UsdtBuyOrder pendingUsdtBuyOrder = usdtBuyOrderService.getPendingUsdtBuyOrder(UserContext.getCurrentUserId());

        if (pendingUsdtBuyOrder == null) {
            log.error("获取USDT支付页面数据失败 订单已超时, 会员账号: {}", memberInfo.getMemberAccount());
            return RestResult.failure(ResultCode.ORDER_EXPIRED);
        }

        UsdtBuyVo usdtBuyVo = new UsdtBuyVo();

        BeanUtils.copyProperties(pendingUsdtBuyOrder, usdtBuyVo);

        //获取支付剩余时间
        usdtBuyVo.setUsdtPaymentExpireTime(redisUtil.getUsdtPaymentRemainingTime(pendingUsdtBuyOrder.getPlatformOrder()));

        log.info("获取USDT支付页面数据成功, 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), usdtBuyVo);

        return RestResult.ok(usdtBuyVo);
    }

    /**
     * 获取取消买入页面数据
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    @Override
    public RestResult<CancelBuyPageDataVo> getCancelBuyPageData(PlatformOrderReq platformOrderReq) {

        //获取会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null) {
            log.error("获取取消买入页面数据失败: 获取会员信息失败");
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //买入根据订单号 查询买入订单
        CollectionOrder collectionOrder = collectionOrderService.getCollectionOrderByPlatformOrder(platformOrderReq.getPlatformOrder());

        String memberId = String.valueOf(memberInfo.getId());

        if (collectionOrder == null || !collectionOrder.getMemberId().equals(memberId)) {
            log.error("获取取消买入页面数据失败 该订单不存在或该订单不属于该会员 会员信息: {}, 订单信息: {}", memberInfo, collectionOrder);
            return RestResult.failure(ResultCode.ORDER_VERIFICATION_FAILED);
        }

        CancelBuyPageDataVo cancelBuyPageDataVo = new CancelBuyPageDataVo();

        BeanUtils.copyProperties(collectionOrder, cancelBuyPageDataVo);

        //获取充值取消原因列表
        cancelBuyPageDataVo.setReason(cancellationRechargeService.getBuyCancelReasonsList());

        log.info("获取取消买入页面数据成功 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), cancelBuyPageDataVo);

        return RestResult.ok(cancelBuyPageDataVo);
    }

    /**
     * 获取支付类型
     *
     * @return {@link RestResult}<{@link List}<{@link PaymentTypeVo}>>
     */
    @Override
    public RestResult<List<PaymentTypeVo>> getPaymentType() {
        PaymentTypeVo paymentTypeVo = new PaymentTypeVo();

        List<PaymentTypeVo> res = new ArrayList<>();

        res.add(paymentTypeVo);

        return RestResult.ok(res);
    }

    /**
     * 更新买入订单为 申诉中
     *
     * @param collectionOrder
     * @return {@link Boolean}
     */
    private Boolean updateCollectionOrderToAppealInProgress(CollectionOrder collectionOrder) {

        //更新买入订单状态为 申诉中
        collectionOrder.setOrderStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新买入订单 申诉时间
        collectionOrder.setAppealTime(LocalDateTime.now());

        boolean b = collectionOrderService.updateById(collectionOrder);

        log.info("买入订单申诉处理 更新买入订单信息: {}, sql执行结果: {}", collectionOrder, b);

        return b;
    }

    /**
     * 更新撮合列表订单为 申诉中
     *
     * @param matchingOrder
     * @return {@link Boolean}
     */
    private Boolean updateMatchingOrderToAppealInProgress(MatchingOrder matchingOrder) {

        //更新撮合列表订单状态为: 申诉中
        matchingOrder.setStatus(OrderStatusEnum.COMPLAINT.getCode());

        //申诉时间
        matchingOrder.setAppealTime(LocalDateTime.now());

        boolean b = matchingOrderService.updateById(matchingOrder);

        log.info("买入订单申诉处理 更新撮合列表订单信息: {}, sql执行结果: {}", matchingOrder, b);

        return b;
    }


    /**
     * 更新卖出订单为 申诉中
     *
     * @param paymentOrder
     * @return {@link Boolean}
     */
    private Boolean updatePaymentOrderToAppealInProgress(PaymentOrder paymentOrder) {

        //更新卖出订单状态为: 申诉中
        paymentOrder.setOrderStatus(OrderStatusEnum.COMPLAINT.getCode());

        //更新申诉时间
        paymentOrder.setAppealTime(LocalDateTime.now());

        boolean b = paymentOrderService.updateById(paymentOrder);

        log.info("买入订单申诉处理 更新卖出订单信息: {}, sql执行结果: {}", paymentOrder, b);

        return b;
    }

    /**
     * 生成申诉订单
     *
     * @param complainant
     * @param collectionOrder
     * @param paymentOrder
     * @param buyOrderAppealImage
     * @param buyOrderAppealVideo
     * @param appealReason
     * @return {@link Boolean}
     */
    private Boolean createAppealOrder(MemberInfo complainant, CollectionOrder collectionOrder, PaymentOrder paymentOrder, String buyOrderAppealImage, String buyOrderAppealVideo, String appealReason) {

        //先查询 如果存在申诉订单了 那么只是改变申诉订单状态就可以了
        //根据买入订单号 查询申诉订单 加上排他行锁
        AppealOrder appealOrderNew = appealOrderMapper.selectAppealOrderByRechargeOrderNoForUpdate(collectionOrder.getPlatformOrder());

        if (appealOrderNew != null) {
            //已存在申诉订单, 只是改变申诉订单状态即可

            //判断 如果申诉订单是 未支付状态 才改变申诉状态
            String appealStatus = String.valueOf(appealOrderNew.getAppealStatus());

            if (AppealStatusEnum.UNPAID.getCode().equals(appealStatus)) {
                //申诉状态 1为待处理
                appealOrderNew.setAppealStatus(Integer.valueOf(AppealStatusEnum.PENDING.getCode()));

                //申诉类型 2为买入订单申诉
                appealOrderNew.setAppealType(2);

                //UTR
                appealOrderNew.setUtr(collectionOrder.getUtr());

                //所属商户 这是买入申诉  所以记录买家的商户名称
                appealOrderNew.setMerchantName(collectionOrder.getMerchantName());

                //申诉订单金额
                appealOrderNew.setOrderAmount(collectionOrder.getAmount());

                //会员id
                appealOrderNew.setMid(String.valueOf(complainant.getId()));

                //被申诉会员id 买入订单申诉 所以卖出订单的会员id就是被申诉人id
                appealOrderNew.setAppealedMemberId(paymentOrder.getMemberId());

                //会员账号
                appealOrderNew.setMAccount(complainant.getMemberAccount());

                //买入订单号
                appealOrderNew.setRechargeOrderNo(collectionOrder.getPlatformOrder());

                //卖出订单号
                appealOrderNew.setWithdrawOrderNo(paymentOrder.getPlatformOrder());

                //实际金额
                appealOrderNew.setActualAmount(collectionOrder.getActualAmount());

                //申诉原因
                if (appealReason != null) {
                    appealOrderNew.setReason(appealReason);
                }

                //申诉图片
                appealOrderNew.setPicInfo(buyOrderAppealImage);

                //申诉视频
                if (buyOrderAppealVideo != null) {
                    appealOrderNew.setVideoUrl(buyOrderAppealVideo);
                }

                boolean b = appealOrderService.updateById(appealOrderNew);

                log.info("买入订单申诉 已存在申诉订单, 更改申诉订单信息: {}, sql执行结果: {}", appealOrderNew, b);

                return b;
            } else {
                return true;
            }
        } else {
            AppealOrder appealOrder = new AppealOrder();

            //UTR
            appealOrder.setUtr(collectionOrder.getUtr());

            //申诉类型 2为买入订单申诉
            appealOrder.setAppealType(2);

            //所属商户 这是买入申诉  所以记录买家的商户名称
            appealOrder.setMerchantName(collectionOrder.getMerchantName());

            //申诉状态 1为待处理
            appealOrder.setAppealStatus(Integer.valueOf(AppealStatusEnum.PENDING.getCode()));

            //申诉订单金额
            appealOrder.setOrderAmount(collectionOrder.getAmount());

            //会员id
            appealOrder.setMid(String.valueOf(complainant.getId()));

            //被申诉会员id 买入订单申诉 所以卖出订单的会员id就是被申诉人id
            appealOrder.setAppealedMemberId(paymentOrder.getMemberId());

            //会员账号
            appealOrder.setMAccount(complainant.getMemberAccount());

            //买入订单号
            appealOrder.setRechargeOrderNo(collectionOrder.getPlatformOrder());

            //卖出订单号
            appealOrder.setWithdrawOrderNo(paymentOrder.getPlatformOrder());

            //实际金额
            appealOrder.setActualAmount(collectionOrder.getActualAmount());

            //申诉原因
            if (appealReason != null) {
                appealOrder.setReason(appealReason);
            }

            //申诉图片
            appealOrder.setPicInfo(buyOrderAppealImage);

            //申诉视频
            if (buyOrderAppealVideo != null) {
                appealOrder.setVideoUrl(buyOrderAppealVideo);
            }

            boolean save = appealOrderService.save(appealOrder);

            log.info("买入订单申诉处理 会员信息: {}, 生成申诉订单: {}, sql执行结果", complainant, appealOrder, save);

            return save;
        }
    }


    @Override
    public List<BuyProcessingOrderListVo> processingBuyOrderList(Long memberId, boolean getFromRedis) {
        if(ObjectUtils.isEmpty(arProperty.getProcessOrderSource())
                || arProperty.getProcessOrderSource() == 1
                || !getFromRedis
        ){
            // 获取待支付、确认中、申诉中的买入订单列表
            List<CollectionOrder> records = collectionOrderService.processingBuyOrderList(memberId);
            if(ObjectUtils.isEmpty(records)){
                return Collections.emptyList();
            }
            return processingBuyOrderVo(memberId, records);
        }
        // 从redis中获取进行中的订单号信息
        List<String> memberProcessingOrder = redisUtil.getMemberProcessingOrder(String.valueOf(memberId));
        return processingBuyOrderList(memberId, memberProcessingOrder);
    }

    public List<BuyProcessingOrderListVo> processingBuyOrderList(Long memberId, List<String> platformOrderList) {
        // 根据redis中的订单号获取进行中的订单信息
        List<CollectionOrder> records = collectionOrderService.processingBuyOrderList(platformOrderList);
        if(ObjectUtils.isEmpty(records)){
            return Collections.emptyList();
        }
        return processingBuyOrderVo(memberId, records);
    }

    private List<BuyProcessingOrderListVo> processingBuyOrderVo(Long memberId,  List<CollectionOrder> records){
        Long countdown = null;
        Long countdownLimit = null;
        List<BuyProcessingOrderListVo> resultList = new ArrayList<>();
        // 获取配置表中超时配置
        MemberInfo memberInfo = memberInfoService.getMemberInfoById(String.valueOf(memberId));
        TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(memberInfo);
        TradeConfig tradeConfig = tradeConfigService.getById(1);
        // 待支付时限
        Integer rechargeExpirationTime = tradeConfig.getRechargeExpirationTime();
        // 确认时限
        Integer schemeConfirmExpirationTime = schemeConfigByMemberTag.getSchemeConfirmExpirationTime();

        for (CollectionOrder record : records) {
            BuyProcessingOrderListVo vo = new BuyProcessingOrderListVo();
            BeanUtil.copyProperties(record, vo);
            if (vo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode())){
                long confirmRemainingTime = redisUtil.getConfirmRemainingTime(vo.getPlatformOrder());
                countdown = confirmRemainingTime < 1 ? 0 : confirmRemainingTime * 1000L;
                countdownLimit = schemeConfirmExpirationTime * 60 * 1000L;
                if(countdown < 1){
                    continue;
                }
            }

            if (vo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode())){
                long paymentRemainingTime = redisUtil.getPaymentRemainingTime(vo.getPlatformOrder());
                countdown = paymentRemainingTime < 1 ? 0 : paymentRemainingTime * 1000L;
                countdownLimit = rechargeExpirationTime * 60 * 1000L;
                if(countdown < 1){
                    continue;
                }
            }
            //设置是否经过申诉
            if (record.getAppealTime() != null){
                vo.setIsAppealed(1);
            }
            // 人工审核状态添加
            if(OrderStatusEnum.isAuditing(record.getOrderStatus(), record.getAuditDelayTime())){
                vo.setOrderStatus(OrderStatusEnum.AUDITING.getCode());
            }
            // 设置倒计时
            vo.setCountdown(countdown);
            vo.setCountdownLimit(countdownLimit);
            resultList.add(vo);
        }
        return resultList;
    }
}
