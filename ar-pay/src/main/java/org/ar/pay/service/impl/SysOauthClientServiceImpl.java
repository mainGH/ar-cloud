package org.ar.pay.service.impl;



import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.pay.entity.SysOauthClient;
import org.ar.pay.mapper.SysOauthClientMapper;
import org.springframework.stereotype.Service;
import org.ar.pay.service.ISysOauthClientService;


@Service
@RequiredArgsConstructor
public class SysOauthClientServiceImpl extends ServiceImpl<SysOauthClientMapper, SysOauthClient> implements ISysOauthClientService {

}
