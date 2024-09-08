package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.req.RoleListPageReq;
import org.ar.pay.req.SaveSysRoleReq;
import org.ar.pay.service.ISysRoleService;
import org.ar.pay.vo.SysRoleSelectVO;
import org.ar.pay.vo.SysRoleVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/role")
@Slf4j
@RequiredArgsConstructor
@Api(description = "角色控制器")
public class RoleController {
    private final ISysRoleService roleService;

    /**
     * 角色列表select
     */
    @GetMapping("/select")
    @ApiOperation(value = "角色列表select")
    public RestResult<List<SysRoleSelectVO>> roleSelect() {
        List<SysRoleSelectVO> roleVOList = roleService.roleSelect();
        return RestResult.ok(roleVOList);
    }

    /**
     * 列表分页
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "角色列表分页")
    public RestResult<List<SysRoleVO>> listPage(RoleListPageReq req) {
        PageReturn<SysRoleVO> roleVOAPage = roleService.listPage(req);
        return RestResult.page(roleVOAPage);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @ApiOperation(value = "创建角色")
    public RestResult createRole(@Validated @RequestBody SaveSysRoleReq req) {
        roleService.createRole(req);
        return RestResult.ok();
    }

    /**
     * 更新角色
     */
    @PutMapping
    @ApiOperation(value = "更新角色")
    public RestResult updateRole(@Validated @RequestBody SaveSysRoleReq req) {
        roleService.updateRole(req);
        return RestResult.ok();
    }

    /**
     * 更新角色状态
     */
    @PatchMapping("/updateStatus/{id}/{status}")
    @ApiOperation(value = "更新角色状态")
    public RestResult updateRole(@PathVariable Long id, @PathVariable int status) {
        roleService.updateStatus(id, status);
        return RestResult.ok();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{ids}")
    @ApiOperation(value = "删除角色")
    public RestResult deletes(@PathVariable List<Long> ids) {
        roleService.deletes(ids);
        return RestResult.ok();
    }

}
