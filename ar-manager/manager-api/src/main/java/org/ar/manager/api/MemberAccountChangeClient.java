package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.common.pay.dto.MemberAccountChangeDTO;
import org.ar.common.pay.req.C2cConfigReq;
import org.ar.common.pay.req.MemberAccountChangeReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "member-accountchange")
public interface MemberAccountChangeClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/memberAccounthange/listpage")
    RestResult<List<MemberAccountChangeDTO>> listpage(@RequestBody MemberAccountChangeReq req);




}
