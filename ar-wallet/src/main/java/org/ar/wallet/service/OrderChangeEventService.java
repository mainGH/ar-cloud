package org.ar.wallet.service;

import org.ar.wallet.entity.NotifyOrderStatusChangeMessage;

public interface OrderChangeEventService {

    /**
     * 处理订单状所有状态变化的事件("卖出"除外)
     *
     * @param orderStatusChangeMessage
     */
    void process(NotifyOrderStatusChangeMessage orderStatusChangeMessage);

    /**
     * 卖出订单处理
     *
     * @param orderStatusChangeMessage
     */
    void processSellOrder(NotifyOrderStatusChangeMessage orderStatusChangeMessage);

    /**
     * 处理取消卖出订单(无撮合订单)
     *
     * @param orderStatusChangeMsg
     */
    void processCancelSellOrder(NotifyOrderStatusChangeMessage orderStatusChangeMsg);

    /**
     * 同步会员进行中的订单缓存
     */
    void syncMemberProcessingOrderCache();

    /**
     * 同步指定会员的进行中订单缓存
     *
     * @param memberId
     */
    void syncMemberProcessingOrderCacheByMember(Long memberId);

}
