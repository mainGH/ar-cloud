package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.BiPaymentOrderDTO;
import org.ar.common.pay.dto.BiPaymentOrderExportDTO;
import org.ar.common.pay.dto.MemberOrderOverviewDTO;
import org.ar.common.pay.dto.OrderStatusOverviewDTO;
import org.ar.common.pay.req.CommonDateLimitReq;
import org.ar.common.pay.req.MemberInfoIdReq;
import org.ar.manager.entity.BiPaymentOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.PaymentOrderReportReq;

import java.util.List;

/**
 * @author
 */
public interface IBiPaymentOrderService extends IService<BiPaymentOrder> {

    /**
     * 查询代收日报表记录
     * @param req
     * @return
     */
    PageReturn<BiPaymentOrder> listPage(PaymentOrderReportReq req);


    RestResult<MemberOrderOverviewDTO> getMemberOrderOverview(CommonDateLimitReq req, RestResult<MemberOrderOverviewDTO> usdtData);
    PageReturn<BiPaymentOrderExportDTO> listPageForExport(PaymentOrderReportReq req);

    BiPaymentOrder getPaymentOrderStatusOverview(CommonDateLimitReq req);
}
