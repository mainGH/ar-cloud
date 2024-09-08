package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.CommonDateLimitReq;
import org.ar.manager.entity.BiWithdrawOrderDaily;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.WithdrawDailyOrderReportReq;

import java.util.List;

/**
 * @author
 */
public interface IBiWithdrawOrderDailyService extends IService<BiWithdrawOrderDaily> {

    PageReturn<BiWithdrawOrderDaily> listPage(WithdrawDailyOrderReportReq req);


    MemberOrderOverviewDTO getMemberOrderOverview(CommonDateLimitReq req);

    PageReturn<BiWithdrawOrderDailyExportDTO> listPageForExport(WithdrawDailyOrderReportReq req);

    BiWithdrawOrderDaily getWithdrawOrderStatusOverview(CommonDateLimitReq req);
}
