package org.ar.job.feign;

import io.swagger.annotations.ApiOperation;
import org.ar.common.core.result.RestResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "ar-wallet", contextId = "task-collection-record")
public interface TaskCollectionRecordFeignClient {

    /**
     * 前台-发送获奖会员
     *
     * @return {@link Boolean}
     */
    @GetMapping("/api/v1/taskCollectionRecord/sendRewardMember")
    @ApiOperation(value = "前台-发送获奖会员")
    RestResult<Boolean> sendRewardMember();
}
