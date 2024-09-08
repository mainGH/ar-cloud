package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "payment-order")
public interface PaymentOrderFeignClient {




    @PostMapping("/api/v1/paymentOrder/listRecordPage")
    RestResult<List<PaymentOrderListPageDTO>> listRecordPage(@RequestBody PaymentOrderListPageReq req);


    @PostMapping("/api/v1/paymentOrder/listPage")
    RestResult<List<PaymentOrderListPageDTO>> listPage(@RequestBody PaymentOrderListPageReq req);

    @PostMapping("/api/v1/paymentOrder/listPageExport")
    RestResult<List<PaymentOrderExportDTO>> listPageExport(@RequestBody PaymentOrderListPageReq req);


    @PostMapping("/api/v1/paymentOrder/listRecordTotalPage")
    RestResult<PaymentOrderListPageDTO> listRecordTotalPage(@RequestBody PaymentOrderListPageReq req);

    @PostMapping("/api/v1/paymentOrder/getInfo")
    RestResult<PaymentOrderInfoDTO> getInfo(@RequestBody PaymentOrderGetInfoReq req);
    @PostMapping("/api/v1/paymentOrder/cancel")
    RestResult<PaymentOrderListPageDTO> cancel(@RequestBody PaymentOrderIdReq req);

    @PostMapping("/api/v1/paymentOrder/manualCallback")
    RestResult<Boolean> manualCallback(@RequestParam(value = "id") Long id, @RequestParam(value = "opName") String opName);
}
