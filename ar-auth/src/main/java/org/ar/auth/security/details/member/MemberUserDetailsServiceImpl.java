package org.ar.auth.security.details.member;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.auth.comm.enums.PasswordEncoderTypeEnum;
import org.ar.auth.security.details.user.SysUserDetails;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.pay.api.UserFeignClient;
import org.ar.common.pay.dto.UserAuthDTO;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service("memberUserDetailsService")
@Slf4j
@RequiredArgsConstructor
public class MemberUserDetailsServiceImpl implements UserDetailsService {
    private final UserFeignClient userFeignClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 后面从管理端获取用户信息
        RestResult<UserAuthDTO> result = userFeignClient.getUserByUsername(username);
        SysUserDetails userDetails = null;
        if (RestResult.ok().getCode().equals(result.getCode())) {
            UserAuthDTO user = result.getData();
            if (null != user) {
                userDetails = SysUserDetails.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .googleAuthSecret(user.getGooglesecret())
                        .authorities(handleRoles(user.getRoles()))
                        .enabled(user.getStatus() == 1)
                        .password(PasswordEncoderTypeEnum.BCRYPT.getPrefix() + user.getPassword())
                        .build();
            }
        }
        if (Objects.isNull(userDetails)) {
            throw new UsernameNotFoundException(ResultCode.USERNAME_OR_PASSWORD_ERROR.getMsg());
        } else if (!userDetails.isEnabled()) {
            throw new DisabledException("该账户已被禁用!");
        } else if (!userDetails.isAccountNonLocked()) {
            throw new LockedException("该账号已被锁定!");
        } else if (!userDetails.isAccountNonExpired()) {
            throw new AccountExpiredException("该账号已过期!");
        }
        return userDetails;
    }

    private Collection<SimpleGrantedAuthority> handleRoles(List<String> roles) {
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return authorities;
    }

}
