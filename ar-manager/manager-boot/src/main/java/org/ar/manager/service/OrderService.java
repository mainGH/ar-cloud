package org.ar.manager.service;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.OrderStatusOverviewListDTO;
import org.ar.common.pay.req.CommonDateLimitReq;

/**
 * @author admin
 * @date 2024/3/15 14:14
 */
public interface OrderService {
    RestResult<OrderStatusOverviewListDTO> getOrderStatusOverview(CommonDateLimitReq req);
}
