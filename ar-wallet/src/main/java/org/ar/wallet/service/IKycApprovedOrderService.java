package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.KycApprovedOrderDTO;
import org.ar.common.pay.req.KycApprovedOrderListPageReq;
import org.ar.wallet.entity.KycApprovedOrder;

/**
 * <p>
 * 通过 KYC 验证完成的订单表 服务类
 * </p>
 *
 * @author
 * @since 2024-05-03
 */
public interface IKycApprovedOrderService extends IService<KycApprovedOrder> {

    /**
     * 根据卖出订单号查询KYC交易订单是否存在
     *
     * @param sellerOrderId
     * @return boolean 如果 KYC交易订单存在，返回true；否则返回false。
     */
    Boolean checkKycTransactionExistsBySellOrderId(String sellerOrderId);

    /**
     * 完成交易订单列表
     * @param req
     * @return
     */
    PageReturn<KycApprovedOrderDTO>  listPage(KycApprovedOrderListPageReq req);
}
