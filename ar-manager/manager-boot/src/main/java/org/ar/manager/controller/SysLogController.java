package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.manager.entity.SysLog;
import org.ar.manager.req.SysLogReq;
import org.ar.manager.req.UserListPageReq;
import org.ar.manager.service.ISysLogService;
import org.ar.manager.vo.SysUserVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
* @author 
*/
    @RestController
    @Slf4j
    @RequiredArgsConstructor
    @RequestMapping("/syslog")
    @Api(description = "日志控制器")
    public class SysLogController {
    private final   ISysLogService sysLogService;

    @PostMapping("/listPage")
    @ApiOperation(value = "日志分页列表")
    public RestResult<List<SysLog>> listPage(@RequestBody SysLogReq req) {

        PageReturn<SysLog> sysLogPage = sysLogService.listPage(req);
        return RestResult.page(sysLogPage);
    }


}
