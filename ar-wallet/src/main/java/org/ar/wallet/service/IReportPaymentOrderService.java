package org.ar.wallet.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.PaymentOrderDTO;
import org.ar.common.pay.req.PaymentOrderReq;
import org.ar.wallet.entity.PaymentOrder;


/**
 * @author
 */
public interface IReportPaymentOrderService extends IService<PaymentOrder> {

    PageReturn<PaymentOrderDTO> listDayPage(PaymentOrderReq req);

    PageReturn<PaymentOrderDTO> listMothPage(PaymentOrderReq req);

    PageReturn<PaymentOrderDTO> listDayTotal(PaymentOrderReq req);

    PageReturn<PaymentOrderDTO> listMothTotal(PaymentOrderReq req);


}
