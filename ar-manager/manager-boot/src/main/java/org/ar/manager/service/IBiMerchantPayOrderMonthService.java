package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiMerchantPayOrderExportDTO;
import org.ar.manager.entity.BiMerchantPayOrderMonth;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantMonthReportReq;

/**
* @author 
*/
    public interface IBiMerchantPayOrderMonthService extends IService<BiMerchantPayOrderMonth> {

    PageReturn<BiMerchantPayOrderMonth> listPage(MerchantMonthReportReq req);

    PageReturn<BiMerchantPayOrderExportDTO> listPageForExport(MerchantMonthReportReq req);
}
