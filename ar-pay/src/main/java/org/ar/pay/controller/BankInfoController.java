package org.ar.pay.controller;


import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.BankInfo;
import org.ar.pay.entity.PayCard;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.req.BankInfoReq;
import org.ar.pay.req.PayCardReq;
import org.ar.pay.service.IBankInfoService;
import org.ar.pay.service.ICollectionOrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

    import org.springframework.web.bind.annotation.RestController;

        @Slf4j
        @RequiredArgsConstructor
        @RestController
        @RequestMapping("/bankInfo")
        public class BankInfoController {
            private final IBankInfoService bankInfoService;

            @PostMapping("/save")
            @ApiOperation(value = "保存银行信息")
            public RestResult<PayConfig> save(@RequestBody BankInfo bankInfo) {

                bankInfoService.save(bankInfo);
                return RestResult.ok();

            }

            @PostMapping("/update")
            @ApiOperation(value = "更新银行卡")
            public RestResult update(@RequestBody BankInfo bankInfo) {
                boolean su = bankInfoService.updateById(bankInfo);
                return RestResult.ok();

            }

            @PostMapping("/list")
            @ApiOperation(value = "查看银行卡列表")
            public RestResult list(@RequestBody BankInfoReq bankInfoReq) {
                PageReturn<BankInfo> payConfigPage = bankInfoService.listPage(bankInfoReq);
                return RestResult.ok(payConfigPage);
            }

    }
