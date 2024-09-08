package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UsdtConfigDTO;
import org.ar.common.pay.req.*;
import org.ar.manager.annotation.SysLog;
import org.ar.manager.api.CancellationRechargeClient;
import org.ar.manager.api.UsdtConfigClient;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author
 */
@RestController
@RequiredArgsConstructor
@Api(description = "usdt管理")
@RequestMapping(value = {"/api/v1/usdtConfigAdmin", "/usdtConfigAdmin"})
public class UsdtConfigController {
    @Resource
    UsdtConfigClient usdtConfigClient;

    @PostMapping("/listpage")
    @ApiOperation(value = "usdt管理列表")
    public RestResult<List<UsdtConfigDTO>> list(@RequestBody @ApiParam UsdtConfigPageReq req) {
        RestResult<List<UsdtConfigDTO>> result = usdtConfigClient.listpage(req);
        return result;
    }


    @PostMapping("/create")
    @SysLog(title = "usdt管理", content = "新增usdt配置")
    @ApiOperation(value = "新增usdt配置")
    public RestResult<UsdtConfigDTO> create(@RequestBody @ApiParam UsdtConfigCreateReq req) {
        RestResult result = usdtConfigClient.create(req);
        return result;
    }

    @PostMapping("/update")
    @SysLog(title = "usdt管理", content = "修改usdt配置")
    @ApiOperation(value = "修改usdt配置")
    public RestResult<UsdtConfigDTO> update(@RequestBody @ApiParam UsdtConfigReq req) {
        RestResult<UsdtConfigDTO> result = usdtConfigClient.update(req);

        return result;
    }

    @PostMapping("/changeStatus")
    @SysLog(title = "usdt管理", content = "修改状态")
    @ApiOperation(value = "修改状态")
    public RestResult<UsdtConfigDTO> changeStatus(@RequestBody @ApiParam UsdtConfigQueryReq req) {
        RestResult<UsdtConfigDTO> result = usdtConfigClient.changeStatus(req);
        return result;
    }

    @PostMapping("/getInfo")
    @ApiOperation(value = "获取配置详情")
    public RestResult<UsdtConfigDTO> getInfo(@RequestBody @ApiParam UsdtConfigIdReq req) {
        RestResult<UsdtConfigDTO> result = usdtConfigClient.getInfo(req);

        return result;
    }

    @PostMapping("/delete")
    @SysLog(title = "usdt管理", content = "删除")
    @ApiOperation(value = "删除")
    public RestResult<UsdtConfigDTO> delete(@RequestBody @ApiParam UsdtConfigIdReq req) {
        RestResult<UsdtConfigDTO> result = usdtConfigClient.delete(req);
        return result;
    }

}
