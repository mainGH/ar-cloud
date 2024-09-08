package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MemberOperationLogsDTO;
import org.ar.common.pay.req.MemberOperationLogsReq;
import org.ar.wallet.entity.MemberOperationLogs;

/**
 * <p>
 * 会员操作日志表 服务类
 * </p>
 *
 * @author 
 * @since 2024-01-13
 */
public interface IMemberOperationLogsService extends IService<MemberOperationLogs> {

    PageReturn<MemberOperationLogsDTO> listPage(MemberOperationLogsReq memberOperationLogsReq);
}
