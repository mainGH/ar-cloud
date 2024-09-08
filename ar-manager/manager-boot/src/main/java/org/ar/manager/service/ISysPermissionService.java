package org.ar.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.entity.SysPermission;
import org.ar.manager.req.CommonReq;
import org.ar.manager.req.SavePermissionReq;
import org.ar.manager.vo.SysPermissionVO;
import org.ar.manager.vo.SysServiceVO;

import java.util.List;

public interface ISysPermissionService extends IService<SysPermission> {
    boolean refreshPermRolesRules();

    List<SysPermission> listPermRoles();


    List<SysPermissionVO> listByMenuId(Long menuId);


    SysPermission createPermission(SavePermissionReq req);


    SysPermission updatePermission(SavePermissionReq req);


    List<SysServiceVO> getServices();

    void deletePermissions(List<Long> ids);


    List<Long> listRolePermission(Long roleId);


    List<SysPermission> updateRolePermission(Long roleId, CommonReq req);
}
