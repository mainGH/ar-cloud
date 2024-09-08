package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.AppVersionDTO;
import org.ar.common.pay.dto.FrontPageConfigDTO;
import org.ar.manager.annotation.SysLog;
import org.ar.manager.service.IAppVersionManagerService;
import org.ar.manager.service.IFrontPageConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 首页弹窗内容 前端控制器
 * </p>
 *
 * @author 
 * @since 2024-04-27
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Api(description = "首页弹窗内容控制器")
@RequestMapping(value = {"/api/v1/frontPageConfig", "/frontPageConfig"})
public class FrontPageConfigController {

    @Resource
    IFrontPageConfigService iFrontPageConfigService;

    @PostMapping("/listPage")
    @ApiOperation(value = "查询首页弹窗内容")
    public RestResult<List<FrontPageConfigDTO>> listPage() {
        List<FrontPageConfigDTO> result = iFrontPageConfigService.listPage();
        return RestResult.ok(result);
    }


    @PostMapping("/update")
    @SysLog(title="首页弹窗内容控制器",content = "修改首页弹窗内容")
    @ApiOperation(value = "修改首页弹窗内容")
    public RestResult update(@RequestBody @ApiParam @Valid FrontPageConfigDTO req) {
        RestResult result = iFrontPageConfigService.updateInfo(req);
        return result;
    }

}
