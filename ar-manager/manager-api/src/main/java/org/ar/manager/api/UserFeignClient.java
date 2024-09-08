package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.manager.dto.UserAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "ar-manager")
public interface UserFeignClient {

    @GetMapping("/api/v1/users/username/{username}")
    RestResult<UserAuthDTO> getUserByUsername(@PathVariable String username);

    @GetMapping("/api/v1/users/member/username/{username}")
    RestResult<UserAuthDTO> getMemberUserByUsername(@PathVariable String username);
}
