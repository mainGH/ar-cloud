package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberLoginLogsDTO;
import org.ar.common.pay.dto.UserVerificationCodeslistPageDTO;
import org.ar.common.pay.req.MemberLoginLogsReq;
import org.ar.common.pay.req.UserTextMessageReq;
import org.ar.wallet.entity.MemberLoginLogs;
import org.ar.wallet.service.IMemberLoginLogsService;
import org.ar.wallet.service.IUserVerificationCodesService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * <p>
 * 会员登录日志表 前端控制器
 * </p>
 *
 * @author
 * @since 2024-01-13
 */
@RequestMapping("/api/v1/memberLoginLogs")
@Slf4j
@RequiredArgsConstructor
@RestController
@Api(description = "会员登录日志控制器")
@ApiIgnore
public class MemberLoginLogsController {

    private final IMemberLoginLogsService memberLoginLogsService;

    @PostMapping("/listPage")
    @ApiOperation(value = "获取会员操作日志列表")
    public RestResult<List<MemberLoginLogsDTO>> listPage(@RequestBody @ApiParam MemberLoginLogsReq req) {
        PageReturn<MemberLoginLogsDTO> payConfigPage = memberLoginLogsService.listPage(req);
        return RestResult.page(payConfigPage);
    }

}
