package org.ar.common.pay.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "ar-wallet", contextId = "memberApp")
public interface AppMemberFeignClient {

    @GetMapping("/api/v1/memberInfo/appusername/{username}")
    RestResult<MemberAuthDTO> getAppMemberByUsername(@PathVariable String username);

}
