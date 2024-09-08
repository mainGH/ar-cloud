package org.ar.pay.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.pay.service.IPayConfigService;
import org.ar.pay.service.PayRouteAbstract;
import org.ar.pay.service.paymentservice.PaymentRouteAbstract;
import org.ar.pay.util.SpringContextUtil;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/notify")
@Api(description = "支付回调控制器")
public class ArPaymentNotifyController {
    private final IPayConfigService payConfigService;

    @PostMapping("/callbackpayment/{code}")
    @ApiOperation(value = "接收三方支付回调接口")
    public String pay(HttpServletRequest request, HttpServletResponse response, @PathVariable String code) {
        PaymentRouteAbstract payRouteAbstract = SpringContextUtil.getBean(code);
        return payRouteAbstract.notify(request, response, code);
    }
}