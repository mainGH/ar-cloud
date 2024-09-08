package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiPaymentOrderExportDTO;
import org.ar.manager.entity.BiPaymentOrderMonth;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.PaymentMonthOrderReportReq;

import java.util.List;

/**
 * @author
 */
public interface IBiPaymentOrderMonthService extends IService<BiPaymentOrderMonth> {

    PageReturn<BiPaymentOrderMonth> listPage(PaymentMonthOrderReportReq req);
    PageReturn<BiPaymentOrderExportDTO> listPageForExport(PaymentMonthOrderReportReq req);
}
