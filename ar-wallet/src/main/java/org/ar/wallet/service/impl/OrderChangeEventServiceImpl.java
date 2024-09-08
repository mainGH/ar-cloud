package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.utils.StringUtils;
import org.ar.wallet.Enum.NotificationTypeEnum;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.entity.*;
import org.ar.wallet.service.*;
import org.ar.wallet.vo.BuyProcessingOrderListVo;
import org.ar.wallet.vo.SellProcessingOrderListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.ar.common.redis.constants.RedisKeys.MEMBER_PROCESSING_ORDER;

@Service
@Slf4j
public class OrderChangeEventServiceImpl implements OrderChangeEventService {
    @Autowired
    private IPaymentOrderService paymentOrderService;
    @Autowired
    private IMatchPoolService matchPoolService;
    @Autowired
    private IMatchingOrderService matchingOrderService;
    @Autowired
    private ICollectionOrderService collectionOrderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    @Lazy
    private IBuyService buyService;
    @Autowired
    @Lazy
    private ISellService sellService;

    /**
     * 处理订单状所有状态变化的事件("卖出"除外)
     *
     * @param orderStatusChangeMessage
     */
    @Override
    public void process(NotifyOrderStatusChangeMessage orderStatusChangeMessage) {
        PaymentOrder paymentOrder;
        MatchingOrder matchingOrder;
        CollectionOrder collectionOrder;
        MatchPool matchPool = null;
        // 查询相关联的订单, 需要根据买入/卖出订单查询撮合订单, 因为撮合订单没有相关索引, 这里需要间接查询
        if (NotificationTypeEnum.NOTIFY_SELLER.getCode().equals(orderStatusChangeMessage.getType())) {
            String payOrderNo = orderStatusChangeMessage.getPlatformOrder();
            paymentOrder = paymentOrderService.getPaymentOrderByOrderNo(payOrderNo);
            if (paymentOrder == null) {
                log.info("订单状态变化处理, 本次变化事件丢弃, 未查询到卖出订单:{}, 消息类型:{}", payOrderNo, orderStatusChangeMessage.getType());
                return;
            }
            String matchPoolNo = paymentOrder.getMatchOrder();
            if (!StringUtils.isEmpty(matchPoolNo)) {
                matchPool = matchPoolService.getMatchPoolOrderByOrderNo(matchPoolNo);
            }
            String matchingOrderNo = paymentOrder.getMatchingPlatformOrder();
            if (StringUtils.isEmpty(matchingOrderNo)) {
                log.info("订单状态变化处理, 本次变化事件丢弃, 卖出订单没有对应撮合订单:{}, 消息类型:{}", payOrderNo, orderStatusChangeMessage.getType());
                return;
            }
            matchingOrder = matchingOrderService.getMatchingOrder(matchingOrderNo);
            if (matchingOrder == null) {
                log.info("订单状态变化处理, 本次变化事件丢弃, 未查询到撮合订单:{}, 消息类型:{}", matchingOrderNo, orderStatusChangeMessage.getType());
                return;
            }
        } else {
            String buyOrderNo = orderStatusChangeMessage.getPlatformOrder();
            collectionOrder = collectionOrderService.getCollectionOrderByPlatformOrder(buyOrderNo);
            if (collectionOrder == null) {
                log.info("订单状态变化处理, 本次变化事件丢弃, 未查询到买入订单:{}, 消息类型:{}", buyOrderNo, orderStatusChangeMessage.getType());
                return;
            }
            String matchingOrderNo = collectionOrder.getMatchingPlatformOrder();
            matchingOrder = matchingOrderService.getMatchingOrder(matchingOrderNo);
            if (matchingOrder == null) {
                log.info("订单状态变化处理, 本次变化事件丢弃, 未查询到撮合订单:{}, 消息类型:{}", matchingOrderNo, orderStatusChangeMessage.getType());
                return;
            }
            String payOrderNo = matchingOrder.getPaymentPlatformOrder();
            paymentOrder = paymentOrderService.getPaymentOrderByOrderNo(payOrderNo);
            if (paymentOrder == null) {
                log.info("订单状态变化处理, 本次变化事件丢弃, 未查询到卖出订单:{}, 消息类型:{}", payOrderNo, orderStatusChangeMessage.getType());
                return;
            }
            String matchPoolNo = paymentOrder.getMatchOrder();
            if (!StringUtils.isEmpty(matchPoolNo)) {
                matchPool = matchPoolService.getMatchPoolOrderByOrderNo(matchPoolNo);
            }
        }
        // 缓存会员进行中的订单
        cachedMemberProcessingOrder(matchingOrder, paymentOrder, matchPool);

    }

