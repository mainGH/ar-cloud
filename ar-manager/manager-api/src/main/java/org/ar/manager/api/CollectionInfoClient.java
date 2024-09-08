package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.common.pay.dto.CollectionInfoDTO;
import org.ar.common.pay.req.C2cConfigReq;
import org.ar.common.pay.req.CollectionInfoIdReq;
import org.ar.common.pay.req.CollectionInfoListPageReq;
import org.ar.common.pay.req.CollectionInfoReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "collection-info")
public interface CollectionInfoClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/collectionInfo/listPage")
    RestResult<List<CollectionInfoDTO>> listpage(@RequestBody CollectionInfoListPageReq req);

    /**
     *
     * @param req
     * @return
     */



    /**
     * 详情
     * @param
     * @param
     * @return
     */
    @PostMapping("/api/v1/collectionInfo/update")
    RestResult<CollectionInfoDTO> update(@RequestBody CollectionInfoReq req);

    @PostMapping("/api/v1/collectionInfo/getInfo")
    RestResult<List<CollectionInfoDTO>> getInfo(@RequestBody CollectionInfoIdReq req);



    @PostMapping("/api/v1/collectionInfo/delete")
    RestResult delete(@RequestBody CollectionInfoIdReq req);


    @PostMapping("/api/v1/collectionInfo/add")
    RestResult<CollectionInfoDTO> add(CollectionInfoReq collectionInfoReq);
}
