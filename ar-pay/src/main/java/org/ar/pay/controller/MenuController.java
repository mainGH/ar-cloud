package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.pay.req.CommonReq;
import org.ar.pay.req.SaveMenuReq;
import org.ar.pay.service.ISysMenuService;
import org.ar.pay.vo.SysMenuSelectVO;
import org.ar.pay.vo.SysMenuVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/menu")
@Slf4j
@RequiredArgsConstructor
@Api(description = "菜单控制器")
public class MenuController {
    private final ISysMenuService sysMenuService;

    /**
     * 菜单详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "获取菜单详情")
    public RestResult<SysMenuVO> getDetail(@PathVariable Long id) {
        SysMenuVO sysMenuVO = sysMenuService.getDetail(id);
        return RestResult.ok(sysMenuVO);
    }

    /**
     * 获取菜单数据列表
     *
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "获取菜单数据列表")
    public RestResult<List<SysMenuVO>> list() {
        List<SysMenuVO> sysMenuVOS = sysMenuService.loadMenus();
        return RestResult.ok(sysMenuVOS);
    }

    /**
     * 创建菜单
     */
    @PostMapping
    @ApiOperation(value = "创建菜单")
    public RestResult<Object> createMenu(@Validated @RequestBody SaveMenuReq req) {
        sysMenuService.createMenu(req);
        return RestResult.ok();
    }

    /**
     * 菜单树
     */
    @GetMapping("/listTree")
    @ApiOperation(value = "菜单树")
    public RestResult<List<SysMenuVO>> listTree() {
        List<SysMenuVO> menuVOS = sysMenuService.listTree();
        return RestResult.ok(menuVOS);
    }

    /**
     * 更新菜单
     */
    @PutMapping
    @ApiOperation(value = "更新菜单")
    public RestResult updateMenu(@Validated @RequestBody SaveMenuReq req) {
        sysMenuService.updateMenu(req);
        return RestResult.ok();
    }

    /**
     * 更新菜单
     */
    @PatchMapping("/{id}/{status}")
    @ApiOperation(value = "更新菜单状态")
    public RestResult updateMenuStatus(@PathVariable Long id, @PathVariable int status) {
        sysMenuService.updateMenuStatus(id, status);
        return RestResult.ok();
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{ids}")
    @ApiOperation(value = "删除菜单")
    public RestResult deletes(@PathVariable List<Long> ids) {
        sysMenuService.deletes(ids);
        return RestResult.ok();
    }

    /**
     * 菜单select
     */

    @GetMapping("/select/{status}")
    @ApiOperation(value = "菜单select")
    public RestResult<List<SysMenuSelectVO>> select(@PathVariable int status) {
        List<SysMenuSelectVO> selectVOS = sysMenuService.select(status);
        return RestResult.ok(selectVOS);
    }

    /**
     * 查询角色绑定的菜单
     */
    @GetMapping("/role/{roleId}")
    @ApiOperation(value = "查询角色绑定的菜单")
    public RestResult<List<Long>> listRoleMenu(@PathVariable Long roleId) {
        List<Long> menuIds = sysMenuService.listRoleMenu(roleId);
        return RestResult.ok(menuIds);
    }


    /**
     * 更新角色绑定的菜单
     */
    @PutMapping("/role/{roleId}")
    @ApiOperation(value = "更新角色绑定的菜单")
    public RestResult updateRoleMenu(@PathVariable Long roleId, @RequestBody CommonReq req) {
        sysMenuService.updateRoleMenu(roleId, req);
        return RestResult.ok();
    }

    /**
     * 获取当前用户绑定的菜单
     */

    @GetMapping("/currentUser")
    @ApiOperation(value = "获取当前用户绑定的菜单")
    public RestResult<List<Long>> currentUser() {
        List<Long> menus = sysMenuService.currentUser();
        return RestResult.ok(menus);

    }
}
