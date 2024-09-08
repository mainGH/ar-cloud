package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.common.pay.dto.WithdrawalCancellationDTO;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "Withdrawal-Cancellation")
public interface WindrawalCancellationClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/withdrawalCancellation/listpage")
    RestResult<List<WithdrawalCancellationDTO>> listpage(@RequestBody WithdrawalCancellationReq req);

    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/withdrawalCancellation/create")
    RestResult<WithdrawalCancellationDTO> create(@RequestBody WithdrawalCancellationCreateReq req);


    /**
     * 详情
     * @param
     * @param
     * @return
     */
    @PostMapping("/api/v1/withdrawalCancellation/update")
    RestResult<WithdrawalCancellationDTO> update(@RequestBody WithdrawalCancellationAddReq req);


    @PostMapping("/api/v1/withdrawalCancellation/getInfo")
    RestResult<WithdrawalCancellationDTO> getInfo(@RequestBody WithdrawalCancellationIdReq req);

    @PostMapping("/api/v1/withdrawalCancellation/delete")
    RestResult delete(@RequestBody WithdrawalCancellationIdReq req);


}
