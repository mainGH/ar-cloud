package org.ar.wallet.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberOrderOverviewDTO;
import org.ar.common.pay.dto.PaymentOrderExportDTO;
import org.ar.common.pay.dto.PaymentOrderListPageDTO;
import org.ar.common.pay.req.CommonDateLimitReq;
import org.ar.common.pay.req.PaymentOrderIdReq;
import org.ar.common.pay.req.PaymentOrderListPageReq;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.entity.PaymentOrder;
import org.ar.wallet.req.BuyListReq;
import org.ar.wallet.req.SellOrderListReq;
import org.ar.wallet.vo.CollectionOrderInfoVo;
import org.ar.wallet.vo.SellOrderListVo;
import org.ar.wallet.vo.ViewSellOrderDetailsVo;

import java.util.List;

/**
 * @author
 */
public interface IPaymentOrderService extends IService<PaymentOrder> {

    PageReturn<PaymentOrderListPageDTO> listPage(PaymentOrderListPageReq req);
    PageReturn<PaymentOrderExportDTO> listPageExport(PaymentOrderListPageReq req);


    PageReturn<PaymentOrderListPageDTO> listRecordPage(PaymentOrderListPageReq req);

    PaymentOrderListPageDTO listRecordTotalPage(PaymentOrderListPageReq req);

    RestResult<CollectionOrderInfoVo> getPaymentOrderInfoByOrderNo(String merchantOrder);

    /*
     * 更新代付订单状态为: 确认中
     * */
    boolean updateOrderStatusToConfirmation(String merchantOrder);

    /*
     * 查询所有代付未成功和未匹配的订单
     * */
    List<PaymentOrder> getPaymentOrderBySatus();

    /*
     * 更新代付订单状态为待支付
     * */
    boolean updateOrderStatusBePaid(String merchantOrder);

    /**
     * 获取最老的卖出订单列表
     *
     * @param buyListReq
     * @param memberId
     * @param pagePaymentOrder
     * @return {@link PageReturn}<{@link PaymentOrder}>
     */
    PageReturn<PaymentOrder> getOldestOrders(BuyListReq buyListReq, String memberId, Page<PaymentOrder> pagePaymentOrder);


    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo
     * @return {@link PaymentOrder}
     */
    PaymentOrder getPaymentOrderByOrderNo(String orderNo);

    /**
     * 查询卖出订单列表
     *
     * @param req
     * @return {@link List}<{@link SellOrderListVo}>
     */
    List<SellOrderListVo> sellOrderList(SellOrderListReq req);

    /**
     * 查看卖出订单详情
     *
     * @param platformOrder
     * @return {@link ViewSellOrderDetailsVo}
     */
    ViewSellOrderDetailsVo viewSellOrderDetails(String platformOrder);

    /**
     * 根据会员id 获取正在进行中的卖出订单
     *
     * @param memberId
     * @return {@link List}<{@link SellOrderListVo}>
     */
    List<PaymentOrder> ongoingSellOrders(String memberId);

    /**
     * 获取在进行中的订单列表
     * @param memberId
     * @return
     */
    List<PaymentOrder> getProcessingOrderByMemberId(String memberId);

    /**
     * 根据订单号获取订单信息
     * @param platformOrderList
     * @return
     */
    List<PaymentOrder> getOrderListByPlatformOrderList(List<String> platformOrderList);


    /**
     * 根据收款id获取正在匹配中的订单
     *
     * @param collectionInfoId
     * @return {@link Integer}
     */
    Integer getMatchingOrdersBycollectionId(Long collectionInfoId);


    /**
     * 获取卖出订单列表
     *
     * @param sellOrderListReq
     * @param memberInfo
     * @return {@link List}<{@link SellOrderListVo}>
     */
    List<SellOrderListVo> getPaymentOrderOrderList(SellOrderListReq sellOrderListReq, MemberInfo memberInfo);


    /**
     * 根据匹配订单号获取卖出订单列表
     *
     * @param matchOrder
     * @return {@link List}<{@link SellOrderListVo}>
     */
    List<SellOrderListVo> getPaymentOrderListByMatchOrder(String matchOrder);


    /**
     * 根据匹配订单号获取卖出订单列表
     *
     * @param matchOrder
     * @return {@link List}<{@link PaymentOrder}>
     */
    List<PaymentOrder> getPaymentOrderByByMatchOrder(String matchOrder);

    Boolean manualCallback(Long id, String opName);


    /**
     * 查看该母订单是否已结束(查看母订单状态和该母订单下的子订单是否有未结束的订单)
     *
     * @param matchOrder
     * @return {@link Boolean}
     */
    Boolean existsActiveSubOrders(String matchOrder);

    /**
     * 获取usdt概览相关数据
     * @return
     */
    MemberOrderOverviewDTO getUsdtData(CommonDateLimitReq req);


    /**
     * 根据IP获取卖出订单
     *
     * @param ip
     * @return
     */
    List<PaymentOrder> getPaymentOrderByByIp(String ip);

    /**
     * 标记订单为指定的tag
     *
     * @param riskTag
     * @param platformOrders
     */
    void taggingOrders(String riskTag, List<String> platformOrders);

}
