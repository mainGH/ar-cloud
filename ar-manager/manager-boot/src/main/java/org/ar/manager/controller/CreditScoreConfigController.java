package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.CreditScoreConfigDTO;
import org.ar.common.pay.req.CreditScoreConfigListPageReq;
import org.ar.common.pay.req.CreditScoreConfigUpdateReq;
import org.ar.manager.api.CreditScoreConfigClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 信用分配置表 前端控制器
 * </p>
 *
 * @author 
 * @since 2024-04-09
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/api/v1/creditScoreConfigAdmin", "/creditScoreConfigAdmin"})
@Api(description = "信用分配置控制器")
public class CreditScoreConfigController {
    private final CreditScoreConfigClient creditScoreConfigClient;


    @PostMapping("/listPage")
    @ApiOperation(value = "查询信用分配置列表")
    public RestResult<List<CreditScoreConfigDTO>> listPage(@RequestBody @ApiParam CreditScoreConfigListPageReq req) {
        return creditScoreConfigClient.listPage(req);
    }

    @PostMapping("/updateScore")
    @ApiOperation(value = "更新信用分配置")
    public RestResult<CreditScoreConfigDTO> updateScore(@RequestBody @ApiParam CreditScoreConfigUpdateReq req) {
        return creditScoreConfigClient.updateScore(req);
    }

}
