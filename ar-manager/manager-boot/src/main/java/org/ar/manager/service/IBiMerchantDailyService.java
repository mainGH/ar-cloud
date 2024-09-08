package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.BiMerchantDailyDTO;
import org.ar.manager.entity.BiMerchantDaily;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantDailyReportReq;

import java.util.List;

/**
 * @author
 */
public interface IBiMerchantDailyService extends IService<BiMerchantDaily> {

    PageReturn<BiMerchantDaily> listPage(MerchantDailyReportReq req);

    PageReturn<BiMerchantDailyDTO> listPageForExport(MerchantDailyReportReq req);
}
