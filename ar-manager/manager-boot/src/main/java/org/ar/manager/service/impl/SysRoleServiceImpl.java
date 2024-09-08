package org.ar.manager.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.manager.config.AdminMapStruct;
import org.ar.manager.entity.BiPaymentOrder;
import org.ar.manager.entity.SysRole;
import org.ar.manager.mapper.SysRoleMapper;
import org.ar.manager.req.RoleListPageReq;
import org.ar.manager.req.SaveSysRoleReq;
import org.ar.manager.service.ISysRoleMenuService;
import org.ar.manager.service.ISysRolePermissionService;
import org.ar.manager.service.ISysRoleService;
import org.ar.manager.util.PageUtils;
import org.ar.manager.vo.SysRoleSelectVO;
import org.ar.manager.vo.SysRoleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.ar.manager.entity.SysRolePermission;
import org.ar.manager.entity.SysRoleMenu;
import org.ar.common.core.utils.StringUtils;
import java.util.Collections;
import java.util.List;


@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    private final AdminMapStruct adminMapStruct;
    private final ISysRolePermissionService rolePermissionService;
    private final ISysRoleMenuService roleMenuService;
    private final SysRoleMapper sysRoleMapper;

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
        if (!StringUtils.isEmpty(req.getKeyword())) {
            lambdaQuery.like(SysRole::getName, req.getKeyword());
        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<SysRole> records = page.getRecords();
        List<SysRoleVO> sysUserVOS = adminMapStruct.sysRoleToSysRoleListVO(records);
        return PageUtils.flush(page, sysUserVOS);
    }

    @Override
    public SysRole createRole(SaveSysRoleReq req) {
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(req, sysRole);
        SysRole sysRoleTmp = sysRoleMapper.selectOneByName(req.getName());

        if(ObjectUtils.isNotEmpty(sysRoleTmp)){
            sysRoleMapper.updateRoleById(sysRoleTmp.getId());
        }else {
            save(sysRole);
        }

        return sysRole;
    }

    @Override
    public SysRole updateRole(SaveSysRoleReq req) {
        AssertUtil.notEmpty(req.getId(), ResultCode.PARAM_VALID_FAIL);
        SysRole sysRole = new SysRole();
        BeanUtils.copyProperties(req, sysRole);
        updateById(sysRole);
        return sysRole;
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
    public SysRole updateStatus(Long id, int status) {
        lambdaUpdate().eq(SysRole::getId, id).set(SysRole::getStatus, status).update();
        SysRole sysRole = this.lambdaQuery().eq(SysRole::getId, id).one();
        return sysRole;
    }
}
