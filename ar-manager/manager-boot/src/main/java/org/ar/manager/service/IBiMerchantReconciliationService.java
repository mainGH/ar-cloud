package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.BiMerchantReconciliationDTO;
import org.ar.manager.entity.BiMemberReconciliation;
import org.ar.manager.entity.BiMerchantReconciliation;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantDailyReportReq;

/**
 * <p>
 * 商户对账报表 服务类
 * </p>
 *
 * @author
 * @since 2024-03-06
 */
public interface IBiMerchantReconciliationService extends IService<BiMerchantReconciliation> {

    PageReturn<BiMerchantReconciliationDTO> listPage(MerchantDailyReportReq req);
}
