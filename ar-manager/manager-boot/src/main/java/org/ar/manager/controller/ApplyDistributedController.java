package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.ApplyDistributedDTO;
import org.ar.common.pay.req.ApplyDistributedListPageReq;
import org.ar.manager.annotation.SysLog;
import org.ar.manager.api.ApplyDistributedClient;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@Api(description = "下发申请控制器")
@RequestMapping(value = {"/api/v1/applyDistributedAdmin", "/applyDistributedAdmin"})
public class ApplyDistributedController {
    @Resource
    ApplyDistributedClient applyDistributedClient;

    @PostMapping("/listpage")
    @ApiOperation(value = "下发申请列表")
    public RestResult<List<ApplyDistributedDTO>> list(@RequestBody @ApiParam ApplyDistributedListPageReq applyDistributedReq) {

        RestResult<List<ApplyDistributedDTO>> result = applyDistributedClient.listpage(applyDistributedReq);
        return result;
    }




    @PostMapping("/distribute")
    @SysLog(title = "下发申请控制器", content = "同意")
    @ApiOperation(value = "同意")
    public RestResult update(@ApiParam @RequestParam("id") Long id) {
        RestResult<ApplyDistributedDTO> result = applyDistributedClient.distribute(id);
        return result;
    }


    @PostMapping("/nodistribute")
    @SysLog(title = "下发申请控制器", content = "不同意")
    @ApiOperation(value = "不同意")
    public RestResult<ApplyDistributedDTO> nodistribute(@ApiParam @RequestParam("id") Long id) {
        RestResult<ApplyDistributedDTO> result = applyDistributedClient.nodistribute(id);
        return result;
    }


    @PostMapping("/listRecordPage")
    @ApiOperation(value = "下发申请表")
    public RestResult<List<ApplyDistributedDTO>> listRecordPage(@RequestBody @ApiParam ApplyDistributedListPageReq req) {
        RestResult<List<ApplyDistributedDTO>> result = applyDistributedClient.listRecordPage(req);
        return result;
    }


    @PostMapping("/listRecordTotal")
    @ApiOperation(value = "下发申请记录总计")
    public RestResult<ApplyDistributedDTO> listRecordTotal(@RequestBody @ApiParam ApplyDistributedListPageReq req) {
        RestResult<ApplyDistributedDTO> result = applyDistributedClient.listRecordTotal(req);
        return result;
    }

}
