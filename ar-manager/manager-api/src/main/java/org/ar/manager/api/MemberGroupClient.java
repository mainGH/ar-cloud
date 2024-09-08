package org.ar.manager.api;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.common.pay.dto.MemberAuthListDTO;
import org.ar.common.pay.dto.MemberGroupDTO;
import org.ar.common.pay.dto.MemberGroupListPageDTO;
import org.ar.common.pay.req.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * @author Admin
 */
@FeignClient(value = "ar-wallet", contextId = "member-group")
public interface MemberGroupClient {


    /**
     *
     * @param
     * @return
     */
    @PostMapping("/api/v1/memberGroup/listpage")
    RestResult<List<MemberGroupListPageDTO>> listpage(@RequestBody MemberGroupListPageReq req);

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
    RestResult<MemberGroupDTO> create(@RequestBody MemberGroupAddReq req);



    /**
     * 详情
     * @param
     * @param
     * @return
     */
    @PostMapping("/api/v1/memberGroup/getInfo")
    RestResult<MemberGroupDTO> getInfo(@RequestBody MemberGroupIdReq req);


    @PostMapping("/api/v1/memberGroup/delete")
    RestResult<MemberGroupDTO> delete(@RequestBody MemberGroupIdReq req);


    @PostMapping("/api/v1/memberGroup/authList")
    RestResult<List<MemberAuthListDTO>> authList();


}
