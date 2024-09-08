package org.ar.pay.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.AccountChange;
import org.ar.pay.req.AccountChangeReq;
import org.ar.pay.service.IAccountChangeService;
import org.ar.pay.vo.AccountChangeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/accountChange")
@Api(description = "账变控制器")
public class AccountChangeController {
    private final IAccountChangeService accountChangeOrderService;

    @PostMapping("/save")
    @ApiOperation(value = "保存账变")
    public RestResult<AccountChange> save(@RequestBody AccountChangeVo accountChangeVo) {
        AccountChange accountChange = new AccountChange();
        BeanUtils.copyProperties(accountChangeVo, accountChange);
        accountChangeOrderService.save(accountChange);
        return RestResult.ok();

    }

    @PostMapping("/update")
    @ApiOperation(value = "更新账变")
    public RestResult update(@RequestBody AccountChangeVo accountChangeVo) {
        AccountChange accountChange = new AccountChange();
        BeanUtils.copyProperties(accountChangeVo, accountChange);
        boolean su = accountChangeOrderService.updateById(accountChange);
        return RestResult.ok();

    }

    @PostMapping("/list")
    @ApiOperation(value = "获取账变列表")
    public RestResult list(@RequestBody(required = false) @ApiParam AccountChangeReq accountChangeReq) {
        PageReturn<AccountChange> collectionOrderPage = accountChangeOrderService.listPage(accountChangeReq);
        return RestResult.ok(collectionOrderPage);
    }

}
