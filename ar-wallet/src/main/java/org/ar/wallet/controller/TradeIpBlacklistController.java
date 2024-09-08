package org.ar.wallet.controller;


import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TradeIpBlackListPageDTO;
import org.ar.common.pay.req.TradeIpBlackListReq;
import org.ar.wallet.entity.TradeIpBlacklist;
import org.ar.wallet.rabbitmq.RabbitMQService;
import org.ar.wallet.service.ITradeIpBlacklistService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * <p>
 * 交易IP黑名单表，用于存储不允许进行交易的IP地址 前端控制器
 * </p>
 *
 * @author
 * @since 2024-02-21
 */
@RestController
@Api(description = "交易IP黑名单控制器")
@RequiredArgsConstructor
@RequestMapping(value = {"/api/v1/tradeIpBlacklist", "tradeIpBlacklist"})
@ApiIgnore
public class TradeIpBlacklistController {

    private final ITradeIpBlacklistService tradeIpBlacklistService;
    private final RabbitMQService rabbitMQService;

    @PostMapping("/save")
    @ApiOperation(value = "保存")
    public RestResult save(@RequestBody @ApiParam TradeIpBlackListReq req) {
        return tradeIpBlacklistService.save(req);
    }


    @PostMapping("/listPage")
    @ApiOperation(value = "交易IP黑名单列表")
    public RestResult<List<TradeIpBlackListPageDTO>> list(@RequestBody @ApiParam TradeIpBlackListReq req) {
        PageReturn<TradeIpBlackListPageDTO> tradeIpBlacklistPageReturn = tradeIpBlacklistService.listPage(req);
        return RestResult.page(tradeIpBlacklistPageReturn);
    }


    @PostMapping("/del")
    @ApiOperation(value = "删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "行id", required = true, dataType = "String")
    })
    public RestResult del(@RequestParam(value = "id") String id) {
        boolean result = tradeIpBlacklistService.del(id);
        return result ? RestResult.ok() : RestResult.failed();
    }

}
