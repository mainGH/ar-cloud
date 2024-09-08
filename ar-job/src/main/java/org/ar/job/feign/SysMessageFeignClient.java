package org.ar.job.feign;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.ar.common.core.result.RestResult;
import org.ar.manager.req.SysMessageSendReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author admin
 * @date 2024/5/7 11:26
 */
@FeignClient(value = "ar-manager")
public interface SysMessageFeignClient {

    @PostMapping("/sysMessage/sendMessage")
    @ApiOperation(value = "发送消息")
    RestResult sendMessage(@RequestBody @ApiParam SysMessageSendReq req);
}
