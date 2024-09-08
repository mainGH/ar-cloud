package org.ar.manager.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.manager.entity.SysRoleMenu;
import org.ar.manager.mapper.SysRoleMenuMapper;
import org.ar.manager.service.ISysRoleMenuService;
import org.springframework.stereotype.Service;




@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements ISysRoleMenuService {

}
