package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MemberLoginLogsDTO;
import org.ar.common.pay.req.MemberLoginLogsReq;
import org.ar.wallet.entity.MemberLoginLogs;

/**
 * <p>
 * 会员登录日志表 服务类
 * </p>
 *
 * @author 
 * @since 2024-01-13
 */
public interface IMemberLoginLogsService extends IService<MemberLoginLogs> {

    PageReturn<MemberLoginLogsDTO> listPage(MemberLoginLogsReq req);
}
