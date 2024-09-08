package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MemberManualLogDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.pay.req.MemberManualLogsReq;

/**
 * <p>
 * 会员手动操作记录 服务类
 * </p>
 *
 * @author 
 * @since 2024-02-29
 */
public interface IMemberManualLogService extends IService<MemberManualLogDTO> {

    PageReturn<MemberManualLogDTO> listPage(MemberManualLogsReq req);
}
