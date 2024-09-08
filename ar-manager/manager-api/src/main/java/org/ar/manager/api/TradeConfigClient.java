package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "trade-config")
public interface TradeConfigClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/tradeConfig/listpage")
    RestResult<List<TradeConfigDTO>> listpage(@RequestBody TradeConfigListPageReq req);

    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/tradeConfig/updateBuy")
    RestResult<TradeConfigBuyDTO> updateBuy(@RequestBody TradeConfigBuyReq req);


    @PostMapping("/api/v1/tradeConfig/updateSell")
    RestResult<TradeConfigSellDTO> updateSell(@RequestBody TradeConfigSellReq req);

    @PostMapping("/api/v1/tradeConfig/updateVoiceEnable")
    RestResult<TradeConfigVoiceEnableDTO> updateVoiceEnable(@RequestBody TradeConfigVoiceEnableReq req);


    /**
     * 详情
     * @param
     * @param
     * @return
     */
    @PostMapping("/api/v1/tradeConfig/detaill")
    RestResult<TradeConfigDTO> detaill(@RequestBody TradeConfigIdReq req);


    @PostMapping("/api/v1/tradeConfig/delete")
    RestResult delete(@RequestBody TradeConfigIdReq req);

    @PostMapping("/api/v1/tradeConfig/warningConfigDetail")
    RestResult<TradeWarningConfigDTO> warningConfigDetail(@RequestBody TradeConfigIdReq req);

    @PostMapping("/api/v1/tradeConfig/updateWarningConfig")
    RestResult<TradeWarningConfigDTO> updateWarningConfig(@RequestBody TradeConfigWarningConfigUpdateReq req);



}
