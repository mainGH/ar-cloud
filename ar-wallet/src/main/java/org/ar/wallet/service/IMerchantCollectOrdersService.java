package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.wallet.entity.MerchantCollectOrders;

/**
 * <p>
 * 商户代收订单表 服务类
 * </p>
 *
 * @author
 * @since 2024-01-05
 */
public interface IMerchantCollectOrdersService extends IService<MerchantCollectOrders> {


    /**
     * 根据商户订单号 获取订单信息
     *
     * @return {@link MerchantCollectOrders}
     */
    MerchantCollectOrders getOrderInfoByOrderNumber(String merchantOrder);


    /**
     * 取消充值订单
     *
     * @param platformOrder 平台订单号
     * @return {@link Boolean}
     */
    Boolean cancelPayment(String platformOrder);

    /**
     * 支付超时处理
     *
     * @param orderNo
     * @return boolean
     */
    boolean handlePaymentTimeout(String orderNo);
}
