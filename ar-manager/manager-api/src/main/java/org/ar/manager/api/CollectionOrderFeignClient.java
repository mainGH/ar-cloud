package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.CollectionOrderDTO;
import org.ar.common.pay.dto.CollectionOrderExportDTO;
import org.ar.common.pay.dto.CollectionOrderInfoDTO;
import org.ar.common.pay.req.CollectionOrderGetInfoReq;
import org.ar.common.pay.req.CollectionOrderIdReq;
import org.ar.common.pay.req.CollectionOrderListPageReq;
import org.ar.manager.dto.AccountChangeDTO;
import org.ar.manager.req.AccountChangeReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "collection-order")
public interface CollectionOrderFeignClient {




    @PostMapping("/api/v1/collectionOrder/listRecordPage")
    RestResult<List<CollectionOrderDTO>> listRecordPage(@RequestBody CollectionOrderListPageReq req);


    @PostMapping("/api/v1/collectionOrder/listPage")
    RestResult<List<CollectionOrderDTO>> listPage(@RequestBody CollectionOrderListPageReq req);

    @PostMapping("/api/v1/collectionOrder/listPageExport")
    RestResult<List<CollectionOrderExportDTO>> listPageExport(@RequestBody CollectionOrderListPageReq req);


    @PostMapping("/api/v1/collectionOrder/listPageRecordTotal")
    RestResult<CollectionOrderDTO> listPageRecordTotal(@RequestBody CollectionOrderListPageReq req);

    @PostMapping("/api/v1/collectionOrder/getInfo")
    RestResult<CollectionOrderInfoDTO> getInfo(@RequestBody CollectionOrderGetInfoReq req);
    @PostMapping("/api/v1/collectionOrder/pay")
    RestResult<CollectionOrderDTO> pay(@RequestBody CollectionOrderIdReq req);

    @PostMapping("/api/v1/collectionOrder/manualCallback")
    RestResult<Boolean> manualCallback(@RequestParam(value = "id") Long id, @RequestParam(value = "opName") String opName);
}
