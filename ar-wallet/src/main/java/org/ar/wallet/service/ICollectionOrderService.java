package org.ar.wallet.service;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.CollectionOrderDTO;
import org.ar.common.pay.dto.CollectionOrderExportDTO;
import org.ar.common.pay.req.CollectionOrderIdReq;
import org.ar.common.pay.req.CollectionOrderListPageReq;
import org.ar.wallet.entity.CollectionOrder;
import org.ar.wallet.req.BuyOrderListReq;
import org.ar.wallet.req.ProcessingOrderListReq;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.vo.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author
 */
public interface ICollectionOrderService extends IService<CollectionOrder> {


    /**
     * 查询买入订单列表
     *
     * @param req
     * @return {@link RestResult}<{@link List}<{@link BuyOrderListVo}>>
     */
    RestResult<PageReturn<BuyOrderListVo>> buyOrderList(BuyOrderListReq req);

    /**
     * 进行中的订单
     * @param memberId memberId
     * @return
     */
    List<CollectionOrder> processingBuyOrderList(Long memberId);

    List<CollectionOrder> processingBuyOrderList(List<String> platformOrderList);

    /**
     * 查看买入订单详情
     *
     * @param platformOrder
     * @return {@link ViewBuyOrderDetailsVo}
     */
    ViewBuyOrderDetailsVo viewBuyOrderDetails(String platformOrder);

    PageReturn<CollectionOrderDTO> listRecordPage(CollectionOrderListPageReq req);

    PageReturn<CollectionOrderDTO> listPage(CollectionOrderListPageReq req);

    PageReturn<CollectionOrderExportDTO> listPageExport(CollectionOrderListPageReq req);

    CollectionOrderDTO listPageRecordTotal(CollectionOrderListPageReq req);

    CollectionOrderDTO pay(CollectionOrderIdReq req);

    boolean updateOrderByOrderNo(String merchantNo, String orderId, String realAmount, String payType);

    /*
     * 手动回调
     * */
    RestResult manualCallback(String merchantOrder);

    Boolean manualCallback(Long id, String opName);

    /*
     * 查询代收订单详情
     * */
    RestResult<CollectionOrderInfoVo> getCollectionOrderInfoByOrderNo(String merchantOrder);

    /*
     * 查询下拉列表数据(币种,支付类型)
     * */
    RestResult selectList();

    /*
     * 根据id更改订单已发送状态
     * */
    int updateOrderSendById(String id);

    /*
     * 更新支付订单状态为: 确认中
     * */
    boolean updateOrderStatusToConfirmation(String merchantOrder);

    /**
     * 根据支付订单匹配代付订单
     */
    JSONObject matchWithdrawOrder(CollectionOrder collectionOrder);

    /*
     * 查询最接近给定数字的前10个元素
     * p1 代付池金额列表
     * p2 充值金额
     * p3 列表推荐个数
     * */
    List<Map.Entry<String, Integer>> findClosestValues(Map<String, Integer> map, int collectionAmount, int count);

    /**
     * 更新充值订单状态为待支付并设置实际金额(匹配成功调用)
     */
    boolean updateCollectionOrderStatusToBePaid(String merchantOrder, BigDecimal actualAmount);


    /**
     * 根据订单号获取买入订单
     *
     * @param platformOrder
     * @return {@link CollectionOrder}
     */
    CollectionOrder getCollectionOrderByPlatformOrder(String platformOrder);


    /**
     * 根据会员id 查看进行中的订单数量
     *
     * @param memberId
     */
    CollectionOrder countActiveBuyOrders(String memberId);

    /**
     * 根据会员id 获取待支付和支付超时的买入订单
     *
     * @param memberId
     * @return {@link CollectionOrder}
     */
    CollectionOrder getPendingBuyOrder(String memberId);

    /**
     * 获取买入订单详情
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link BuyOrderDetailsVo}>
     */
    RestResult<BuyOrderDetailsVo> getBuyOrderDetails(PlatformOrderReq platformOrderReq);

    /**
     * 根据IP获取买入订单
     *
     * @param ip
     * @return
     */
    List<CollectionOrder> getCollectOrderByByIp(String ip);

    /**
     * 标记订单为指定的tag
     *
     * @param riskTag
     * @param platformOrders
     */
    void taggingOrders(String riskTag, List<String> platformOrders);
}
