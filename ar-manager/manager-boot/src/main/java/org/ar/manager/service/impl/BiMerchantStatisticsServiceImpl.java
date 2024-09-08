package org.ar.manager.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.manager.entity.BiMerchantPayOrderMonth;
import org.ar.manager.entity.BiMerchantStatistics;
import org.ar.manager.mapper.BiMerchantStatisticsMapper;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.service.IBiMerchantStatisticsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * <p>
 * 商户统计报表 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-03-09
 */
@Service
public class BiMerchantStatisticsServiceImpl extends ServiceImpl<BiMerchantStatisticsMapper, BiMerchantStatistics> implements IBiMerchantStatisticsService {

    @Override
    public List<BiMerchantStatistics> listPage(MerchantDailyReportReq req) {
        LambdaQueryChainWrapper<BiMerchantStatistics> lambdaQuery = lambdaQuery();
        String dateStr = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()).plusDays(-1), GlobalConstants.DATE_FORMAT_DAY);
        lambdaQuery.orderByDesc(BiMerchantStatistics::getDateTime, BiMerchantStatistics::getMemberNum);
        lambdaQuery.eq(BiMerchantStatistics::getDateTime, dateStr);
        List<BiMerchantStatistics> resultList = baseMapper.selectList(lambdaQuery.getWrapper());

        return resultList;
    }
}
