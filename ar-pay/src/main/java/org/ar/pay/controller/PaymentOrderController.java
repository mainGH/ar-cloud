package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.PaymentOrder;
import org.ar.pay.req.PaymentOrderReq;
import org.ar.pay.service.IPaymentOrderService;
import org.ar.pay.vo.CollectionOrderInfoVo;
import org.ar.pay.vo.PaymentOrderListVo;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/paymentOrder")
@Api(description = "代付订单控制器")
public class PaymentOrderController {

    private final IPaymentOrderService paymentOrderService;

    @PostMapping("/save")
    @ApiOperation(value = "保存代付订单")
    public RestResult<PaymentOrder> save(@RequestBody @ApiParam PaymentOrder paymentOrder) {
        paymentOrderService.save(paymentOrder);
        return RestResult.ok(paymentOrder);
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新代付订单")
    public RestResult update(@RequestBody @ApiParam PaymentOrder paymentOrder) {
        boolean su = paymentOrderService.updateById(paymentOrder);
        return RestResult.ok();
    }

    @PostMapping("/list")
    @ApiOperation(value = "查看代付订单")
    public RestResult list(@RequestBody(required = false) @ApiParam PaymentOrderReq paymentOrder) {
        PageReturn<PaymentOrderListVo> roleVOPage = paymentOrderService.listPage(paymentOrder);
        return RestResult.ok(roleVOPage);
    }

    @GetMapping("/paymentOrderInfo")
    @ApiOperation(value = "查询代付订单详情")
    public RestResult<CollectionOrderInfoVo> paymentOrderInfo(@ApiParam(name = "商户订单号") String merchantOrder) {
        return paymentOrderService.getPaymentOrderInfoByOrderNo(merchantOrder);
    }

}
