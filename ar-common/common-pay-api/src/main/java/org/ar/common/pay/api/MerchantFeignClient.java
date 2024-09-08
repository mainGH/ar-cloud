package org.ar.common.pay.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UserAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "ar-wallet" , contextId = "manager")
public interface MerchantFeignClient {



    @GetMapping("/api/v1/merchantinfo/merchant/username/{username}")
    RestResult<UserAuthDTO> getMerchantByUsername(@PathVariable String username);


}
