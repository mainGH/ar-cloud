package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.pay.req.CommonReq;
import org.ar.pay.req.SavePermissionReq;
import org.ar.pay.service.ISysPermissionService;
import org.ar.pay.vo.SysPermissionVO;
import org.ar.pay.vo.SysServiceVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/permission")
@Slf4j
@RequiredArgsConstructor
@Api(description = "权限控制器")
public class PermissionController {
    private final ISysPermissionService permissionService;

    /**
     * 权限列表
     */
    @GetMapping("/list/{menuId}")
    @ApiOperation(value = "获取权限列表")
    public RestResult<List<SysPermissionVO>> list(@PathVariable Long menuId) {
        List<SysPermissionVO> permissionVOS = permissionService.listByMenuId(menuId);
        return RestResult.ok(permissionVOS);
    }

    /**
     * 创建权限
     */
    @PostMapping
    @ApiOperation(value = "创建权限")
    public RestResult createPermission(@Validated @RequestBody SavePermissionReq req) {
        permissionService.createPermission(req);
        return RestResult.ok();
    }

    /**
     * 更新权限
     */
    @PutMapping
    @ApiOperation(value = "更新权限")
    public RestResult updatePermission(@Validated @RequestBody SavePermissionReq req) {
        permissionService.updatePermission(req);
        return RestResult.ok();
    }

    /**
     * 获取服务列表
     */
    @GetMapping("/services")
    @ApiOperation(value = "获取服务列表")
    public RestResult<List<SysServiceVO>> getServices() {
        List<SysServiceVO> sysServiceVOS = permissionService.getServices();
        return RestResult.ok(sysServiceVOS);
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{ids}")
    @ApiOperation(value = "删除权限")
    public RestResult getServices(@PathVariable List<Long> ids) {
        permissionService.deletePermissions(ids);
        return RestResult.ok();
    }

    /**
     * 查询角色绑定的权限
     */
    @GetMapping("/role/{roleId}")
    @ApiOperation(value = "查询角色绑定的权限")
    public RestResult<List<Long>> listRolePermission(@PathVariable Long roleId) {
        List<Long> permissions = permissionService.listRolePermission(roleId);
        return RestResult.ok(permissions);
    }

    /**
     * 更新角色绑定的权限
     */
    @PutMapping("/role/{roleId}")
    @ApiOperation(value = "更新角色绑定的权限")
    public RestResult updateRolePermission(@PathVariable Long roleId, @RequestBody CommonReq req) {
        permissionService.updateRolePermission(roleId, req);
        return RestResult.ok();
    }
}
