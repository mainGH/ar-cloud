package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.manager.dto.UserAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "ar-wallet")
public interface WalletFeignClient {



    @GetMapping("/api/v1/users/merchant/username/{username}")
    RestResult<UserAuthDTO> getMemberUserByUsername(@PathVariable String username);
}
