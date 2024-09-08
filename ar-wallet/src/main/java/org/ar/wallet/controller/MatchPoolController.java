package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MatchPoolDTO;
import org.ar.common.pay.dto.MatchPoolListPageDTO;
import org.ar.common.pay.dto.PaymentOrderChildDTO;
import org.ar.common.pay.dto.PaymentOrderDTO;
import org.ar.common.pay.req.MatchPoolGetChildReq;
import org.ar.common.pay.req.MatchPoolListPageReq;
import org.ar.common.pay.req.MatchPoolReq;
import org.ar.wallet.service.IMatchPoolService;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

/**
 * @author
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(description = "匹配池配置信息控制器")
@RequestMapping(value = {"/api/v1/matchPool", "/matchPool"})
@ApiIgnore
public class MatchPoolController {


    private final IMatchPoolService matchPoolService;

    @PostMapping("/listpage")
    @ApiOperation(value = "列表")
    public RestResult<MatchPoolListPageDTO> list(@RequestBody @ApiParam MatchPoolListPageReq req) {
        PageReturn<MatchPoolListPageDTO> payConfigPage = matchPoolService.listPage(req);
        return RestResult.page(payConfigPage);
    }


    @PostMapping("/matchPooTotal")
    @ApiOperation(value = "总计")
    public RestResult<MatchPoolListPageDTO> matchPooTotal(@Validated @RequestBody MatchPoolListPageReq req) {

       MatchPoolListPageDTO  matchPoolDTO= matchPoolService.matchPooTotal(req);
        return RestResult.ok(matchPoolDTO);
    }

    @PostMapping("/getChildren")
    @ApiOperation(value = "查看")
    public RestResult<List<PaymentOrderChildDTO>> getChildren(@Validated @RequestBody MatchPoolGetChildReq req) {

        List<PaymentOrderChildDTO>  list = matchPoolService.getChildren(req);
        return RestResult.ok(list);
    }


}
