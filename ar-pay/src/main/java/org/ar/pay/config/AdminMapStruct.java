package org.ar.pay.config;


import org.ar.pay.entity.SysPermission;
import org.ar.pay.entity.SysRole;
import org.ar.pay.entity.SysUser;
import org.ar.pay.vo.SysPermissionVO;
import org.ar.pay.vo.SysRoleSelectVO;
import org.ar.pay.vo.SysRoleVO;
import org.ar.pay.vo.SysUserVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminMapStruct {
    List<SysRoleSelectVO> sysRoleToSysRoleVO(List<SysRole> sysRoles);
    List<SysUserVO> sysUserToSysUserVO(List<SysUser> sysUsers);
    List<SysRoleVO> sysRoleToSysRoleListVO(List<SysRole> sysRoles);
    List<SysPermissionVO> sysPermissionToSysPermissionVO(List<SysPermission> permissions);
}
