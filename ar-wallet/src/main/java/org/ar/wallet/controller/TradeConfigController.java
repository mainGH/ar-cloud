package org.ar.wallet.controller;


import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.ar.common.redis.util.RedisUtils;
import org.ar.wallet.entity.TradeConfig;
import org.ar.wallet.service.ITradeConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(description = "配置信息控制器")
@RequestMapping(value = {"/api/v1/tradeConfig", "/tradeConfig"})
@ApiIgnore
public class TradeConfigController {
    private final ITradeConfigService tradeConfigService;
    private final RedisUtils redisUtils;

    @PostMapping("/listpage")
    @ApiOperation(value = "获取配置列表")
    public RestResult list(@RequestBody @ApiParam TradeConfigListPageReq req) {
        PageReturn<TradeConfigDTO> payConfigPage = tradeConfigService.listPage(req);
        return RestResult.page(payConfigPage);
    }


    @PostMapping("/updateBuy")
    @ApiOperation(value = "参数配置")
    public RestResult updateBuy(@Validated @RequestBody TradeConfigBuyReq req) {
        TradeConfig tradeConfig = new TradeConfig();
        BeanUtils.copyProperties(req, tradeConfig);
        tradeConfig.setIsSplitOrder(null);
        tradeConfigService.updateById(tradeConfig);
        TradeConfigBuyDTO tradeConfigBuyDTO = new TradeConfigBuyDTO();
        BeanUtils.copyProperties(tradeConfig,tradeConfigBuyDTO);
        return RestResult.ok(tradeConfigBuyDTO);
    }

    @PostMapping("/updateSell")
    @ApiOperation(value = "卖出参数配置")
    public RestResult<TradeConfigSellDTO> updateSell(@Validated @RequestBody TradeConfigSellReq req) {
        TradeConfig tradeConfig = new TradeConfig();
        BeanUtils.copyProperties(req, tradeConfig);
        tradeConfigService.updateById(tradeConfig);
        redisUtils.set(GlobalConstants.SELL_CONFIG, JSON.toJSON(req));
        TradeConfigSellDTO tradeConfigSellDTO = new TradeConfigSellDTO();
        BeanUtils.copyProperties(tradeConfig, tradeConfigSellDTO);
        return RestResult.ok(tradeConfigSellDTO);
    }
    @PostMapping("/updateVoiceEnable")
    @ApiOperation(value = "语音开关参数配置")
    public RestResult<TradeConfigVoiceEnableDTO> updateVoiceEnable(@Validated @RequestBody TradeConfigVoiceEnableReq req) {
        TradeConfigVoiceEnableDTO result = tradeConfigService.updateVoiceEnable(req);
        return RestResult.ok(result);
    }


    @PostMapping("/detaill")
    @ApiOperation(value = "查看配置信息")
    public RestResult<TradeConfigDTO> detaill(@Validated @RequestBody TradeConfigIdReq req) {
        TradeConfig tradeConfig  = tradeConfigService.getById(req.getId());
        TradeConfigDTO tradeConfigDTO = new TradeConfigDTO();
        BeanUtils.copyProperties(tradeConfig,tradeConfigDTO);
        return RestResult.ok(tradeConfigDTO);
    }

    @PostMapping("/delete")
    @ApiOperation(value = "查看配置信息")
    public RestResult delete(@Validated @RequestBody TradeConfigIdReq req) {
        TradeConfig tradeConfig = new TradeConfig();
        BeanUtils.copyProperties(req,tradeConfig);
        tradeConfigService.removeById(tradeConfig);
        return RestResult.ok("删除成功");
    }

    @PostMapping("/updateWarningConfig")
    @ApiOperation(value = "预警参数配置")
    public RestResult<TradeWarningConfigDTO> updateWarningConfig(@Validated @RequestBody TradeConfigWarningConfigUpdateReq req) {
        TradeWarningConfigDTO result = tradeConfigService.updateWarningConfig(req);
        return RestResult.ok(result);
    }

    @PostMapping("/warningConfigDetail")
    @ApiOperation(value = "获取预警参数配置")
    public RestResult<TradeWarningConfigDTO> warningConfigDetail(@Validated @RequestBody TradeConfigIdReq req) {
        TradeWarningConfigDTO result = tradeConfigService.warningConfigDetail(req);
        return RestResult.ok(result);
    }


}
