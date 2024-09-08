package org.ar.manager.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.manager.entity.SysRolePermission;
import org.ar.manager.mapper.SysRolePermissionMapper;
import org.ar.manager.service.ISysRolePermissionService;
import org.springframework.stereotype.Service;




@Service
@RequiredArgsConstructor
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> implements ISysRolePermissionService {


}
