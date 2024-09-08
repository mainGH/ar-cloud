package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.BiMemberReconciliationDTO;
import org.ar.manager.entity.BiMerchantStatistics;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.service.IBiMerchantStatisticsService;
import org.ar.manager.service.impl.BiMemberReconciliationServiceImpl;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 商户统计报表 前端控制器
 * </p>
 *
 * @author 
 * @since 2024-03-09
 */
@RestController
@RequestMapping(value = {"/api/v1/biMerchantStatistics", "/biMerchantStatistics"})
@Api(description = "商户统计报表控制器")
public class BiMerchantStatisticsController {

    @Resource
    IBiMerchantStatisticsService iBiMerchantStatisticsService;

    @PostMapping("/query")
    @ApiOperation(value = "查询商户统计报表记录")
    public RestResult<List<BiMerchantStatistics>> listPage(@Validated @RequestBody MerchantDailyReportReq req) {

        List<BiMerchantStatistics> result = iBiMerchantStatisticsService.listPage(req);
        return RestResult.ok(result);
    }

}
