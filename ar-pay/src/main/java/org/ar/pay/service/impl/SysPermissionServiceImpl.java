package org.ar.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.AssertUtil;
import org.ar.pay.config.AdminConfig;
import org.ar.pay.config.AdminMapStruct;
import org.ar.pay.entity.SysPermission;
import org.ar.pay.entity.SysRolePermission;
import org.ar.pay.mapper.SysPermissionMapper;
import org.ar.pay.req.CommonReq;
import org.ar.pay.req.SavePermissionReq;
import org.ar.pay.service.ISysPermissionService;
import org.ar.pay.service.ISysRolePermissionService;
import org.ar.pay.vo.SysPermissionVO;
import org.ar.pay.vo.SysServiceVO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;


@Service
@RequiredArgsConstructor
@Slf4j
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements ISysPermissionService {

    private final RedisTemplate redisTemplate;
    private final AdminMapStruct adminMapStruct;
    private final AdminConfig adminConfig;
    private final ISysRolePermissionService rolePermissionService;


    @Override
    public void updatePermission(SavePermissionReq req) {
        AssertUtil.notEmpty(req.getId(), ResultCode.PARAM_VALID_FAIL);
        SysPermission sysPermission = new SysPermission();
        BeanUtils.copyProperties(req, sysPermission);
        String urlPerm = String.format(GlobalConstants.ADMIN_URL_PERM, req.getMethod(), req.getServiceName(), req.getUrl());
        sysPermission.setUrlPerm(urlPerm);
        updateById(sysPermission);
    }

    @Override
    public void createPermission(SavePermissionReq req) {
        SysPermission sysPermission = new SysPermission();
        BeanUtils.copyProperties(req, sysPermission);
        String urlPerm = String.format(GlobalConstants.ADMIN_URL_PERM, req.getMethod(), req.getServiceName(), req.getUrl());
        sysPermission.setUrlPerm(urlPerm);
        save(sysPermission);
    }

    @Override
    public List<SysPermissionVO> listByMenuId(Long menuId) {
        List<SysPermission> sysPermissions = lambdaQuery().eq(SysPermission::getMenuId, menuId).list();
        if (CollectionUtil.isNotEmpty(sysPermissions)) {
            return adminMapStruct.sysPermissionToSysPermissionVO(sysPermissions);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean refreshPermRolesRules() {
        redisTemplate.delete(Arrays.asList(GlobalConstants.URL_PERM_ROLES_KEY));
        List<SysPermission> permissions = this.listPermRoles();
        if (CollectionUtil.isNotEmpty(permissions)) {
            //
            List<SysPermission> urlPermList = permissions.stream()
                    .filter(item -> StrUtil.isNotBlank(item.getUrlPerm()))
                    .collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(urlPermList)) {
                Map<String, List<String>> urlPermRoles = new HashMap<>();
                urlPermList.stream().forEach(item -> {
                    String perm = item.getUrlPerm();
                    List<String> roles = item.getRoles();
                    urlPermRoles.put(perm, roles);
                });
                redisTemplate.opsForHash().putAll(GlobalConstants.URL_PERM_ROLES_KEY, urlPermRoles);
            }
        }
        return true;
    }

    @Override
    public List<SysPermission> listPermRoles() {
        return this.baseMapper.listPermRoles();
    }

    @Override
    public List<SysServiceVO> getServices() {
        List<String> services = adminConfig.getServices();
        if (CollectionUtil.isNotEmpty(services)) {
            List<String> validServices = services.stream().filter(service -> service.split(",").length == 2).collect(Collectors.toList());
            return validServices.stream().map(service -> {
                String[] nameCode = service.split(",");
                SysServiceVO sysServiceVO = new SysServiceVO();
                sysServiceVO.setServiceCode(nameCode[0]);
                sysServiceVO.setServiceName(nameCode[1]);
                return sysServiceVO;
            }).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void deletePermissions(List<Long> ids) {
        this.baseMapper.deleteBatchIds(ids);
    }

    @Override
    public List<Long> listRolePermission(Long roleId) {
        List<SysRolePermission> list = rolePermissionService.lambdaQuery().eq(SysRolePermission::getRoleId, roleId).list();
        if (CollectionUtil.isNotEmpty(list)) {
            return list.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void updateRolePermission(Long roleId, CommonReq req) {

        rolePermissionService.getBaseMapper().delete(rolePermissionService.lambdaQuery().eq(SysRolePermission::getRoleId, roleId).getWrapper());

        if (CollectionUtil.isNotEmpty(req.getIds())) {
            List<SysRolePermission> roleMenus = req.getIds().stream().map(menuId -> new SysRolePermission(menuId, roleId)).collect(Collectors.toList());
            rolePermissionService.saveBatch(roleMenus);
        }
    }

}
