package org.ar.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;

import org.ar.common.pay.dto.MemberUserAuthDTO;
import org.ar.manager.entity.MemberUser;
import org.ar.manager.mapper.MemberUserMapper;
import org.ar.manager.service.IMemberUserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberUserServiceImpl extends ServiceImpl<MemberUserMapper, MemberUser> implements IMemberUserService {
    @Override
    public MemberUserAuthDTO getByUsername(String username) {
        MemberUserAuthDTO memberUserAuthDTO = this.baseMapper.getByUsername(username);
        return memberUserAuthDTO;
    }

}