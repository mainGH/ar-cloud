package org.ar.pay.service;


import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.PaymentOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.pay.req.PaymentOrderReq;
import org.ar.pay.vo.CollectionOrderInfoVo;
import org.ar.pay.vo.PaymentOrderListVo;

import java.util.List;

/**
* @author 
*/
    public interface IPaymentOrderService extends IService<PaymentOrder> {

     PageReturn<PaymentOrderListVo> listPage(PaymentOrderReq req);

    RestResult<CollectionOrderInfoVo> getPaymentOrderInfoByOrderNo(String merchantOrder);
}
