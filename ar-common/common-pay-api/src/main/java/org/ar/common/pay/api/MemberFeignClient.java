package org.ar.common.pay.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "ar-wallet", contextId = "member")
public interface MemberFeignClient {

    @GetMapping("/api/v1/memberInfo/username/{username}")
    RestResult<MemberAuthDTO> getMemberByUsername(@PathVariable String username);

}
