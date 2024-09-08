//package org.ar.job.processor;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.ar.common.redis.constants.RedisKeys;
//import org.ar.job.feign.HandleOrderTimeoutFeignClient;
//import org.ar.wallet.Enum.OrderStatusEnum;
//import org.ar.wallet.entity.*;
//import org.ar.wallet.mapper.*;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.transaction.interceptor.TransactionAspectSupport;
//import tech.powerjob.worker.core.processor.ProcessResult;
//import tech.powerjob.worker.core.processor.TaskContext;
//import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
//import tech.powerjob.worker.log.OmsLogger;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//
//@Component("checkOrderTimeoutProcessor")
//@Slf4j
//@RequiredArgsConstructor
//public class CheckOrderTimeoutProcessor implements BasicProcessor {
//    private final CollectionOrderMapper collectionOrderMapper;
//    private final TradeConfigMapper tradeConfigMapper;
//    private final UsdtBuyOrderMapper usdtBuyOrderMapper;
//    private final PaymentOrderMapper paymentOrderMapper;
//    private final RedisTemplate redisTemplate;
//    private final MatchPoolMapper matchPoolMapper;
//    private final MatchingOrderMapper matchingOrderMapper;
//
//
//    private final HandleOrderTimeoutFeignClient handleOrderTimeoutFeignClient;
//
//
//    /**
//     * 定时任务 检测订单是否超时 (避免极端情况 MQ延时任务失败)
//     *
//     * @author Simon
//     * @date 2023/12/01
//     */
//    @Override
//    @Transactional
//    public ProcessResult process(TaskContext context) {
//
//        log.info("定时任务执行: 检测订单是否超时, 当前时间: {}", LocalDateTime.now());
//
//        try {
//
//            //获取配置信息
//            TradeConfig tradeConfig = tradeConfigMapper.selectById(1);
//
//            //支付超时时间 优先让MQ去处理超时订单 所以在查询时间上 + 5分钟
//            Integer rechargeExpirationTime = tradeConfig.getRechargeExpirationTime() + 5;
//
//            //钱包用户确认超时时间
//            Integer memberConfirmExpirationTime = tradeConfig.getMemberConfirmExpirationTime() + 5;
//
//            //商户会员确认超时时间
//            Integer merchantConfirmExpirationTime = tradeConfig.getMerchantConfirmExpirationTime() + 5;
//
//            //钱包用户卖出匹配超时时间
//            int memberSellMatchingDuration = tradeConfig.getMemberSellMatchingDuration() + 5;
//
//            //商户会员卖出匹配超时时间
//            Integer merchantSellMatchingDuration = tradeConfig.getMerchantSellMatchingDuration() + 5;
//
//
//            //买入支付超时
//            checkBuyOrderPaymentTimeout(rechargeExpirationTime);
//
//            //USDT支付超时
//            checkUsdtBuyOrderPaymentTimeout(rechargeExpirationTime);
//
//            //钱包用户卖出匹配超时
//            checkSellOrderSaleMatchTimeout(memberSellMatchingDuration);
//
//            //钱包用户确认超时
//            checkOrderConfirmationTimeout(memberConfirmExpirationTime);
//
//
//        } catch (Exception e) {
//            //手动回滚
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            log.error("检测订单是否超时定时任务执行失败: e: {}", e);
//        }
//
//        // 在线日志功能，可以直接在控制台查看任务日志，非常便捷
//        OmsLogger omsLogger = context.getOmsLogger();
//        omsLogger.info("BasicProcessorDemo start to process, current JobParams is {}.", context.getJobParams());
//
//        return new ProcessResult(true, "return success");
//    }
//
//    /**
//     * 钱包用户确认超时
//     *
//     * @param memberConfirmExpirationTime
//     */
//    private void checkOrderConfirmationTimeout(Integer memberConfirmExpirationTime) {
//        List<MatchingOrder> matchingOrders = new ArrayList<>();
//
//        //查询匹配池订单 超过 minutes 分钟 状态为: 确认中
//        matchingOrders = matchingOrderMapper.findOrdersInConfirmation(OrderStatusEnum.CONFIRMATION.getCode(), memberConfirmExpirationTime);
//
//        log.info("定时任务: 检测订单是否超时: 钱包用户确认超时的撮合列表订单: {}", matchingOrders);
//
//        for (MatchingOrder matchingOrder : matchingOrders) {
//            //将订单改为 确认超时
//            handleOrderTimeoutFeignClient.walletUserConfirmationTimeout(matchingOrder.getPlatformOrder());
//        }
//    }
//
//
//    /**
//     * 钱包用户卖出匹配超时
//     *
//     * @param memberSellMatchingDuration
//     */
//    private void checkSellOrderSaleMatchTimeout(Integer memberSellMatchingDuration) {
//
//
//        List<PaymentOrder> paymentOrders = new ArrayList<>();
//
//        //查询卖出订单 超过 minutes 分钟 状态为: 匹配中
//        paymentOrders = paymentOrderMapper.findMatchingSellOrders(
//                OrderStatusEnum.BE_MATCHED.getCode(), memberSellMatchingDuration);
//
//        log.info("定时任务: 检测订单是否超时: 钱包用户卖出匹配超时的卖出订单: {}", paymentOrders);
//
//        for (PaymentOrder paymentOrder : paymentOrders) {
//
//            //Redis获取订单最后匹配时间戳
//            String redisLastMatchTimeKey = RedisKeys.ORDER_LASTMATCHTIME + paymentOrder.getPlatformOrder();
//
//            log.info("最后匹配时间戳: {}", redisLastMatchTimeKey);
//
//            // 检查键是否存在
//            if (redisTemplate.hasKey(redisLastMatchTimeKey)) {
//                // 获取并返回值，确保它被正确地转换为Long类型
//                Object value = redisTemplate.opsForValue().get(redisLastMatchTimeKey);
//
//                if (value != null) {
//                    try {
//                        long lastMatchTime = Long.parseLong(value.toString());
//
//                        //将订单改为 匹配超时
//                        Boolean b = handleOrderTimeoutFeignClient.walletUserSaleMatchTimeout(paymentOrder.getPlatformOrder(), lastMatchTime);
//
//                        System.out.println("b: " + b);
//                    } catch (NumberFormatException e) {
//                        // 处理转换异常
//                        throw e;
//                    }
//                }
//            }
//        }
//
//        //查询匹配池
//        List<MatchPool> matchPools = new ArrayList<>();
//
//        matchPools = matchPoolMapper.findMatchingSellOrders(OrderStatusEnum.BE_MATCHED.getCode(), memberSellMatchingDuration);
//
//        log.info("定时任务: 检测订单是否超时: 钱包用户卖出匹配超时的匹配池订单: {}", matchPools);
//
//        for (MatchPool matchPool : matchPools) {
//
//            //Redis获取订单最后匹配时间戳
//            String redisLastMatchTimeKey = RedisKeys.ORDER_LASTMATCHTIME + matchPool.getMatchOrder();
//
//            // 检查键是否存在
//            if (redisTemplate.hasKey(redisLastMatchTimeKey)) {
//                // 获取并返回值，确保它被正确地转换为Long类型
//                Object value = redisTemplate.opsForValue().get(redisLastMatchTimeKey);
//
//                if (value != null) {
//                    try {
//                        long lastMatchTime = Long.parseLong(value.toString());
//
//                        //将订单改为 匹配超时
//                        handleOrderTimeoutFeignClient.walletUserSaleMatchTimeout(matchPool.getMatchOrder(), lastMatchTime);
//                    } catch (NumberFormatException e) {
//                        // 处理转换异常
//                        throw e;
//                    }
//                }
//            }
//        }
//    }
//
//
//    /**
//     * 买入支付超时
//     *
//     * @param rechargeExpirationTime
//     */
//    private void checkBuyOrderPaymentTimeout(Integer rechargeExpirationTime) {
//
//        List<CollectionOrder> collectionOrders = new ArrayList<>();
//
//        //查询买入订单 超过 minutes 分钟 状态为: 待支付
//        collectionOrders = collectionOrderMapper.findPendingBuyOrders(OrderStatusEnum.BE_PAID.getCode(), rechargeExpirationTime);
//
//        log.info("定时任务: 检测订单是否超时: 支付超时的买入订单: {}", collectionOrders);
//
//        for (CollectionOrder collectionOrder : collectionOrders) {
//            //将订单改为支付超时
//            handleOrderTimeoutFeignClient.paymentTimeout(collectionOrder.getPlatformOrder());
//        }
//    }
//
//
//    /**
//     * USDT支付超时
//     *
//     * @param rechargeExpirationTime
//     */
//    private void checkUsdtBuyOrderPaymentTimeout(Integer rechargeExpirationTime) {
//
//        List<UsdtBuyOrder> usdtBuyOrders = new ArrayList<>();
//
//        //查询USDT买入订单 超过 minutes 分钟 状态为: 待支付
//        usdtBuyOrders = usdtBuyOrderMapper.findPendingUsdtBuyOrders(OrderStatusEnum.BE_PAID.getCode(), rechargeExpirationTime);
//
//        log.info("定时任务: 检测订单是否超时: USDT支付超时的买入订单: {}", usdtBuyOrders);
//
//        for (UsdtBuyOrder usdtBuyOrder : usdtBuyOrders) {
//            handleOrderTimeoutFeignClient.usdtPaymentTimeout(usdtBuyOrder.getPlatformOrder());
//        }
//    }
//}
