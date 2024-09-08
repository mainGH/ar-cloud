package org.ar.pay.service.impl;


import lombok.RequiredArgsConstructor;
import org.ar.pay.entity.SysRolePermission;
import org.ar.pay.mapper.SysRolePermissionMapper;
import org.ar.pay.service.ISysRolePermissionService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;




@Service
@RequiredArgsConstructor
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission> implements ISysRolePermissionService {


}
