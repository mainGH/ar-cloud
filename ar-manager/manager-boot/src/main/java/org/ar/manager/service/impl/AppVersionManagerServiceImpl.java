package org.ar.manager.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.result.RestResult;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.utils.UserContext;
import org.ar.common.pay.dto.AppVersionDTO;
import org.ar.manager.mapper.AppVersionManagerMapper;
import org.ar.manager.service.IAppVersionManagerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * APP版本管理 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-04-20
 */
@Service
@RequiredArgsConstructor
public class AppVersionManagerServiceImpl extends ServiceImpl<AppVersionManagerMapper, AppVersionDTO> implements IAppVersionManagerService {

    private final RedisUtils redisUtils;

    @Override
    public List<AppVersionDTO> listPage() {
        List<AppVersionDTO>  result = this.baseMapper.selectList(null);
        return result;
    }

    @Override
    public RestResult updateInfo(AppVersionDTO req) {
        req.setUpdateBy(UserContext.getCurrentUserName());
        baseMapper.updateById(req);
        List<AppVersionDTO>  result = this.baseMapper.selectList(null);
        redisUtils.set(RedisConstants.APP_VERSION_CONFIG, JSON.toJSONString(result));
        return RestResult.ok();
    }

}
