package org.ar.manager.controller;




import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;

import org.ar.manager.annotation.SysLog;
import org.ar.manager.api.C2cConfigClient;
import org.ar.manager.api.TradeConfigClient;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(description = "配置管理控制器")
@RequestMapping(value = {"/api/v1/tradeConfigAdmin", "/tradeConfigAdmin"})
public class TradeConfigController {
    private final TradeConfigClient tradeConfigClient;

    @PostMapping("/listpage")
    @ApiOperation(value = "配置管理列表")
    public RestResult list(@RequestBody @ApiParam TradeConfigListPageReq req) {
       RestResult<List<TradeConfigDTO>> result = tradeConfigClient.listpage(req);
        return result;
    }


    @PostMapping("/updateBuy")
    @SysLog(title = "配置管理控制器",content = "参数配置买入")
    @ApiOperation(value = "参数配置买入")
    public RestResult<TradeConfigBuyDTO> updateBuy(@Validated @ApiParam @RequestBody TradeConfigBuyReq req) {
       RestResult<TradeConfigBuyDTO> result = tradeConfigClient.updateBuy(req);
        return result;
    }

    @PostMapping("/updateSell")
    @SysLog(title = "配置管理控制器",content = "参数配置卖出")
    @ApiOperation(value = "参数配置卖出")
    public RestResult<TradeConfigSellDTO> updateSell(@Validated @ApiParam @RequestBody TradeConfigSellReq req) {
        RestResult<TradeConfigSellDTO> result = tradeConfigClient.updateSell(req);
        return result;
    }

    @PostMapping("/updateVoiceEnable")
    @SysLog(title = "配置管理控制器",content = "参数配置语音开关")
    @ApiOperation(value = "参数配置语音开关")
    public RestResult<TradeConfigVoiceEnableDTO> updateVoiceEnable(@Validated @ApiParam @RequestBody TradeConfigVoiceEnableReq req) {
        RestResult<TradeConfigVoiceEnableDTO> result = tradeConfigClient.updateVoiceEnable(req);
        return result;
    }

    @PostMapping("/detaill")
    @ApiOperation(value = "查看配置信息")
    public RestResult<TradeConfigDTO> detaill(@Validated @ApiParam @RequestBody TradeConfigIdReq req) {
        RestResult<TradeConfigDTO> result = tradeConfigClient.detaill(req);
        return result;
    }

    @PostMapping("/delete")
    @SysLog(title = "配置管理控制器",content = "删除")
    @ApiOperation(value = "删除")
    public RestResult delete(@Validated @ApiParam @RequestBody TradeConfigIdReq req) {

        RestResult result = tradeConfigClient.delete(req);
        return result;
    }

    @PostMapping("/updateWarningConfig")
    @ApiOperation(value = "预警参数配置")
    public RestResult<TradeWarningConfigDTO> updateWarningConfig(@Validated @RequestBody TradeConfigWarningConfigUpdateReq req) {
        return tradeConfigClient.updateWarningConfig(req);
    }

    @PostMapping("/warningConfigDetail")
    @ApiOperation(value = "获取预警参数配置")
    public RestResult<TradeWarningConfigDTO> warningConfigDetail(@Validated @RequestBody TradeConfigIdReq req) {
        return tradeConfigClient.warningConfigDetail(req);
    }

}
