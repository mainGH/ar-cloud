package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UsdtBuyOrderDTO;
import org.ar.common.pay.dto.UsdtBuyOrderInfoDTO;
import org.ar.common.pay.req.UsdtBuyOrderGetInfoReq;
import org.ar.common.pay.req.UsdtBuyOrderIdReq;
import org.ar.common.pay.req.UsdtBuyOrderReq;
import org.ar.common.web.utils.UserContext;
import org.ar.manager.annotation.SysLog;
import org.ar.manager.api.UsdtBuyOrderFeignClient;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(description = "usdt买入订单")
@RequestMapping(value = {"/api/v1/usdtBuyOrderAdmin", "/usdtBuyOrderAdmin"})
public class UsdtBuyOrderController {
    private final UsdtBuyOrderFeignClient usdtBuyOrderFeignClient;

    @PostMapping("/listpage")
    @ApiOperation(value = "usdt订单买入列表")
    public RestResult<List<UsdtBuyOrderDTO>> listpage(@RequestBody @ApiParam UsdtBuyOrderReq req) {
        RestResult<List<UsdtBuyOrderDTO>> result = usdtBuyOrderFeignClient.listpage(req);
        return result;
    }

    @PostMapping("/getInfo")
    @ApiOperation(value = "查看")
    public RestResult<UsdtBuyOrderInfoDTO> getInfo(@RequestBody @ApiParam UsdtBuyOrderGetInfoReq req) {

        RestResult<UsdtBuyOrderInfoDTO> result =  usdtBuyOrderFeignClient.getInfo(req);
        return result;
    }


    @PostMapping("/pay")
    @SysLog(title = "usdt买入订单",content = "支付")
    @ApiOperation(value = "支付")
    public RestResult<UsdtBuyOrderDTO> pay(@RequestBody @ApiParam UsdtBuyOrderIdReq req) {
        String updateBy = UserContext.getCurrentUserName();
        req.setUpdateBy(updateBy);
        RestResult<UsdtBuyOrderDTO> result = usdtBuyOrderFeignClient.pay(req);

        return result;
    }

    @PostMapping("/nopay")
    @SysLog(title = "usdt买入订单",content = "未支付")
    @ApiOperation(value = "未支付")
    public RestResult<UsdtBuyOrderDTO> nopay(@RequestBody @ApiParam UsdtBuyOrderIdReq req) {
        RestResult<UsdtBuyOrderDTO> result = usdtBuyOrderFeignClient.nopay(req);
        return result;
    }

}
