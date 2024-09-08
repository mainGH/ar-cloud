package org.ar.wallet.service;


/**
 * 处理超时订单
 *
 * @author Simon
 * @date 2023/12/01
 */
public interface HandleOrderTimeoutService {


    /**
     * 钱包用户确认超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    Boolean handleWalletUserConfirmationTimeout(String platformOrder);


    /**
     * 商户会员确认超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    Boolean handleMerchantMemberConfirmationTimeout(String platformOrder);


    /**
     * 钱包用户卖出匹配超时处理
     *
     * @param platformOrder
     * @param lastUpdateTimestamp
     * @return {@link Boolean}
     */
    Boolean handleWalletUserSaleMatchTimeout(String platformOrder, Long lastUpdateTimestamp);


    /**
     * 商户会员卖出匹配超时处理
     *
     * @param platformOrder
     * @param lastUpdateTimestamp
     * @return {@link Boolean}
     */
    Boolean handleMerchantMemberSaleMatchTimeout(String platformOrder, Long lastUpdateTimestamp);


    /**
     * 支付超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    Boolean handlePaymentTimeout(String platformOrder);


    /**
     * USDT支付超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    Boolean handleUsdtPaymentTimeout(String platformOrder);


    /**
     * 匹配超时时自动取消订单
     *
     * @param taskInfo
     * @return {@link Boolean}
     */
    Boolean autoCancelOrderOnMatchTimeout(String taskInfo);

    /**
     * 会员确认超时风控标记订单
     *
     * @param taskInfo
     * @return {@link Boolean}
     */
    Boolean taggingOrderOnMemberConfirmTimeout(String taskInfo);

    /**
     * 会员确认超时自动取消订单
     * @param taskInfo
     * @return
     */
    Boolean autoCancelOrderOnMemberConfirmTimeout(String taskInfo);

}
