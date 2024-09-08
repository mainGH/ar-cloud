package org.ar.wallet.controller;


import cn.hutool.core.date.DateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.ApplyDistributedDTO;
import org.ar.common.pay.req.ApplyDistributedListPageReq;
import org.ar.wallet.Enum.AccountChangeEnum;
import org.ar.wallet.Enum.ChangeModeEnum;
import org.ar.wallet.Enum.CurrenceEnum;
import org.ar.wallet.Enum.DistributeddStatusEnum;
import org.ar.wallet.entity.ApplyDistributed;
import org.ar.wallet.entity.MerchantInfo;
import org.ar.wallet.mapper.CollectionOrderMapper;
import org.ar.wallet.mapper.PaymentOrderMapper;
import org.ar.wallet.req.ApplyDistributedReq;
import org.ar.wallet.req.MerchantInfoReq;
import org.ar.wallet.service.IApplyDistributedService;
import org.ar.wallet.service.IMerchantInfoService;
import org.ar.wallet.util.AmountChangeUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDateTime;
import java.util.List;

/**
    * @author
    */
    @Slf4j
    @RequiredArgsConstructor
    @RestController
    @Api(description = "下发申请控制器")
    @RequestMapping(value = {"/api/v1/applyDistributed", "/applyDistributed"})
    @ApiIgnore
    public class ApplyDistributedController {
        private final IApplyDistributedService applyDistributedService;
        private final AmountChangeUtil amountChangeUtil;
        private final IMerchantInfoService merchantInfoService;

        @PostMapping("/listpage")
        @ApiOperation(value = "下发申请列表")
        public RestResult<List<ApplyDistributedDTO>> list(@RequestBody @ApiParam ApplyDistributedListPageReq req) {
            PageReturn<ApplyDistributedDTO> payConfigPage = applyDistributedService.listPage(req);
            return RestResult.page(payConfigPage);
        }

    @PostMapping("/listRecordPage")
    @ApiOperation(value = "下发申请记录列表")
    public RestResult<List<ApplyDistributedDTO>> listRecordPage(@RequestBody @ApiParam ApplyDistributedListPageReq req) {
        PageReturn<ApplyDistributedDTO> payConfigPage = applyDistributedService.listRecordPage(req);
        return RestResult.page(payConfigPage);
    }




    @PostMapping("/listRecordTotal")
    @ApiOperation(value = "下发申请记录总计")
    public RestResult<ApplyDistributedDTO> listRecordTotal(@RequestBody @ApiParam ApplyDistributedListPageReq req) {
        ApplyDistributedDTO applyDistributedDTO = applyDistributedService.listRecordTotal(req);
        return  RestResult.ok(applyDistributedDTO);
    }


        @PostMapping("/appliy")
        @ApiOperation(value = "下发申请接口")
        public RestResult<ApplyDistributedDTO> appliy(@RequestBody @ApiParam ApplyDistributedReq req) {
            ApplyDistributed applyDistributed =  new ApplyDistributed();
            BeanUtils.copyProperties(req, applyDistributed);
            applyDistributedService.save(applyDistributed);
            ApplyDistributedDTO applyDistributedDTO = new ApplyDistributedDTO();
            BeanUtils.copyProperties(applyDistributed,applyDistributedDTO);
            return RestResult.ok(applyDistributedDTO);
        }



    @PostMapping("/distribute")
    @ApiOperation(value = "同意")
    public RestResult<ApplyDistributedDTO> update(@RequestParam("id") @ApiParam Long id) {
        ApplyDistributed applyDistributed =  new ApplyDistributed();
        //BeanUtils.copyProperties(req, applyDistributed);

        applyDistributed.setId(id);
        ApplyDistributed rapplyDistributed = applyDistributedService.getById(applyDistributed);
        ApplyDistributedDTO applyDistributedDto = applyDistributedService.distributed(rapplyDistributed);
        return RestResult.ok(applyDistributedDto);
    }


    @PostMapping("/nodistribute")
    @ApiOperation(value = "不同意")
    @Transactional
    public RestResult<ApplyDistributedDTO> nodistribute(@RequestParam("id") @ApiParam Long id) {
        ApplyDistributed applyDistributed =  new ApplyDistributed();
        applyDistributed.setId(id);
        String time = DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd");
        ApplyDistributed rapplyDistributed = applyDistributedService.getById(applyDistributed);
        rapplyDistributed.setStatus(DistributeddStatusEnum.NODISTRIBUTED.getCode());
        applyDistributedService.updateById(rapplyDistributed);
        amountChangeUtil.insertChangeAmountRecord(rapplyDistributed.getMerchantCode(), rapplyDistributed.getAmount(), ChangeModeEnum.ADD, CurrenceEnum.ARB.getCode(), rapplyDistributed.getOrderNo(),
                AccountChangeEnum.WITHDRAW_BACK, time, "", "");

        ApplyDistributedDTO  applyDistributedDto = new ApplyDistributedDTO();
        BeanUtils.copyProperties(rapplyDistributed,applyDistributedDto);
        return RestResult.ok(applyDistributedDto);
    }


}