    /**
     * 卖出订单处理, 只处理此单一场景
     *
     * @param orderStatusChangeMsg
     */
    @Override
    public void processSellOrder(NotifyOrderStatusChangeMessage orderStatusChangeMsg) {
        String sellOrderNo = orderStatusChangeMsg.getPlatformOrder();
        String sellerKey = String.format(MEMBER_PROCESSING_ORDER, orderStatusChangeMsg.getMemberId());

        // 缓存会员进行中的订单
        log.info("订单状态变化处理, 卖出订单场景, 缓存会员进行中的订单-卖出:{}, memberId:{}", sellOrderNo, orderStatusChangeMsg.getMemberId());
        redisTemplate.opsForSet().add(sellerKey, sellOrderNo);

        // 其他业务可在此扩展
    }


    /**
     * 处理取消卖出订单(无撮合订单)
     *
     * @param orderStatusChangeMsg
     */
    @Override
    public void processCancelSellOrder(NotifyOrderStatusChangeMessage orderStatusChangeMsg) {
        String sellOrderNo = orderStatusChangeMsg.getPlatformOrder();
        String sellerKey = String.format(MEMBER_PROCESSING_ORDER, orderStatusChangeMsg.getMemberId());

        // 缓存会员进行中的订单
        log.info("订单状态变化处理, 取消订单, 移除会员进行中的订单-卖出:{}, memberId:{}", sellOrderNo, orderStatusChangeMsg.getMemberId());
        redisTemplate.opsForSet().remove(sellerKey, sellOrderNo);

    }


    /**
     * 同步会员进行中的订单缓存
     */
    @Override
    public void syncMemberProcessingOrderCache() {
        log.info("同步会员进行中的订单缓存...");
        Set<String> processingStatus = new HashSet<>(Arrays.asList(OrderStatusEnum.BUYING_STATUS));
        processingStatus.addAll(Arrays.asList(OrderStatusEnum.SELLING_STATUS));
        LocalDateTime queryStartTime = LocalDateTime.now().minusMonths(9);
        LambdaQueryChainWrapper<MatchingOrder> queryWrapper = matchingOrderService.lambdaQuery().in(MatchingOrder::getStatus, processingStatus.stream().collect(Collectors.toList()))
                .gt(MatchingOrder::getCreateTime, queryStartTime).orderByAsc(MatchingOrder::getId)
                .select(MatchingOrder::getId, MatchingOrder::getStatus, MatchingOrder::getCollectionMemberId, MatchingOrder::getPaymentMemberId, MatchingOrder::getCollectionPlatformOrder, MatchingOrder::getPaymentPlatformOrder);

        Page page = new Page<>();
        page.setSize(1000L);
        long currentPage = 0L;
        int selectCount = 0;
        long syncCount = 0L;
        while (selectCount < 100000) {
            page.setCurrent(++currentPage);
            Page<MatchingOrder> resultPage = matchingOrderService.getBaseMapper().selectPage(page, queryWrapper.getWrapper());
            List<MatchingOrder> resultList = resultPage.getRecords();
            if (resultList.isEmpty()) {
                break;
            }

            for (MatchingOrder matchingOrder : resultList) {
                if (OrderStatusEnum.isBuyingStatus(matchingOrder.getStatus())) {
                    String buyerKey = String.format(MEMBER_PROCESSING_ORDER, matchingOrder.getCollectionMemberId());
                    redisTemplate.opsForSet().add(buyerKey, matchingOrder.getCollectionPlatformOrder());
                    syncCount++;
                }
                if (OrderStatusEnum.isSellingStatus(matchingOrder.getStatus())) {
                    String sellerKey = String.format(MEMBER_PROCESSING_ORDER, matchingOrder.getPaymentMemberId());
                    redisTemplate.opsForSet().add(sellerKey, matchingOrder.getPaymentPlatformOrder());
                    syncCount++;
                }
            }
            selectCount++;
        }

        log.info("同步会员进行中的订单缓存, 查询总次数:{}", selectCount);

        List<MatchPool> matchPoolList = matchPoolService.lambdaQuery().in(MatchPool::getOrderStatus, Arrays.asList(OrderStatusEnum.SELLING_STATUS))
                .gt(MatchPool::getCreateTime, queryStartTime)
                .select(MatchPool::getOrderStatus, MatchPool::getMatchOrder, MatchPool::getMemberId)
                .list();
        if (Collections.isEmpty(matchPoolList)) {
            log.info("同步会员进行中的订单缓存, 匹配池中没有进行中的订单:{}, 本次同步总数量:{}", selectCount, syncCount);
            return;
        }
        for (MatchPool matchPool : matchPoolList) {
            if (OrderStatusEnum.isSellingStatus(matchPool.getOrderStatus())) {
                String sellerKey = String.format(MEMBER_PROCESSING_ORDER, matchPool.getMemberId());
                redisTemplate.opsForSet().add(sellerKey, matchPool.getMatchOrder());
                syncCount++;
            }
        }
        log.info("同步会员进行中的订单缓存, 同步完成, 本次同步总数量:{}", syncCount);

    }

