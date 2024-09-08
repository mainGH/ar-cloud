package org.ar.manager.api;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TaskManagerDTO;
import org.ar.common.pay.req.TaskManagerIdReq;
import org.ar.common.pay.req.TaskManagerListReq;
import org.ar.common.pay.req.TaskManagerReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author admin
 * @date 2024/3/19 9:52
 */
@FeignClient(value = "ar-wallet", contextId = "task-manager")
public interface TaskManagerClient {

    @PostMapping("/api/v1/taskManager/listPage")
    PageReturn<TaskManagerDTO> listPage(@RequestBody TaskManagerListReq req);

    @PostMapping("/api/v1/taskManager/taskDetail")
    RestResult<TaskManagerDTO>  taskDetail(@RequestBody TaskManagerIdReq req);

    @PostMapping("/api/v1/taskManager/createTask")
    RestResult<?>  createTask(@RequestBody TaskManagerReq req);

    @PostMapping("/api/v1/taskManager/deleteTask")
    RestResult<?> deleteTask(@RequestBody TaskManagerIdReq req);
    @PostMapping("/api/v1/taskManager/updateTask")
    RestResult<?> updateTask(@RequestBody TaskManagerReq req);
}
