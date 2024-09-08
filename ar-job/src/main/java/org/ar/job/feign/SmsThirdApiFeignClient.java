package org.ar.job.feign;

import io.swagger.annotations.ApiOperation;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.SmsBalanceWarnDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "ar-wallet", contextId = "sms-third-api")
public interface SmsThirdApiFeignClient {

    @GetMapping("/api/v1/smsThirdApi/checkBalance")
    @ApiOperation(value = "监控短信账户余额")
    RestResult<SmsBalanceWarnDTO> checkBalance();
}
