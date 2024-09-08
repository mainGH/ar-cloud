package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiMerchantDailyDTO;
import org.ar.manager.entity.BiMerchantDaily;
import org.ar.manager.entity.BiMerchantMonth;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantMonthReportReq;

import java.util.List;

/**
 * @author
 */
public interface IBiMerchantMonthService extends IService<BiMerchantMonth> {

    PageReturn<BiMerchantMonth> listPage(MerchantMonthReportReq req);
    PageReturn<BiMerchantDailyDTO> listPageForExport(MerchantMonthReportReq req);
}
