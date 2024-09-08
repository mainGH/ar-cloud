package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.PayCard;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.req.PayCardReq;
import org.ar.pay.service.IPayCardService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/payCard")
@Api(description = "银行卡控制器")
public class PayCardController {

    private final IPayCardService payCardService;

    @PostMapping("/save")
    @ApiOperation(value = "保存银行卡")
    public RestResult<PayConfig> save(@RequestBody PayCard paycard) {

        payCardService.save(paycard);
        return RestResult.ok();

    }

    @PostMapping("/update")
    @ApiOperation(value = "更新银行卡")
    public RestResult update(@RequestBody PayCard payCard) {
        boolean su = payCardService.updateById(payCard);
        return RestResult.ok();

    }

    @PostMapping("/list")
    @ApiOperation(value = "查看银行卡列表")
    public RestResult list(@RequestBody PayCardReq payCardReq) {
        PageReturn<PayCard> payConfigPage = payCardService.listPage(payCardReq);
        return RestResult.ok(payConfigPage);
    }


}
