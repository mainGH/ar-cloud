package org.ar.manager.api;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "user-log")
public interface UserTextMessageFeignClient {

    @PostMapping("/api/v1/userVerificationCodes/listPage")
    RestResult<List<UserVerificationCodeslistPageDTO>> listPage(@RequestBody UserTextMessageReq req);
    @PostMapping("/api/v1/memberLoginLogs/listPage")
    RestResult<List<MemberLoginLogsDTO>> memberLoginLogsListPage(MemberLoginLogsReq req);
    @PostMapping("/api/v1/memberOperationLogs/listPage")
    RestResult<List<MemberOperationLogsDTO>> memberOperationLogsListPage(MemberOperationLogsReq memberOperationLogsReq);
}
