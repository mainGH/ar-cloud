package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.entity.*;
import org.ar.wallet.req.*;
import org.ar.wallet.vo.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IBuyService {

    /**
     * 获取买入金额列表
     *
     * @param buyListReq
     * @return {@link List}<{@link BuyListVo}>
     */
    PageReturn<BuyListVo> getBuyList(BuyListReq buyListReq, String memberId);

    /**
     * 买入处理
     *
     * @param buyReq
     * @return {@link Boolean}
     */
    RestResult buyProcessor(BuyReq buyReq, HttpServletRequest request);

    /**
     * 买入订单校验
     *
     * @param buyReq
     * @param memberInfo
     * @param tradeConfig
     * @return {@link RestResult}
     */
    RestResult orderValidation(BuyReq buyReq, MemberInfo memberInfo, TradeConfig tradeConfig);

    /**
     * 生成买入订单
     *
     * @param buyReq
     * @param buyMemberInfo
     * @param buyplatformOrder
     * @param matchingPlatformOrder
     * @return {@link Boolean}
     */
    Boolean createBuyOrder(BuyReq buyReq, MemberInfo buyMemberInfo, String buyplatformOrder, String matchingPlatformOrder, CollectionInfo collectionInfo, String realIP);

    /**
     * 生成卖出订单-拆单
     *
     * @param buyReq
     * @param sellMemberInfo
     * @param matchPool
     * @param tradeConfig
     * @param sellplatformOrder
     * @return {@link Boolean}
     */
    Boolean createSellOrderSplit(BuyReq buyReq, MemberInfo sellMemberInfo, MatchPool matchPool, TradeConfig tradeConfig, String sellplatformOrder, String matchingPlatformOrder, CollectionInfo collectionInfo, String matchPoolOrderClientIp);

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
    Boolean createMatchedOrder(BuyReq buyReq, String buyplatformOrder, String sellplatformOrder, String matchingPlatformOrder, CollectionInfo collectionInfo, MemberInfo buyMemberInfo, MemberInfo sellMemberInfo);

    /**
     * 更新匹配池订单数据
     *
     * @param buyReq
     * @param matchPoolOrder
     * @return {@link Boolean}
     */
    Boolean updateMatchingPoolOrderData(BuyReq buyReq, MatchPool matchPoolOrder);

    /**
     * 更新卖出订单数据
     *
     * @param buyReq
     * @param paymentOrder
     * @param matchingPlatformOrder
     * @return {@link Boolean}
     */
    Boolean updateSellOrder(BuyReq buyReq, PaymentOrder paymentOrder, String matchingPlatformOrder);


    /**
     * USDT买入处理
     *
     * @param usdtBuyReq
     * @return {@link RestResult}
     */
    RestResult usdtBuyProcessor(UsdtBuyReq usdtBuyReq);


    /**
     * USDT买入订单校验
     *
     * @param usdtBuyReq
     * @param usdtBuyMemberInfo
     * @param tradeConfig
     * @return {@link RestResult}
     */
    RestResult usdtOrderValidation(UsdtBuyReq usdtBuyReq, MemberInfo usdtBuyMemberInfo, TradeConfig tradeConfig);


    /**
     * 生成USDT买入订单
     *
     * @param usdtBuyReq
     * @param usdtBuyMemberInfo
     * @param usdtInfo
     * @param platformOrder
     * @return {@link Boolean}
     */
    Boolean createUsdtOrder(UsdtBuyReq usdtBuyReq, MemberInfo usdtBuyMemberInfo, UsdtConfig usdtInfo, String platformOrder);


    /**
     * 取消买入订单处理
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    RestResult cancelPurchaseOrder(CancelOrderReq cancelOrderReq);


    /**
     * 取消买入订单处理
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    RestResult cancelPurchaseOrder(CancelOrderReq cancelOrderReq,
                                   OrderStatusEnum buyStatus,
                                   OrderStatusEnum sellStatus,
                                   OrderStatusEnum matchStatus,
                                   MemberInfo memberInfo);


//    /**
//     * 提交UTR处理
//     *
//     * @param submitUtrReq
//     * @return {@link RestResult}
//     */
//    RestResult submitUtr(SubmitUtrReq submitUtrReq);


    /**
     * 完成支付 处理
     *
     * @param platformOrder
     * @param voucherImage
     * @param utr
     * @return {@link RestResult}<{@link List}<{@link BuyListVo}>>
     */
    RestResult<List<BuyListVo>> buyCompletedProcessor(String platformOrder, String voucherImage, String utr);


    /**
     * 买入订单申诉处理
     *
     * @param platformOrder
     * @param appealReason
     * @param images
     * @param video
     * @return {@link RestResult}
     */
    RestResult buyOrderAppealProcess(String platformOrder, String appealReason, List<String> images, String video);

    /**
     * 取消支付
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    RestResult cancelPayment(CancelOrderReq cancelOrderReq);

    /**
     * 获取支付页面数据
     *
     * @return {@link RestResult}<{@link BuyVo}>
     */
    RestResult<BuyVo> getPaymentPageData();

    /**
     * 获取USDT支付页面数据
     *
     * @return {@link RestResult}<{@link UsdtBuyVo}>
     */
    RestResult<UsdtBuyVo> getUsdtPaymentPageData();

    /**
     * 获取取消买入页面数据
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    RestResult getCancelBuyPageData(PlatformOrderReq platformOrderReq);

    /**
     * 获取支付类型
     *
     * @return {@link RestResult}<{@link List}<{@link PaymentTypeVo}>>
     */
    RestResult<List<PaymentTypeVo>> getPaymentType();

    List<BuyProcessingOrderListVo> processingBuyOrderList(Long memberId, boolean getFromRedis);

}
