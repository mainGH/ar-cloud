package org.ar.pay.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.pay.entity.SysPermission;
import org.ar.pay.req.CommonReq;
import org.ar.pay.req.SavePermissionReq;
import org.ar.pay.vo.SysPermissionVO;
import org.ar.pay.vo.SysServiceVO;

import java.util.List;

public interface ISysPermissionService extends IService<SysPermission> {

    boolean refreshPermRolesRules();

    List<SysPermission> listPermRoles();


    List<SysPermissionVO> listByMenuId(Long menuId);


    void createPermission(SavePermissionReq req);


    void updatePermission(SavePermissionReq req);


    List<SysServiceVO> getServices();

    void deletePermissions(List<Long> ids);


    List<Long> listRolePermission(Long roleId);


    void updateRolePermission(Long roleId, CommonReq req);
}
