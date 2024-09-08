package org.ar.manager.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.FrontPageConfigDTO;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.utils.UserContext;
import org.ar.manager.mapper.FrontPageConfigMapper;
import org.ar.manager.service.IFrontPageConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 首页弹窗内容 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-04-27
 */
@Service
@RequiredArgsConstructor
public class FrontPageConfigServiceImpl extends ServiceImpl<FrontPageConfigMapper, FrontPageConfigDTO> implements IFrontPageConfigService {

    private final RedisUtils redisUtils;

    @Override
    public List<FrontPageConfigDTO> listPage() {
        List<FrontPageConfigDTO>  result = this.baseMapper.selectList(null);
        return result;
    }

    @Override
    public RestResult updateInfo(FrontPageConfigDTO req) {
        req.setUpdateBy(UserContext.getCurrentUserName());
        req.setUpdateTime(LocalDateTime.now());
        baseMapper.updateById(req);
        List<FrontPageConfigDTO>  result = this.baseMapper.selectList(null);
        redisUtils.set(RedisConstants.FRONT_PAGE_CONFIG, JSON.toJSONString(result));
        return RestResult.ok();
    }

}
