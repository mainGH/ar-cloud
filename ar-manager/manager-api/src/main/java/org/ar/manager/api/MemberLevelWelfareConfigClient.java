package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberLevelConfigDTO;
import org.ar.common.pay.dto.MemberLevelWelfareConfigDTO;
import org.ar.common.pay.req.MemberManualLogsReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "member-level-welfare-config")
public interface MemberLevelWelfareConfigClient {

    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/memberLevelWelfareConfig/listPage")
    RestResult<List<MemberLevelWelfareConfigDTO>> listPage(@RequestBody MemberManualLogsReq req);

    @PostMapping("/api/v1/memberLevelWelfareConfig/update")
    RestResult update(MemberLevelWelfareConfigDTO req);
}
