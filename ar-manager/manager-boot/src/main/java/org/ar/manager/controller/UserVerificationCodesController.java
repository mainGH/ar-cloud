package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UserVerificationCodeslistPageDTO;
import org.ar.common.pay.req.UserTextMessageReq;
import org.ar.manager.api.UsdtBuyOrderFeignClient;
import org.ar.manager.api.UserTextMessageFeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户验证码记录表 前端控制器
 * </p>
 *
 * @author
 * @since 2024-01-20
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/api/v1/userVerificationCodes", "/userVerificationCodes"})
@Api(description = "用户验证码控制器")
public class UserVerificationCodesController {

    private final UserTextMessageFeignClient userTextMessageFeignClient;

    @PostMapping("/listPage")
    @ApiOperation(value = "获取用户验证码列表")
    public RestResult<List<UserVerificationCodeslistPageDTO>> listPage(@RequestBody @ApiParam UserTextMessageReq userTextMessageReq) {
        RestResult<List<UserVerificationCodeslistPageDTO>> list = userTextMessageFeignClient.listPage(userTextMessageReq);
        return list;
    }
}

