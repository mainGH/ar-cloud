package org.ar.pay.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.pay.entity.MerchantRole;
import org.ar.pay.entity.SysUser;
import org.ar.pay.mapper.MerchantRoleMapper;
import org.ar.pay.service.IMerchantRoleService;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MerchantRoleServiceImpl extends ServiceImpl<MerchantRoleMapper, MerchantRole> implements IMerchantRoleService {
    @Override
    public List<Long> selectRoleIds(Long userId) {
        List<MerchantRole> userRoles = lambdaQuery().eq(MerchantRole::getMerchantId, userId).list();
        if (CollectionUtil.isNotEmpty(userRoles)) {
            return userRoles.stream().map(MerchantRole::getRoleId).collect(Collectors.toList());
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void deleteByUserId(Long userId) {
        this.baseMapper.delete(lambdaQuery().eq(MerchantRole::getMerchantId,userId).getWrapper());
    }


}
