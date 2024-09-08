package org.ar.wallet.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;

import org.ar.common.pay.dto.MemberAccountChangeDTO;
import org.ar.common.pay.req.MemberAccountChangeReq;
import org.ar.wallet.entity.MemberAccountChange;
import org.ar.wallet.service.IMemberAccountChangeService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

    import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

/**
* @author 
*/
        @RestController
        @RequiredArgsConstructor
        @RequestMapping(value = {"/api/v1/memberAccounthange", "/memberAccounthange"})
        @ApiIgnore
        public class MemberAccountChangeController {
        private final IMemberAccountChangeService memberAccountChangeService;


    @PostMapping("/listpage")
    @ApiOperation(value = "列表")
    public RestResult<List<MemberAccountChangeDTO>> list(@RequestBody @ApiParam @Valid MemberAccountChangeReq memberAccountChangeReq) {
        PageReturn<MemberAccountChangeDTO> payConfigPage =   memberAccountChangeService.listPage(memberAccountChangeReq);
        return RestResult.page(payConfigPage);
    }

//    @PostMapping("/create")
//    @ApiOperation(value = "添加收款信息")
//    public RestResult<MemberAccountChangeDTO> create(@RequestBody @ApiParam @Valid MemberAccountChangeReq memberAccountChangeReq) {
//        MemberAccountChange memberAccountChange = new MemberAccountChange();
//        BeanUtils.copyProperties(memberAccountChangeReq,memberAccountChange);
//        MemberAccountChangeDTO memberAccountChangeDTO = new MemberAccountChangeDTO();
//        memberAccountChangeService.save(memberAccountChange);
//        BeanUtils.copyProperties(memberAccountChange,memberAccountChangeDTO);
//
//        return RestResult.ok(memberAccountChangeDTO);
//    }
//
//    @PostMapping("/udpate")
//    @ApiOperation(value = "添加收款信息")
//    public RestResult<MemberAccountChangeDTO> update(@RequestBody @ApiParam @Valid MemberAccountChangeReq memberAccountChangeReq) {
//        MemberAccountChange memberAccountChange = new MemberAccountChange();
//        BeanUtils.copyProperties(memberAccountChangeReq,memberAccountChange);
//        MemberAccountChangeDTO memberAccountChangeDTO = new MemberAccountChangeDTO();
//        memberAccountChangeService.updateById(memberAccountChange);
//        BeanUtils.copyProperties(memberAccountChange,memberAccountChangeDTO);
//
//        return RestResult.ok(memberAccountChangeDTO);
//    }





}
