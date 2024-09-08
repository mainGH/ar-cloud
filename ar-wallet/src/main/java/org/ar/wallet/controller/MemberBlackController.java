package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MatchPoolListPageDTO;
import org.ar.common.pay.dto.MemberAccountChangeDTO;
import org.ar.common.pay.dto.MemberBlackDTO;
import org.ar.common.pay.req.MatchPoolListPageReq;
import org.ar.common.pay.req.MemberBlackReq;
import org.ar.wallet.service.IMatchPoolService;
import org.ar.wallet.service.IMemberBlackService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * <p>
 * 会员黑名单 前端控制器
 * </p>
 *
 * @author 
 * @since 2024-03-29
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = {"/api/v1/memberBlack", "/memberBlack"})
@ApiIgnore
@Api(description = "会员黑名单前端控制器")
public class MemberBlackController {

    private final IMemberBlackService memberBlackService;

    @PostMapping("/listPage")
    @ApiOperation(value = "查询会员黑名单列表")
    public RestResult<List<MemberBlackDTO>> listPage(@RequestBody @ApiParam MemberBlackReq req) {
        PageReturn<MemberBlackDTO> payConfigPage = memberBlackService.listPage(req);
        return RestResult.page(payConfigPage);
    }


    @PostMapping("/removeBlack")
    @ApiOperation(value = "移除黑名单")
    public RestResult removeBlack(@RequestBody @ApiParam MemberBlackReq req) {
        return memberBlackService.removeBlack(req);
    }

}
