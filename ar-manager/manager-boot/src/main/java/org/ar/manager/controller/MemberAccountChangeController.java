package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberAccountChangeDTO;
import org.ar.common.pay.req.MemberAccountChangeReq;
import org.ar.manager.api.MemberAccountChangeClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * @author
 */
@RestController
@RequiredArgsConstructor
@Api(description = "会员账变控制器")
@RequestMapping(value = {"/api/v1/memberAccounthangeAdmin", "/memberAccounthangeAdmin"})
public class MemberAccountChangeController {
    private final MemberAccountChangeClient memberAccountChangeClient;


    @PostMapping("/listpage")
    @ApiOperation(value = "会员账变列表")
    public RestResult<List<MemberAccountChangeDTO>> listpage(@RequestBody @ApiParam @Valid MemberAccountChangeReq memberAccountChangeReq) {
        RestResult<List<MemberAccountChangeDTO>> result = memberAccountChangeClient.listpage(memberAccountChangeReq);
        return result;
    }


}
