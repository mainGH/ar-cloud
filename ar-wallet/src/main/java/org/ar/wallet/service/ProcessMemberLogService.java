package org.ar.wallet.service;

import org.ar.common.core.dto.MemberLoginLogMessage;
import org.ar.wallet.entity.MemberOperationLogMessage;

public interface ProcessMemberLogService {

    /**
     * 处理会员登录日志记录
     *
     * @param memberLoginLogMessage
     * @return {@link Boolean}
     */
    Boolean processMemberLoginLog(MemberLoginLogMessage memberLoginLogMessage);


    /**
     * 处理会员操作日志记录
     *
     * @param memberOperationLogMessage
     * @return {@link Boolean}
     */
    Boolean processMemberOperationLog(MemberOperationLogMessage memberOperationLogMessage);
}
