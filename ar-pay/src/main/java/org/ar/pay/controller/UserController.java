package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.pay.req.SaveUserReq;
import org.ar.pay.req.UserListPageReq;
import org.ar.pay.service.IMerchantInfoService;
import org.ar.pay.service.ISysUserService;
import org.ar.pay.vo.SysUserVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@RestController
@RequestMapping(value = {"/api/v1/users", "/user"})
@Slf4j
@RequiredArgsConstructor
@Api(description = "用户控制器")
public class UserController {

    private final ISysUserService sysUserService;
    private final IMerchantInfoService merchantInfoService;

    /**
     * 获取用户信息
     */
    @GetMapping("/username/{username}")
    @ApiOperation(value = "获取用户信息")
    public RestResult<UserAuthDTO> getUserByUsername(@NotBlank(message = "username 不能为空") @PathVariable String username) {
        UserAuthDTO user = sysUserService.getByUsername(username);
        return RestResult.ok(user);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    @ApiOperation(value = "获取当前用户信息")
    public RestResult<SysUserVO> currentUserInfo() {
        SysUserVO sysUserVO = sysUserService.currentUserInfo();
        return RestResult.ok(sysUserVO);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @ApiOperation(value = "创建用户")
    public RestResult createUser(@Validated @RequestBody SaveUserReq req) {
        sysUserService.createUser(req);
        return RestResult.ok();
    }

    /**
     * 用户详情
     */
    @GetMapping("/{userId}")
    @ApiOperation(value = "用户详情")
    public RestResult<SysUserVO> userDetail(@NotNull(message = "userId 不能为空") @PathVariable Long userId) {
        SysUserVO sysUserVO = sysUserService.userDetail(userId);
        return RestResult.ok(sysUserVO);
    }

    /**
     * 更新用户信息
     */

    @PutMapping("/{userId}")
    @ApiOperation(value = "更新用户信息")
    public RestResult updateUserInfo(@Validated @RequestBody SaveUserReq req, @NotNull(message = "userId 不能为空") @PathVariable Long userId) {
        sysUserService.updateUserInfo(req, userId);
        return RestResult.ok();
    }

    /**
     * 批量删除用户
     */

    @DeleteMapping("/{userIds}")
    @ApiOperation(value = "批量删除用户")
    public RestResult mulDeleteUsers(@Size(min = 1, message = "userIds 不能为空") @PathVariable List<Long> userIds) {
        sysUserService.mulDeleteUsers(userIds);
        return RestResult.ok();
    }

    /**
     * 用户分页列表
     */

    @GetMapping("/listPage")
    @ApiOperation(value = "用户分页列表")
    public RestResult<List<SysUserVO>> listPage(UserListPageReq req) {
        PageReturn<SysUserVO> sysUserVOPage = sysUserService.listPage(req);
        return RestResult.page(sysUserVOPage);
    }

    /**
     * 更新用户状态
     */

    @PatchMapping("/updateStatus/{userId}/{status}")
    @ApiOperation(value = "更新用户状态")
    public RestResult updateStatus(@NotNull(message = "userId 不能为空") @PathVariable Long userId, @NotNull(message = "status 不能为空") @PathVariable Integer status) {
        sysUserService.updateStatus(userId, status);
        return RestResult.ok();
    }

    /**
     * 获取会员用户信息
     */
    @GetMapping("/merchant/username/{username}")
    @ApiOperation(value = "获取会员用户信息")
    public RestResult<UserAuthDTO> getMemberUserByUsername(@PathVariable String username) {
        log.info("获取member user info。。。");
        UserAuthDTO user = merchantInfoService.getByUsername(username);
        return RestResult.ok(user);
    }
}
