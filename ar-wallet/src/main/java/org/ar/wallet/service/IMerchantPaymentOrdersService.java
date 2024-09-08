package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.wallet.entity.MerchantPaymentOrders;

/**
 * <p>
 * 商户代付订单表 服务类
 * </p>
 *
 * @author 
 * @since 2024-01-05
 */
public interface IMerchantPaymentOrdersService extends IService<MerchantPaymentOrders> {


    /**
     * 根据商户订单号 获取订单信息
     *
     * @param merchantOrder
     * @return {@link MerchantPaymentOrders}
     */
    MerchantPaymentOrders getOrderInfoByOrderNumber(String merchantOrder);
}
