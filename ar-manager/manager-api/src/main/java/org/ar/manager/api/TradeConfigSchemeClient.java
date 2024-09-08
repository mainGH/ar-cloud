package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.TradeConfigSchemeDTO;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "trade-config-scheme")
public interface TradeConfigSchemeClient {
    /**
     * @param
     * @return
     */
    @PostMapping("/api/v1/tradeConfigScheme/listPage")
    RestResult<List<TradeConfigSchemeDTO>> listPage(@RequestBody TradeConfigSchemeListPageReq req);

    @PostMapping("/api/v1/tradeConfigScheme/detail")
    RestResult<TradeConfigSchemeDTO> detail(@RequestBody TradeConfigIdReq req);

    @PostMapping("/api/v1/tradeConfigScheme/updateScheme")
    RestResult<TradeConfigSchemeDTO> updateScheme(@RequestBody TradeConfigSchemeReq req);

}
