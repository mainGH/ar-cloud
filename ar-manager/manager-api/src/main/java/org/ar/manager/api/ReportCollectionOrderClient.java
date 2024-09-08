package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberGroupDTO;
import org.ar.common.pay.req.MemberGroupReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
@FeignClient(value = "ar-wallet", contextId = "Report-Collection-Order")
interface ReportCollectionOrderClient {




    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/memberGroup/listpage")
    RestResult<List<MemberGroupDTO>> listpage(@RequestBody MemberGroupReq req);

    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/memberGroup/update")
    RestResult<MemberGroupDTO> update(@RequestBody MemberGroupReq req);


    /**
     *
     * @param req
     * @return
     */
    @PostMapping("/api/v1/memberGroup/create")
    RestResult<MemberGroupDTO> create(@RequestBody MemberGroupReq req);



    /**
     * 详情
     * @param
     * @param
     * @return
     */
    @PostMapping("/api/v1/memberGroup/getInfo")
    RestResult<MemberGroupDTO> getInfo(@RequestBody MemberGroupReq req);


    @PostMapping("/api/v1/memberGroup/delete")
    RestResult<MemberGroupDTO> delete(@RequestBody MemberGroupReq req);

}
