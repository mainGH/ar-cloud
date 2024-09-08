package org.ar.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.pay.entity.SysUserRole;
import org.ar.pay.mapper.SysUserRoleMapper;
import org.ar.pay.service.ISysUserRoleService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery;


@Service
@RequiredArgsConstructor
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements ISysUserRoleService {
    @Override
    public List<Long> selectRoleIds(Long userId) {
        List<SysUserRole> userRoles = lambdaQuery().eq(SysUserRole::getUserId, userId).list();
        if (CollectionUtil.isNotEmpty(userRoles)) {
            return userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void deleteByUserId(Long userId) {
        this.baseMapper.delete(lambdaQuery().eq(SysUserRole::getUserId,userId).getWrapper());
    }
}
