package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberAccountChangeDTO;
import org.ar.common.pay.dto.MemberBlackDTO;
import org.ar.common.pay.req.MemberAccountChangeReq;
import org.ar.common.pay.req.MemberBlackReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "member-Black")
public interface MemberBlackClient {


    @PostMapping("/api/v1/memberBlack/listPage")
    RestResult<List<MemberBlackDTO>> listPage(MemberBlackReq req);

    @PostMapping("/api/v1/memberBlack/removeBlack")
    RestResult removeBlack(MemberBlackReq req);
}
