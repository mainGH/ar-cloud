package org.ar.manager.config;


import org.ar.manager.entity.SysPermission;
import org.ar.manager.entity.SysRole;
import org.ar.manager.entity.SysUser;
import org.ar.manager.vo.SysPermissionVO;
import org.ar.manager.vo.SysRoleSelectVO;
import org.ar.manager.vo.SysRoleVO;
import org.ar.manager.vo.SysUserVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminMapStruct {
    List<SysRoleSelectVO> sysRoleToSysRoleVO(List<SysRole> sysRoles);
    List<SysUserVO> sysUserToSysUserVO(List<SysUser> sysUsers);
    List<SysRoleVO> sysRoleToSysRoleListVO(List<SysRole> sysRoles);
    List<SysPermissionVO> sysPermissionToSysPermissionVO(List<SysPermission> permissions);
}
