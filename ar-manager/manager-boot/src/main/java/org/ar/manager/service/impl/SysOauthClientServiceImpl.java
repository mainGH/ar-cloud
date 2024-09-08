package org.ar.manager.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.manager.entity.SysOauthClient;
import org.ar.manager.mapper.SysOauthClientMapper;
import org.ar.manager.service.ISysOauthClientService;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SysOauthClientServiceImpl extends ServiceImpl<SysOauthClientMapper, SysOauthClient> implements ISysOauthClientService {

}
