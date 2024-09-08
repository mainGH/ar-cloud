package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.MatchingOrderAppealReq;
import org.ar.common.pay.req.MatchingOrderIdReq;
import org.ar.common.pay.req.MatchingOrderReq;
import org.ar.common.pay.req.MemberBlackReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "correlation-member")
public interface CorrelationMemberClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/correlationMember/listPage")
    RestResult<List<CorrelationMemberDTO>> listPage(@RequestBody MemberBlackReq req);
}
