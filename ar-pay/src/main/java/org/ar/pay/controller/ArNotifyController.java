package org.ar.pay.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.pay.service.ICollectionOrderService;
import org.ar.pay.service.IPayConfigService;
import org.ar.pay.service.PayRouteAbstract;
import org.ar.pay.util.SpringContextUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/notify")
@Api(description = "支付回调控制器")
public class ArNotifyController {
    private final IPayConfigService payConfigService;

    private final ICollectionOrderService collectionOrderService;

    @PostMapping("/callback/{code}")
    @ApiOperation(value = "接收三方支付回调接口")
    public String pay(HttpServletRequest request, HttpServletResponse response, @PathVariable String code) {
        PayRouteAbstract payRouteAbstract = SpringContextUtil.getBean(code);
        return payRouteAbstract.notify(request, response, code);
    }

    @GetMapping("/manualCallback")
    @ApiOperation(value = "手动回调接口")
    public RestResult manualCallback(@RequestParam(name = "merchantOrder") String merchantOrder) {
        return collectionOrderService.manualCallback(merchantOrder);
    }
}
