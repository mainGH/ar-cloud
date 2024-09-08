package org.ar.manager.service;

import org.ar.common.core.page.PageReturn;
import org.ar.manager.entity.BiMerchantStatistics;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.manager.req.MerchantDailyReportReq;

import java.util.List;

/**
 * <p>
 * 商户统计报表 服务类
 * </p>
 *
 * @author 
 * @since 2024-03-09
 */
public interface IBiMerchantStatisticsService extends IService<BiMerchantStatistics> {

    List<BiMerchantStatistics> listPage(MerchantDailyReportReq req);
}
