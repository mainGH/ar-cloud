package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiMemberReconciliationDTO;
import org.ar.manager.entity.BiMemberReconciliation;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.entity.BiMerchantReconciliation;
import org.ar.manager.req.MerchantDailyReportReq;

import java.util.concurrent.ExecutionException;

/**
 * <p>
 * 会员对账报表 服务类
 * </p>
 *
 * @author 
 * @since 2024-03-06
 */
public interface IBiMemberReconciliationService extends IService<BiMemberReconciliation> {
    PageReturn<BiMemberReconciliationDTO> listPage(MerchantDailyReportReq req);
}
