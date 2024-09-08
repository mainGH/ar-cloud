package org.ar.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.pay.entity.SysMenu;
import org.ar.pay.entity.SysRoleMenu;
import org.ar.pay.mapper.SysMenuMapper;
import org.ar.pay.req.CommonReq;
import org.ar.pay.req.SaveMenuReq;
import org.ar.pay.service.ISysMenuService;
import org.ar.pay.service.ISysRoleMenuService;
import org.ar.pay.service.ISysUserRoleService;
import org.ar.pay.vo.SysMenuSelectVO;
import org.ar.pay.vo.SysMenuVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.ar.common.web.utils.UserContext;


@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements ISysMenuService {
    private final ISysRoleMenuService roleMenuService;
    private final ISysUserRoleService userRoleService;

    @Override
    public void deletes(List<Long> ids) {
        while (CollectionUtil.isNotEmpty(ids)) {
            this.baseMapper.deleteBatchIds(ids);
            List<SysMenu> sysMenus = lambdaQuery().in(SysMenu::getParentId, ids).select(SysMenu::getId).list();
            if (CollectionUtil.isNotEmpty(sysMenus)) {
                ids = sysMenus.stream().map(SysMenu::getId).collect(Collectors.toList());
            } else {
                ids = null;
            }
        }
    }

    @Override
    public void updateMenu(SaveMenuReq req) {
        AssertUtil.notEmpty(req.getId(), ResultCode.PARAM_VALID_FAIL);
        SysMenu sysMenu = new SysMenu();
        BeanUtils.copyProperties(req, sysMenu);
        updateById(sysMenu);
    }

    @Override
    public List<SysMenuVO> listTree() {
        List<SysMenu> menus = lambdaQuery()
                .eq(SysMenu::getVisible, GlobalConstants.STATUS_ON)
                .orderByAsc(SysMenu::getSort).list();
        if (CollectionUtil.isNotEmpty(menus)) {
            return flushMenuVOs(menus, GlobalConstants.ROOT_MENU_ID);
        }
        return Collections.emptyList();
    }

    @Override
    public void createMenu(SaveMenuReq req) {
        SysMenu sysMenu = new SysMenu();
        BeanUtils.copyProperties(req, sysMenu);
        save(sysMenu);
    }

    @Override
    public List<SysMenuVO> loadMenus() {
        List<SysMenu> menus = lambdaQuery().orderByAsc(SysMenu::getSort).list();
        if (CollectionUtil.isNotEmpty(menus)) {
            List<SysMenuVO> menuVOS = flushMenuVOs(menus, GlobalConstants.ROOT_MENU_ID);
            return menuVOS;
        }
        return Collections.emptyList();
    }

    /**
     * 组装菜单
     *
     * @param menus
     * @param parentId
     * @return
     */
    private List<SysMenuVO> flushMenuVOs(List<SysMenu> menus, Long parentId) {
        List<SysMenuVO> childMenus = new ArrayList<>();
        for (SysMenu sysMenu : menus) {
            if (parentId.equals(sysMenu.getParentId())) {
                SysMenuVO sysMenuVO = new SysMenuVO();
                BeanUtils.copyProperties(sysMenu, sysMenuVO);
                childMenus.add(sysMenuVO);
            }
        }
        if (CollectionUtil.isNotEmpty(childMenus)) {
            for (SysMenuVO sysMenuVO : childMenus) {
                List<SysMenuVO> childChildMenus = flushMenuVOs(menus, sysMenuVO.getId());
                sysMenuVO.setChildren(childChildMenus);
            }
            return childMenus;
        }
        return Collections.emptyList();
    }

    /**
     * 组装菜单select
     *
     * @param menus
     * @param parentId
     * @return
     */
    private List<SysMenuSelectVO> flushMenuSelectVOs(List<SysMenu> menus, Long parentId) {
        List<SysMenuSelectVO> childMenus = new ArrayList<>();
        for (SysMenu sysMenu : menus) {
            if (parentId.equals(sysMenu.getParentId())) {
                SysMenuSelectVO sysMenuVO = new SysMenuSelectVO();
                sysMenuVO.setId(sysMenu.getId());
                sysMenuVO.setLabel(sysMenu.getName());
                childMenus.add(sysMenuVO);
            }
        }
        if (CollectionUtil.isNotEmpty(childMenus)) {
            for (SysMenuSelectVO sysMenuVO : childMenus) {
                List<SysMenuSelectVO> childChildMenus = flushMenuSelectVOs(menus, sysMenuVO.getId());
                sysMenuVO.setChildren(childChildMenus);
            }
            return childMenus;
        }
        return Collections.emptyList();
    }

    @Override
    public List<SysMenuSelectVO> select(int status) {
        List<SysMenu> menus = lambdaQuery()
                .eq(status != -1, SysMenu::getVisible, status)
                .orderByAsc(SysMenu::getSort).list();
        if (CollectionUtil.isNotEmpty(menus)) {
            List<SysMenuSelectVO> menuVOS = flushMenuSelectVOs(menus, GlobalConstants.ROOT_MENU_ID);
            return menuVOS;
        }
        return Collections.emptyList();
    }

    @Override
    public SysMenuVO getDetail(Long id) {
        SysMenu sysMenu = getById(id);
        AssertUtil.notEmpty(sysMenu, ResultCode.DATA_NOT_FOUND);
        SysMenuVO sysMenuVO = new SysMenuVO();
        BeanUtils.copyProperties(sysMenu, sysMenuVO);
        return sysMenuVO;
    }

    @Override
    public List<Long> listRoleMenu(Long roleId) {
        List<SysRoleMenu> list = roleMenuService.lambdaQuery().eq(SysRoleMenu::getRoleId, roleId).list();
        if (CollectionUtil.isNotEmpty(list)) {
            return list.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void updateRoleMenu(Long roleId, CommonReq req) {
        // 删除之前绑定的信息
        roleMenuService.getBaseMapper().delete(roleMenuService.lambdaQuery().eq(SysRoleMenu::getRoleId, roleId).getWrapper());
        // 添加新的绑定信息
        if (CollectionUtil.isNotEmpty(req.getIds())) {
            List<SysRoleMenu> roleMenus = req.getIds().stream().map(menuId -> new SysRoleMenu(menuId, roleId)).collect(Collectors.toList());
            roleMenuService.saveBatch(roleMenus);
        }
    }

    @Override
    public void updateMenuStatus(Long id, int status) {
        lambdaUpdate().eq(SysMenu::getId, id).set(SysMenu::getVisible, status).update();
    }

    @Override
    public List<Long> currentUser() {
        List<SysMenu> menus = lambdaQuery().orderByAsc(SysMenu::getSort).list();
        if (CollectionUtil.isNotEmpty(menus)) {
            // 过滤当前用户角色绑定菜单
            Long currentUserId = UserContext.getCurrentUserId();
            List<Long> roleIds = userRoleService.selectRoleIds(currentUserId);
            if (CollectionUtil.isNotEmpty(roleIds)) {
                List<SysRoleMenu> list = roleMenuService.lambdaQuery().in(SysRoleMenu::getRoleId, roleIds).list();
                if (CollectionUtil.isNotEmpty(list)) {
                    return list.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
                }
            }
        }
        return Collections.emptyList();
    }
}
