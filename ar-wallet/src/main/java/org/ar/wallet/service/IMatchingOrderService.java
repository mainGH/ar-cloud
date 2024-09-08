package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MatchingOrderDTO;
import org.ar.common.pay.dto.MatchingOrderExportDTO;
import org.ar.common.pay.dto.MatchingOrderPageListDTO;
import org.ar.common.pay.dto.RelationOrderDTO;
import org.ar.common.pay.req.*;
import org.ar.wallet.entity.MatchingOrder;
import org.ar.wallet.entity.MemberInfo;

import java.util.List;
import java.util.Map;

/**
 * @author
 */
public interface IMatchingOrderService extends IService<MatchingOrder> {

    /*
     * 更新匹配订单 支付状态为: 确认中 更新订单支付时间
     * */
    boolean updateCollectionOrderStatusToConfirmation(String collectionOrder);

    /*
     * 更新匹配订单代付状态为: 确认中
     * */
    boolean updatePaymentOrderStatusToConfirmation(String paymentOrder);

    /*
     * 查询10秒前匹配成功并且未发送MQ的订单
     * */
    List<MatchingOrder> getMatchSuccessAndUnsent();

    /*
     * 根据代付订单号更新订单的匹配发送状态
     * */
    boolean updateOrderMatchSendByOrder(String paymentOrder);

    /*
     * 更新充值交易回调MQ发送状态
     * */
    boolean updateCollectionTradeSend(String collectionOrder);

    /*
     * 更新提现交易回调MQ发送状态
     * */
    boolean updatePaymentTradeSend(String paymentOrder);

    /*
     * 更新匹配回调成功的状态
     * */
    boolean updateMatchSuccess(String paymentOrder);

    /*
     * 更新匹配回调失败的状态
     * */
    boolean updateMatchFailed(String paymentOrder);

    /*
     * 更新充值交易回调成功
     * */
    boolean updateTradeCollectionSuccess(String collectionOrder);

    /*
     * 更新充值交易回调失败
     * */
    boolean updateTradeCollectionFailed(String collectionOrder);

    /*
     * 更新提现交易回调成功
     * */
    boolean updateTradePaymentSuccess(String paymentOrder);

    /*
     * 更新提现交易回调失败
     * */
    boolean updateTradePaymentFailed(String paymentOrder);

    /*
     * 查询10秒前所有交易成功并且未发送MQ的订单
     * */
    List<MatchingOrder> getTradeSuccessAndUnsent();

    /**
     * 根据撮合列表订单号获取撮合列表订单
     */
    MatchingOrder getMatchingOrder(String matchingPlatformOrder);

    /**
     * 根据支付订单号获取匹配订单
     */
    MatchingOrder getMatchingOrderByCollection(String collectionOrder);

    /**
     * 根据提现订单号获取匹配订单
     */
    MatchingOrder getPaymentMatchingOrder(String paymentOrder);


    PageReturn<MatchingOrderPageListDTO> listPage(MatchingOrderReq req);
    PageReturn<MatchingOrderExportDTO> listPageExport(MatchingOrderReq req);


    MatchingOrderDTO getInfo(MatchingOrderIdReq req);

    //AppealOrderDTO appealDetail(MatchingOrderReq req);


    MatchingOrderDTO update(MatchingOrderReq req);


    MatchingOrderDTO getMatchingOrderTotal(MatchingOrderReq req);


    MatchingOrderDTO nopay(MatchingOrderAppealReq req);

    Map<String, String> getMatchMemberIdByPlatOrderIdList(List<String> platOrderIdList, boolean isBuy);

    /**
     * 根据买入订单、卖出订单批量汇总查询关联的撮合订单ID列表
     *
     * @param buyOrderIds
     * @param sellOrderIds
     * @return
     */
    Map<String, String> getMatchOrderIdsByPlatOrderId(List<String> buyOrderIds, List<String> sellOrderIds);

    /**
     * 标记订单为指定的tag
     *
     * @param riskTag
     * @param platformOrderTags
     */
    void taggingOrders(String riskTag, Map<String, String> platformOrderTags);

    Page<RelationOrderDTO> relationOrderList(RelationshipOrderReq req);


    boolean manualReview(MatchingOrderManualReq req, ISellService sellService);

    /**
     * 取消会员确认超时的订单
     */
    void cancelConfirmTimeoutOrder(Integer startDays);
}