    /**
     * 缓存会员进行中的订单
     *
     * @param matchingOrder
     * @param matchPool
     */
    private void cachedMemberProcessingOrder(MatchingOrder matchingOrder,PaymentOrder paymentOrder, MatchPool matchPool) {
        // 买入中的订单: 待支付、确认中、申诉中
        String buyerKey = String.format(MEMBER_PROCESSING_ORDER, matchingOrder.getCollectionMemberId());
        if (OrderStatusEnum.isBuyingStatus(matchingOrder.getStatus())) {
            log.info("订单状态变化处理, 缓存会员进行中的订单-买入:{}, memberId:{}, status:{}", matchingOrder.getCollectionPlatformOrder(), matchingOrder.getCollectionMemberId(), matchingOrder.getStatus());
            redisTemplate.opsForSet().add(buyerKey, matchingOrder.getCollectionPlatformOrder());
        } else {
            log.info("订单状态变化处理, 移除会员进行中的订单-买入:{}, memberId:{}, status:{}", matchingOrder.getCollectionPlatformOrder(), matchingOrder.getCollectionMemberId(), matchingOrder.getStatus());
            redisTemplate.opsForSet().remove(buyerKey, matchingOrder.getCollectionPlatformOrder());
        }

        // 卖出进行中的订单：匹配中、匹配超时、待支付、确认中、申诉中
        String sellerKey = String.format(MEMBER_PROCESSING_ORDER, matchingOrder.getPaymentMemberId());
        if (OrderStatusEnum.isSellingStatus(paymentOrder.getOrderStatus())) {
            log.info("订单状态变化处理, 缓存会员进行中的订单-卖出:{}, memberId:{}, status:{}", matchingOrder.getPaymentPlatformOrder(), matchingOrder.getPaymentMemberId(), paymentOrder.getOrderStatus());
            redisTemplate.opsForSet().add(sellerKey, matchingOrder.getPaymentPlatformOrder());
        } else {
            log.info("订单状态变化处理, 移除会员进行中的订单-卖出:{}, memberId:{}, status:{}", matchingOrder.getPaymentPlatformOrder(), matchingOrder.getPaymentMemberId(), paymentOrder.getOrderStatus());
            redisTemplate.opsForSet().remove(sellerKey, matchingOrder.getPaymentPlatformOrder());
        }
        if (matchPool == null) {
            return;
        }
        if (OrderStatusEnum.isSellingStatus(matchPool.getOrderStatus())) {
            log.info("订单状态变化处理, 缓存会员进行中的订单-卖出:{}, memberId:{}, status:{}", matchPool.getMatchOrder(), matchPool.getMemberId(), matchPool.getOrderStatus());
            redisTemplate.opsForSet().add(sellerKey, matchPool.getMatchOrder());
        } else {
            log.info("订单状态变化处理, 移除会员进行中的订单-卖出:{}, memberId:{}, status:{}", matchPool.getMatchOrder(), matchPool.getMemberId(), matchPool.getOrderStatus());
            redisTemplate.opsForSet().remove(sellerKey, matchPool.getMatchOrder());
        }

    }

    /**
     * 同步指定会员的进行中订单缓存
     *
     * @param memberId
     */
    @Override
    public void syncMemberProcessingOrderCacheByMember(Long memberId) {
        // 获取进行中的买入订单
        List<BuyProcessingOrderListVo> buyProcessingOrderListVos = buyService.processingBuyOrderList(memberId, Boolean.FALSE);
        Set<String> allOrderNoSet = new HashSet<>();
        if (!CollectionUtils.isEmpty(buyProcessingOrderListVos)) {
            Set<String> buySet = buyProcessingOrderListVos.stream().map(BuyProcessingOrderListVo::getPlatformOrder).collect(Collectors.toSet());
            allOrderNoSet.addAll(buySet);
        }

        // 获取进行中的卖出订单
        List<SellProcessingOrderListVo> sellProcessingOrderListVos = sellService.processingSellOrderList(memberId, Boolean.FALSE);
        if (!CollectionUtils.isEmpty(sellProcessingOrderListVos)) {
            Set<String> sellSet = sellProcessingOrderListVos.stream().map(SellProcessingOrderListVo::getPlatformOrder).collect(Collectors.toSet());
            allOrderNoSet.addAll(sellSet);
        }
        String key = String.format(MEMBER_PROCESSING_ORDER, memberId);
        redisTemplate.delete(key);
        if (!CollectionUtils.isEmpty(allOrderNoSet)) {
            log.info("同步指定会员的进行中订单缓存, memberId:{}, 本次同步的订单:{}", memberId, allOrderNoSet);
            redisTemplate.opsForSet().add(key, allOrderNoSet.toArray());
        } else {
            log.info("同步指定会员的进行中订单缓存, memberId:{}, 本次同步的订单为空", memberId);
        }
    }

}
