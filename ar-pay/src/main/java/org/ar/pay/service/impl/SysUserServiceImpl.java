package org.ar.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.common.pay.dto.UserAuthDTO;
import org.ar.common.web.utils.UserContext;
import org.ar.pay.config.AdminMapStruct;
import org.ar.pay.entity.*;
import org.ar.pay.mapper.SysUserMapper;
import org.ar.pay.req.SaveUserReq;
import org.ar.pay.req.UserListPageReq;
import org.ar.pay.service.*;
import org.ar.pay.util.PageUtils;
import org.ar.pay.vo.SysUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    private final PasswordEncoder passwordEncoder;
    private final ISysUserRoleService userRoleService;
    private final ISysRoleMenuService roleMenuService;
    private final ISysRolePermissionService rolePermissionService;
    private final ISysPermissionService permissionService;
    private final AdminMapStruct adminMapStruct;

    @Override
    public UserAuthDTO getByUsername(String username) {
        UserAuthDTO userAuthInfo = this.baseMapper.getByUsername(username);
        return userAuthInfo;
    }


    @Override
    public void createUser(SaveUserReq req) {
        // 生成密码
        String passwd = passwordEncoder.encode(GlobalConstants.USER_DEFAULT_PASSWORD);
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(req, sysUser);
        sysUser.setPassword(passwd);
        save(sysUser);
        // 维护角色关系
        saveUserRoles(req.getRoleIds(), sysUser.getId());
    }

    private void saveUserRoles(List<Long> roleIds, Long userId) {
        if (CollectionUtil.isNotEmpty(roleIds)) {
            List<SysUserRole> sysUserRoles = new ArrayList<>();
            roleIds.forEach(roleId -> {
                sysUserRoles.add(new SysUserRole(userId, roleId));
            });
            userRoleService.saveBatch(sysUserRoles);
        }
    }

    @Override
    public SysUserVO userDetail(Long userId) {
        SysUser sysUser = lambdaQuery().eq(SysUser::getId, userId).one();
        AssertUtil.notEmpty(sysUser, ResultCode.USERNAME_OR_PASSWORD_ERROR);
        SysUserVO sysUserVO = new SysUserVO();
        BeanUtils.copyProperties(sysUser, sysUserVO);
        // 查询绑定的角色IDs
        List<Long> roleIds = userRoleService.selectRoleIds(userId);
        sysUserVO.setRoleIds(roleIds);
        return sysUserVO;
    }

    @Override
    public void updateUserInfo(SaveUserReq req, Long userId) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(req, sysUser);
        lambdaUpdate().eq(SysUser::getId, userId).update(sysUser);
        // 维护角色列表
        userRoleService.deleteByUserId(userId);
        saveUserRoles(req.getRoleIds(), userId);
    }

    @Override
    public void mulDeleteUsers(List<Long> userIds) {
        // 删除用户信息
        lambdaUpdate().in(SysUser::getId, userIds).set(SysUser::getDeleted, GlobalConstants.STATUS_ON).update();
        // 删除用户关联的角色
        userRoleService.getBaseMapper().delete(userRoleService.lambdaQuery().in(SysUserRole::getUserId, userIds).getWrapper());
    }

    @Override
    public PageReturn<SysUserVO> listPage(UserListPageReq req) {
        Page<SysUser> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<SysUser> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(SysUser::getStatus, GlobalConstants.STATUS_ON);
        if (StringUtils.isNoneBlank(req.getKeyword())) {
            lambdaQuery.like(SysUser::getUsername, req.getKeyword()).or().like(SysUser::getNickname, req.getKeyword());
        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<SysUser> records = page.getRecords();
        List<SysUserVO> sysUserVOS = adminMapStruct.sysUserToSysUserVO(records);
        return PageUtils.flush(page, sysUserVOS);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        lambdaUpdate().set(SysUser::getStatus, status).eq(SysUser::getId, userId).update();
    }

    @Override
    public SysUserVO currentUserInfo() {
        Long currentUserId = UserContext.getCurrentUserId();
        AssertUtil.notEmpty(currentUserId, ResultCode.RELOGIN);
        SysUserVO sysUserVO = userDetail(currentUserId);
        AssertUtil.notEmpty(sysUserVO, ResultCode.USERNAME_OR_PASSWORD_ERROR);
        // 查询绑定的菜单
        List<Long> roleIds = userRoleService.selectRoleIds(currentUserId);
        List<SysRoleMenu> roleMenus = roleMenuService.lambdaQuery().in(SysRoleMenu::getRoleId, roleIds).list();
        sysUserVO.setMenuIds(roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList()));
        // 获取权限
        List<SysRolePermission> rolePermissions = rolePermissionService.lambdaQuery().in(SysRolePermission::getRoleId, roleIds).list();
        sysUserVO.setPermissions(Arrays.asList());
        if (CollectionUtil.isNotEmpty(rolePermissions)) {
            List<Long> permissionIds = rolePermissions.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
            List<SysPermission> sysPermissions = permissionService.lambdaQuery().in(SysPermission::getId, permissionIds).list();
            if (CollectionUtil.isNotEmpty(sysPermissions)) {
                List<String> btnSigns = sysPermissions.stream().map(SysPermission::getBtnSign).collect(Collectors.toList());
                sysUserVO.setPermissions(btnSigns);
            }
        }

        return sysUserVO;
    }
}
