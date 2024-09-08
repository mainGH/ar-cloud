package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.BiMemberReconciliationDTO;
import org.ar.manager.entity.BiMemberReconciliation;
import org.ar.manager.entity.BiMerchantDaily;
import org.ar.manager.entity.BiMerchantReconciliation;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.service.IBiMerchantDailyService;
import org.ar.manager.service.IBiMerchantReconciliationService;
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
 * 会员对账报表 前端控制器
 * </p>
 *
 * @author 
 * @since 2024-03-06
 */
@RestController
@RequestMapping(value = {"/api/v1/biMemberReconciliation", "/biMemberReconciliation"})
@Api(description = "会员对账报表控制器")
public class BiMemberReconciliationController {

    @Resource
    BiMemberReconciliationServiceImpl iBiMerchantDailyService;

    @PostMapping("/query")
    @ApiOperation(value = "查询会员对账报表记录")
    public RestResult<List<BiMemberReconciliationDTO>> listPage(@Validated @RequestBody MerchantDailyReportReq req) {

        PageReturn<BiMemberReconciliationDTO> result = iBiMerchantDailyService.listPage(req);
        return RestResult.page(result);
    }

}
