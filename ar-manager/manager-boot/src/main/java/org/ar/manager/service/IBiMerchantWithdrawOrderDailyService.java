package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiMerchantWithdrawOrderDailyDTO;
import org.ar.common.pay.dto.MerchantOrderOverviewDTO;
import org.ar.manager.entity.BiMerchantWithdrawOrderDaily;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.req.MerchantMonthReportReq;
import org.ar.manager.req.WithdrawDailyOrderReportReq;

/**
 * @author
 */
public interface IBiMerchantWithdrawOrderDailyService extends IService<BiMerchantWithdrawOrderDaily> {

    PageReturn<BiMerchantWithdrawOrderDaily> listPage(MerchantMonthReportReq req);

    MerchantOrderOverviewDTO getMerchantOrderOverview(MerchantDailyReportReq req);

    PageReturn<BiMerchantWithdrawOrderDailyDTO> listPageForExport(MerchantMonthReportReq req);
}
