package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TaskRulesContentDTO;
import org.ar.common.pay.req.TaskRulesContentReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author admin
 * @date 2024/3/20 14:35
 */
@FeignClient(value = "ar-wallet", contextId = "trade-rules-content")
public interface TaskRulesContentClient {

    @PostMapping("/api/v1/taskRulesContent/detail")
    RestResult<TaskRulesContentDTO> detail();


    @PostMapping("/api/v1/taskRulesContent/updateContent")
    RestResult updateContent(@RequestBody TaskRulesContentReq req);
}
