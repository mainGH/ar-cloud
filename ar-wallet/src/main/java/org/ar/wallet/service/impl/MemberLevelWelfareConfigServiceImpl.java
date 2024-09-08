package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jsonwebtoken.lang.Collections;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.MemberLevelWelfareConfigDTO;
import org.ar.common.pay.req.MemberManualLogsReq;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.MemberLevelConfig;
import org.ar.wallet.entity.MemberLevelWelfareConfig;
import org.ar.wallet.mapper.MemberLevelWelfareConfigMapper;
import org.ar.wallet.service.IMemberLevelWelfareConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 会员等级福利配置 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-04-10
 */
@Service
@RequiredArgsConstructor
public class MemberLevelWelfareConfigServiceImpl extends ServiceImpl<MemberLevelWelfareConfigMapper, MemberLevelWelfareConfig> implements IMemberLevelWelfareConfigService {
    private final WalletMapStruct walletMapStruct;
    private final RedisUtils redisUtils;
    @Override
    public PageReturn<MemberLevelWelfareConfigDTO> listPage(MemberManualLogsReq req) {
        Page<MemberLevelWelfareConfig> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MemberLevelWelfareConfig> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByAsc(MemberLevelWelfareConfig::getLevel);
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<MemberLevelWelfareConfig> records = page.getRecords();
        List<MemberLevelWelfareConfigDTO> list = walletMapStruct.memberLevelWelfareConfigToDto(records);
        return PageUtils.flush(page, list);
    }

    @Override
    public RestResult updateInfo(MemberLevelWelfareConfigDTO req) {
        MemberLevelWelfareConfig memberLevelConfig = BeanUtil.toBean(req, MemberLevelWelfareConfig.class);
        memberLevelConfig.setUpdateTime(LocalDateTime.now());
        memberLevelConfig.setUpdateBy(UserContext.getCurrentUserName());
        baseMapper.updateById(memberLevelConfig);
        List<MemberLevelWelfareConfig> list = this.baseMapper.selectList(null);
        redisUtils.set(RedisConstants.MEMBER_LEVEL_WELFARE_CONFIG, JSON.toJSONString(list));
        return RestResult.ok();
    }

    @Override
    public void run(String... args) throws Exception {
        List<MemberLevelWelfareConfig> list = this.baseMapper.selectList(null);
        redisUtils.set(RedisConstants.MEMBER_LEVEL_WELFARE_CONFIG, JSON.toJSONString(list));
    }

    /**
     * 根据等级查询对应福利
     *
     * @param level
     * @return
     */
    @Override
    public MemberLevelWelfareConfig getWelfareByLevel(Integer level) {
        if (level == null) {
            return null;
        }
        String value = (String) redisUtils.get(RedisConstants.MEMBER_LEVEL_WELFARE_CONFIG);
        List<MemberLevelWelfareConfig> configList = JSON.parseObject(value, new TypeReference<List<MemberLevelWelfareConfig>>() {
        });
        if (Collections.isEmpty(configList)) {
            return null;
        }
        for (MemberLevelWelfareConfig config : configList) {
            if (config.getLevel().intValue() == level) {
                return config;
            }
        }
        return null;
    }
}
