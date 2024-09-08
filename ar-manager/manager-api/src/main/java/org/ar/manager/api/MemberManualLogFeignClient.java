package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberLoginLogsDTO;
import org.ar.common.pay.dto.MemberManualLogDTO;
import org.ar.common.pay.dto.MemberOperationLogsDTO;
import org.ar.common.pay.dto.UserVerificationCodeslistPageDTO;
import org.ar.common.pay.req.MemberLoginLogsReq;
import org.ar.common.pay.req.MemberManualLogsReq;
import org.ar.common.pay.req.MemberOperationLogsReq;
import org.ar.common.pay.req.UserTextMessageReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "member-manual-log")
public interface MemberManualLogFeignClient {

    @PostMapping("/api/v1/memberManualLog/listPage")
    RestResult<List<MemberManualLogDTO>> listPage(@RequestBody MemberManualLogsReq req);
    @PostMapping("/api/v1/memberManualLog/listPage")
    RestResult<List<MemberLoginLogsDTO>> memberLoginLogsListPage(MemberLoginLogsReq req);
    @PostMapping("/api/v1/memberManualLog/listPage")
    RestResult<List<MemberOperationLogsDTO>> memberOperationLogsListPage(MemberOperationLogsReq memberOperationLogsReq);
}
