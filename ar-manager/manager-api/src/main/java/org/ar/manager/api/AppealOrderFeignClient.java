package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.AppealOrderDTO;
import org.ar.common.pay.dto.AppealOrderExportDTO;
import org.ar.common.pay.dto.ApplyDistributedDTO;
import org.ar.common.pay.req.AppealOrderIdReq;
import org.ar.common.pay.req.AppealOrderPageListReq;
import org.ar.common.pay.req.AppealOrderReq;
import org.ar.common.pay.req.ApplyDistributedListPageReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "ar-wallet", contextId = "appeal-order")
public interface AppealOrderFeignClient {

    /**
     *
     * @param
     * @return
     */
    // @Headers({"Content-Type: application/json","Accept: application/json"})
    @PostMapping("/api/v1/appealOrder/pay")
    RestResult<AppealOrderDTO> pay(@RequestBody AppealOrderIdReq req);

    /**
     *  下发申请接口
     * @param req
     * @return
     */

    //@Headers({"Content-Type: application/json","Accept: application/json"})
    @PostMapping("/api/v1/appealOrder/nopay")
    RestResult<AppealOrderDTO> nopay(@RequestBody AppealOrderIdReq req);


    /**
     * 修改商户提现usdt地址
     * @param
     * @param
     * @return
     */
    @PostMapping("/api/v1/appealOrder/listpage")
    RestResult<List<AppealOrderDTO>> listpage(@RequestBody AppealOrderPageListReq req);

    @PostMapping("/api/v1/appealOrder/listpageExport")
    RestResult<List<AppealOrderExportDTO>> listpageExport(@RequestBody AppealOrderPageListReq req);


}
