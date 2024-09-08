package org.ar.wallet.service;

import io.vertx.core.json.JsonObject;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.wallet.entity.*;
import org.ar.wallet.req.CancelOrderReq;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.req.SellOrderListReq;
import org.ar.wallet.req.SellReq;
import org.ar.wallet.vo.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;

public interface ISellService {


    /**
     * 订单校验
     *
     * @param sellReq
     * @param memberInfo
     * @param tradeConfig
     * @param collectionInfo
     * @return {@link RestResult}
     */
    RestResult orderValidation(SellReq sellReq, MemberInfo memberInfo, TradeConfig tradeConfig, CollectionInfo collectionInfo);


    /**
     * 卖出处理
     *
     * @param sellReq
     * @return {@link Boolean}
     */
    RestResult<SellOrderVo> sellProcessor(SellReq sellReq, HttpServletRequest request);


    /**
     * 根据会员ID 获取当前正在进行中的卖出订单(不去重)
     *
     * @param memberId
     * @return {@link List}<{@link SellOrderListVo}>
     */
    List<SellOrderListVo> getOngoingSellOrdersByMemberId(String memberId);

    List<SellProcessingOrderListVo> processingSellOrderList(Long memberId, boolean getFromRedis);


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
    Boolean createSellOrder(SellReq sellReq, MemberInfo memberInfo, String platformOrder, TradeConfigScheme schemeConfigByMemberTag, CollectionInfo collectionInfo, Long lastUpdateTimestamp, String realIP);


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
    Boolean createMatchPoolOrder(SellReq sellReq, MemberInfo memberInfo, String platformOrder, CollectionInfo collectionInfo, Long lastUpdateTimestamp, TradeConfigScheme schemeConfigByMemberTag, String realIP);


    /**
     * 交易成功处理 (钱包用户)
     *
     * @param sellPlatformOrder
     * @return {@link RestResult}<{@link BuyCompletedVo}>
     */
    RestResult<BuyCompletedVo> transactionSuccessHandler(String sellPlatformOrder, Long memberId, PaymentOrder paymentOrder, CollectionOrder colOrder, String type, String paymentPassword);


    /**
     * 取消卖出订单处理
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    RestResult cancelSellOrder(CancelOrderReq cancelOrderReq);

    Boolean updateBuyMemberInfo(MemberInfo buyMemberInfo, CollectionOrder collectionOrder);


    /**
     * 继续匹配
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    RestResult continueMatching(PlatformOrderReq platformOrderReq);

    /**
     * 获取接口页面数据
     *
     * @return {@link RestResult}<{@link SellListVo}>
     */
    RestResult<SellListVo> fetchPageData();

    /**
     * 提交金额错误处理
     *
     * @param platformOrder
     * @param images
     * @param video
     * @param orderActualAmount
     * @return {@link RestResult}
     */
    RestResult processAmountError(String platformOrder, List<String> images, String video, BigDecimal orderActualAmount);

    /**
     * 文件处理
     *
     * @param images
     * @param video
     * @return {@link RestResult}
     */
    JsonObject saveFile(MultipartFile[] images, MultipartFile video);

    /**
     * 获取卖出订单列表
     *
     * @param sellOrderListReq
     * @return {@link RestResult}<{@link PageReturn}<{@link SellOrderListVo}>>
     */
    RestResult<PageReturn<SellOrderListVo>> sellOrderList(SellOrderListReq sellOrderListReq);

    /**
     * 匹配中(拆单) 页面数据处理
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link MatchPoolSplittingVo}>
     */
//    RestResult<MatchPoolSplittingVo> matchPoolSplitting(PlatformOrderReq platformOrderReq);

    /**
     * 卖出订单申诉处理
     *
     * @param platformOrder
     * @param appealReason
     * @param images
     * @param video
     * @return {@link RestResult}
     */
    RestResult sellOrderAppealProcess(String platformOrder, String appealReason, List<String> images, String video);

    /**
     * 获取取消卖出页面数据
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link CancelSellPageDataVo}>
     */
    RestResult<CancelSellPageDataVo> getCancelSellPageData(PlatformOrderReq platformOrderReq);

    /**
     * 查看卖出订单详情
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link MatchPoolSplittingVo}>
     */
    RestResult<SellOrderDetailsVo> getSellOrderDetails(PlatformOrderReq platformOrderReq);


    /**
     * 查询匹配池订单下面的子订单 并根据子订单状态 更新匹配池订单状态
     *
     * @param matchOrder
     */
    void updateMatchPoolOrderStatus(String matchOrder);

    /**
     * 取消申请 金额错误
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    RestResult cancelApplication(PlatformOrderReq platformOrderReq);


    /**
     * 取消匹配
     *
     * @param platformOrderReq
     * @return {@link RestResult}
     */
    RestResult cancelMatching(PlatformOrderReq platformOrderReq);
}
