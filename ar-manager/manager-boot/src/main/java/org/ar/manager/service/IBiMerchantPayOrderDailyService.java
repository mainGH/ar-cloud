package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiMerchantPayOrderExportDTO;
import org.ar.common.pay.dto.MerchantOrderOverviewDTO;
import org.ar.manager.entity.BiMerchantPayOrderDaily;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.req.MerchantMonthReportReq;

/**
 * @author
 */
public interface IBiMerchantPayOrderDailyService extends IService<BiMerchantPayOrderDaily> {
    PageReturn<BiMerchantPayOrderDaily> listPage(MerchantMonthReportReq req);

    PageReturn<BiMerchantPayOrderExportDTO> listPageForExport(MerchantMonthReportReq req);

    MerchantOrderOverviewDTO getMerchantOrderOverview(MerchantDailyReportReq req);
}
