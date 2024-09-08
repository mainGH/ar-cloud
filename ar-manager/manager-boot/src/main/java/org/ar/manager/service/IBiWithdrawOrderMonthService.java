package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiWithdrawOrderDailyExportDTO;
import org.ar.manager.entity.BiWithdrawOrderMonth;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.WithdrawDailyOrderReportReq;
import org.ar.manager.req.WithdrawMonthOrderReportReq;

import java.util.List;

/**
* @author 
*/
    public interface IBiWithdrawOrderMonthService extends IService<BiWithdrawOrderMonth> {

    PageReturn<BiWithdrawOrderMonth> listPage(WithdrawMonthOrderReportReq req);
    PageReturn<BiWithdrawOrderDailyExportDTO> listPageForExport(WithdrawMonthOrderReportReq req);
}
