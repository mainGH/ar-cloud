package org.ar.job.feign;

import org.ar.common.pay.dto.MerchantLastOrderWarnDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "merchant-info")
public interface MerchantInfoClient {
    @PostMapping("/api/v1/merchantinfo/getLatestOrderTime")
    List<MerchantLastOrderWarnDTO> getLatestOrderTime();
}
