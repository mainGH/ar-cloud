package org.ar.wallet.service;

import org.ar.wallet.entity.CashBackOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 退回订单表 服务类
 * </p>
 *
 * @author 
 * @since 2024-05-09
 */
public interface ICashBackOrderService extends IService<CashBackOrder> {
    CashBackOrder getCashBackOrder(String orderNo);
}
