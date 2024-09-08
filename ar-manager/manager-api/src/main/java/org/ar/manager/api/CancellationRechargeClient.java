package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.common.pay.dto.CancellationRechargeDTO;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author
 */
@FeignClient(value = "ar-wallet", contextId = "cancellation-recharge")
public interface CancellationRechargeClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/cancellationRecharge/listpage")
    RestResult<List<CancellationRechargeDTO>> listpage(@RequestBody CancellationRechargePageListReq req);

    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/cancellationRecharge/create")
    RestResult<CancellationRechargeDTO> create(@RequestBody CancellationRechargeAddReq req);


    /**
     *
     * @param
     * @param
     * @return
     */
    @PostMapping("/api/v1/cancellationRecharge/update")
    RestResult<CancellationRechargeDTO> update(@RequestBody CancellationRechargeReq req);

    @PostMapping("/api/v1/cancellationRecharge/getInfo")
    RestResult<CancellationRechargeDTO> getInfo(@RequestBody CancellationRechargeIdReq req);


    @PostMapping("/api/v1/cancellationRecharge/delete")
    RestResult delete(@RequestBody CancellationRechargeIdReq req);


}
