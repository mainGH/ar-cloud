package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UsdtBuyOrderDTO;
import org.ar.common.pay.req.UsdtBuyOrderReq;
import org.ar.wallet.entity.UsdtBuyOrder;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.vo.UsdtBuyOrderVo;
import org.ar.wallet.vo.UsdtBuyPageDataVo;
import org.ar.wallet.vo.UsdtPurchaseOrderDetailsVo;

import java.util.List;

/**
 * @author
 */
public interface IUsdtBuyOrderService extends IService<UsdtBuyOrder> {

    /**
     * 根据会员id 查询usdt买入记录
     *
     * @param memberId
     * @return {@link PageReturn}<{@link UsdtBuyOrderVo}>
     */
    List<UsdtBuyOrderVo> findPagedUsdtPurchaseRecords(String memberId);

    /**
     * 查询全部USDT买入记录
     *
     * @param pageRequestHome
     * @return {@link List}<{@link UsdtBuyOrderVo}>
     */
    RestResult<PageReturn<UsdtBuyOrderVo>> findAllUsdtPurchaseRecords(PageRequestHome pageRequestHome);


    PageReturn<UsdtBuyOrderDTO> listPage(UsdtBuyOrderReq req);

    /**
     * 根据订单号获取USDT买入订单
     *
     * @param platformOrder
     * @return {@link UsdtBuyOrder}
     */
    UsdtBuyOrder getUsdtBuyOrderByPlatformOrder(String platformOrder);

    /**
     * 获取USDT买入页面数据
     *
     * @return {@link RestResult}<{@link UsdtBuyPageDataVo}>
     */
    RestResult<UsdtBuyPageDataVo> getUsdtBuyPageData();


    /**
     * USDT完成转账处理
     *
     * @param platformOrder
     * @param voucherImage
     * @return {@link RestResult}
     */
    RestResult usdtBuyCompleted(String platformOrder, String voucherImage);


    /**
     * 根据会员id 查看进行中的USDT订单数量
     *
     * @param memberId
     */
    UsdtBuyOrder countActiveUsdtBuyOrders(String memberId);

    /**
     * 获取会员待支付的USDT买入订单
     *
     * @param memberId
     * @return {@link UsdtBuyOrder}
     */
    UsdtBuyOrder getPendingUsdtBuyOrder(Long memberId);


    /**
     * 获取USDT买入订单详情
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link UsdtPurchaseOrderDetailsVo}>
     */
    RestResult<UsdtPurchaseOrderDetailsVo> getUsdtPurchaseOrderDetails(PlatformOrderReq platformOrderReq);
}
