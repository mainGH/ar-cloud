package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TaskManagerDTO;
import org.ar.common.pay.req.TaskManagerIdReq;
import org.ar.common.pay.req.TaskManagerListReq;
import org.ar.common.pay.req.TaskManagerReq;
import org.ar.wallet.service.ITaskManagerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * <p>
 * 任务管理表 前端控制器
 * </p>
 *
 * @author
 * @since 2024-03-18
 */
@RestController
@RequiredArgsConstructor
@Api("任务管理控制器")
@RequestMapping(value = {"/api/v1/taskManager", "/taskManager"})
@ApiIgnore
public class TaskManagerController {

    private final ITaskManagerService taskManagerService;

    @PostMapping("/listPage")
    @ApiOperation(value = "获取任务管理列表")
    public PageReturn<TaskManagerDTO> listPage(@RequestBody @ApiParam TaskManagerListReq req) {
        return taskManagerService.listPage(req);
    }

    @PostMapping("/taskDetail")
    public RestResult<TaskManagerDTO> getBannerById(@RequestBody @ApiParam TaskManagerIdReq req) {
        return taskManagerService.taskDetail(req);
    }

    @PostMapping("/createTask")
    public RestResult<?> createTask(@RequestBody @ApiParam TaskManagerReq req) {
        return taskManagerService.createTask(req);
    }

    @PostMapping("/deleteTask")
    public RestResult<?> deleteTask(@RequestBody @ApiParam TaskManagerIdReq req) {
        return taskManagerService.deleteTask(req) ? RestResult.ok() : RestResult.failed();
    }

    @PostMapping("/updateTask")
    public RestResult<?> updateTask(@RequestBody @ApiParam TaskManagerReq req) {
        return taskManagerService.updateTask(req);
    }
}
