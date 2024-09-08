package org.ar.manager.api;

import io.swagger.annotations.ApiParam;
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
@FeignClient(value = "ar-wallet", contextId = "member-level-config")
public interface MemberLevelConfigClient {

    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/memberLevelConfig/listPage")
    RestResult<List<MemberLevelConfigDTO>> listPage(@RequestBody MemberManualLogsReq req);

    @PostMapping("/api/v1/memberLevelConfig/update")
    RestResult update(MemberLevelConfigDTO req);
}
