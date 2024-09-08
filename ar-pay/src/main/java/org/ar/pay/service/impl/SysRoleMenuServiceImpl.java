package org.ar.pay.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;

import org.ar.pay.entity.SysRoleMenu;
import org.ar.pay.mapper.SysRoleMenuMapper;
import org.ar.pay.service.ISysRoleMenuService;
import org.springframework.stereotype.Service;




@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements ISysRoleMenuService {

}
