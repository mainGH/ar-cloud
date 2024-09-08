package org.ar.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.utils.AssertUtil;
import org.ar.pay.config.AdminMapStruct;
import org.ar.pay.entity.SysRole;
import org.ar.pay.mapper.SysRoleMapper;
import org.ar.pay.req.RoleListPageReq;
import org.ar.pay.req.SaveSysRoleReq;
import org.ar.pay.service.ISysRoleMenuService;
import org.ar.pay.service.ISysRolePermissionService;
import org.ar.pay.service.ISysRoleService;
import org.ar.pay.util.PageUtils;
import org.ar.pay.vo.SysRoleSelectVO;
import org.ar.pay.vo.SysRoleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.ar.common.core.result.ResultCode;
import org.ar.pay.entity.*;

import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    private final AdminMapStruct adminMapStruct;
    private final ISysRolePermissionService rolePermissionService;
    private final ISysRoleMenuService roleMenuService;

    @Override
    public List<SysRoleSelectVO> roleSelect() {
        List<SysRole> sysRoles = lambdaQuery().eq(SysRole::getStatus, GlobalConstants.STATUS_ON).select(SysRole::getId, SysRole::getName).list();
        if (CollectionUtil.isNotEmpty(sysRoles)) {
            return adminMapStruct.sysRoleToSysRoleVO(sysRoles);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public PageReturn<SysRoleVO> listPage(RoleListPageReq req) {
        Page<SysRole> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<SysRole> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByAsc(SysRole::getSort);
        lambdaQuery.eq(SysRole::getDeleted, GlobalConstants.STATUS_OFF);
        if (StringUtils.isNoneBlank(req.getKeyword())) {
            lambdaQuery.like(SysRole::getName, req.getKeyword());
        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<SysRole> records = page.getRecords();
        List<SysRoleVO> sysUserVOS = adminMapStruct.sysRoleToSysRoleListVO(records);
        return PageUtils.flush(page, sysUserVOS);
    }

    @Override
    public void createRole(SaveSysRoleReq req) {
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(req, sysRole);
        save(sysRole);
    }

    @Override
    public void updateRole(SaveSysRoleReq req) {
        AssertUtil.notEmpty(req.getId(), ResultCode.PARAM_VALID_FAIL);
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(req, sysRole);
        updateById(sysRole);
    }

    @Override
    public void deletes(List<Long> ids) {
        lambdaUpdate().in(SysRole::getId, ids).set(SysRole::getDeleted, GlobalConstants.STATUS_ON).update();
        // 删除关联的权限信息
        rolePermissionService.getBaseMapper().delete(rolePermissionService.lambdaQuery().in(SysRolePermission::getRoleId, ids).getWrapper());
        // 删除关联的菜单信息
        roleMenuService.getBaseMapper().delete(roleMenuService.lambdaQuery().in(SysRoleMenu::getRoleId, ids).getWrapper());
    }

    @Override
    public void updateStatus(Long id, int status) {
        lambdaUpdate().eq(SysRole::getId, id).set(SysRole::getStatus, status).update();
    }
}
