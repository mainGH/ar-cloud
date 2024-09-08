package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.req.PayConfigReq;
import org.ar.pay.service.IPayConfigService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/payConfig")
@Api(description = "三方通道控制器")
public class PayConfigController {

    private final IPayConfigService payConfigService;

    @PostMapping("/save")
    @ApiOperation(value = "保存三方通道")
    public RestResult<PayConfig> save(PayConfig payConfig) {
        payConfigService.save(payConfig);
        return RestResult.ok();
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新三方通道")
    public RestResult update(@RequestBody @ApiParam PayConfig payConfig) {
        boolean su = payConfigService.updateById(payConfig);
        return RestResult.ok();
    }

    @PostMapping("/list")
    @ApiOperation(value = "获取三方通道列表")
    public RestResult list(@RequestBody @ApiParam PayConfigReq payconfigReq) {
        PageReturn<PayConfig> payConfigPage = payConfigService.listPage(payconfigReq);
        return RestResult.ok(payConfigPage);
    }
}
