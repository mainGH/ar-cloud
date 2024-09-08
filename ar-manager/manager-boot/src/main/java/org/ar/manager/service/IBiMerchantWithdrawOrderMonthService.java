package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiMerchantWithdrawOrderDailyDTO;
import org.ar.manager.entity.BiMerchantWithdrawOrderMonth;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantMonthReportReq;

/**
 * @author
 */
public interface IBiMerchantWithdrawOrderMonthService extends IService<BiMerchantWithdrawOrderMonth> {

    PageReturn<BiMerchantWithdrawOrderMonth> listPage(MerchantMonthReportReq req);

    PageReturn<BiMerchantWithdrawOrderDailyDTO> listPageForExport(MerchantMonthReportReq req);
}
