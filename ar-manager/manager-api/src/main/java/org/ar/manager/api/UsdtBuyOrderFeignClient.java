package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UsdtBuyOrderDTO;
import org.ar.common.pay.dto.UsdtBuyOrderInfoDTO;
import org.ar.common.pay.req.UsdtBuyOrderGetInfoReq;
import org.ar.common.pay.req.UsdtBuyOrderIdReq;
import org.ar.common.pay.req.UsdtBuyOrderReq;
import org.ar.manager.dto.UserAuthDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(value = "ar-wallet", contextId = "usdt-buy")
public interface UsdtBuyOrderFeignClient {

    @PostMapping("/api/v1/usdtBuyOrder/listpage")
    RestResult<List<UsdtBuyOrderDTO>> listpage(@RequestBody UsdtBuyOrderReq req);

    @PostMapping("/api/v1/usdtBuyOrder/getInfo")
    RestResult<UsdtBuyOrderInfoDTO> getInfo(@RequestBody UsdtBuyOrderGetInfoReq req);

    @PostMapping("/api/v1/usdtBuyOrder/pay")
    RestResult<UsdtBuyOrderDTO> pay(@RequestBody  UsdtBuyOrderIdReq req);

    @PostMapping("/api/v1/usdtBuyOrder/nopay")
    RestResult<UsdtBuyOrderDTO> nopay(@RequestBody  UsdtBuyOrderIdReq req);
}
