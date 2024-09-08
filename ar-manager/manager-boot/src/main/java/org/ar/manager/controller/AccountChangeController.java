package org.ar.manager.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.utils.CommonUtils;
import org.ar.common.pay.req.AccountChangeReq;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.utils.UserContext;
import org.ar.manager.api.AccountChangeFeignClient;
import org.ar.manager.dto.AccountChangeDTO;


import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author
 */
@RestController
@Api(description = "商户账变")
@RequestMapping(value = {"/api/v1/accountChangeAdmin", "/accountChangeAdmin"})
public class AccountChangeController {

    @Resource
    AccountChangeFeignClient accountChangeFeignClient;

    @Resource
    RedisUtils redisUtils;

    @PostMapping("/query")
    @ApiOperation(value = "商户账变列表")
    public RestResult<List<AccountChangeDTO>> listPage(@RequestBody @ApiParam AccountChangeReq accountChangeReq) {
        RestResult<List<AccountChangeDTO>> result = accountChangeFeignClient.listPage(accountChangeReq);
        return result;
    }


    @PostMapping ("/queryTotal")
    @ApiOperation(value = "商户账变总订单合计")
    public RestResult<AccountChangeDTO> queryTotal(@RequestBody @ApiParam AccountChangeReq accountChangeReq) {
        RestResult<AccountChangeDTO> result = accountChangeFeignClient.queryTotal(accountChangeReq);
        return result;
    }


    @PostMapping("/fetchAccountType")
    @ApiOperation(value = "获取账变类型")
    public RestResult orderCallbackStatus() {

        RestResult<Map<Integer, String>> map = accountChangeFeignClient.fetchAccountType();
        return map;

    }
}
