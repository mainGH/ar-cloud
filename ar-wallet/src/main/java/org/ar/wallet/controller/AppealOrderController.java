package org.ar.wallet.controller;


import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.SneakyThrows;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.AppealOrderDTO;
import org.ar.common.pay.dto.AppealOrderExportDTO;
import org.ar.common.pay.dto.ApplyDistributedDTO;
import org.ar.common.pay.req.AppealOrderIdReq;
import org.ar.common.pay.req.AppealOrderPageListReq;
import org.ar.common.pay.req.ApplyDistributedListPageReq;
import org.ar.wallet.entity.AppealOrder;
import org.ar.wallet.req.AccountChangeReq;
import org.ar.wallet.service.IAppealOrderService;
import org.ar.wallet.vo.AccountChangeVo;
import org.ar.wallet.vo.AppealOrderVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 申诉控制器
 * @author
 */
@RestController
@RequestMapping(value = {"/api/v1/appealOrder", "/appealOrder"})
@ApiIgnore
public class AppealOrderController {

    @Resource
    IAppealOrderService iAppealOrderService;

    @PostMapping("/submit")
    @ApiOperation(value = "提交申诉")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "上传图片", required = true, dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "video", value = "上传视频", required = true, dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "appealType", value = "申诉类型: 1-提现申诉 2-充值申诉", required = true, dataType = "Integer"),
            @ApiImplicitParam(name = "orderNo", value = "订单号", required = true, dataType = "String"),
            @ApiImplicitParam(name = "reason", value = "申诉原因", required = true, dataType = "String"),
            @ApiImplicitParam(name = "mid", value = "会员id", required = true, dataType = "String"),
            @ApiImplicitParam(name = "mAccount", value = "会员账号", required = true, dataType = "String"),
            @ApiImplicitParam(name = "orderAmount", value = "订单金额", required = true, dataType = "BigDecimal"),
            @ApiImplicitParam(name = "belongMerchantCode", value = "会员所属商户code", dataType = "String"),
    })
    public RestResult submitAppeal(MultipartFile[] file,
                                   MultipartFile videoUpload,
                                   Integer appealType,
                                   String orderNo,
                                   String reason,
                                   String mid,
                                   String mAccount,
                                   BigDecimal orderAmount,
                                   String belongMerchantCode) throws FileNotFoundException {

        iAppealOrderService.submitAppeal(file, videoUpload, appealType, orderNo, reason, mid, mAccount, orderAmount, belongMerchantCode);

        return RestResult.ok();

    }

    @PostMapping("/query")
    @ApiOperation(value = "查询申诉详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "orderNo", value = "订单号", required = true, dataType = "String"),
            @ApiImplicitParam(name = "appealType", value = "申诉类型: 1-提现申诉 2-充值申诉", required = true, dataType = "Integer"),
    })
    public RestResult<AppealOrderVo> queryAppealOrder(String orderNo, Integer appealType) throws Exception {
        AppealOrderVo result = iAppealOrderService.queryAppealOrder(orderNo, appealType);
        return RestResult.ok(result);
    }



    @PostMapping("/pay")
    @ApiOperation(value = "已支付")
    public RestResult<AppealOrderDTO> pay(@RequestBody @ApiParam AppealOrderIdReq req)  {
        AppealOrderDTO appealOrderDTO = iAppealOrderService.pay(req);
        return RestResult.ok(appealOrderDTO);
    }


    @PostMapping("/nopay")
    @ApiOperation(value = "未支付")
    public RestResult<AppealOrderDTO> nopay(@RequestBody @ApiParam AppealOrderIdReq req)  {
        AppealOrderDTO appealOrderDTO= iAppealOrderService.nopay(req);
        return RestResult.ok(appealOrderDTO);
    }

    @SneakyThrows
    @PostMapping("/listpage")
    @ApiOperation(value = "申诉列表")
    public RestResult<List<AppealOrderDTO>> listpage(@RequestBody @ApiParam AppealOrderPageListReq req) {
        PageReturn<AppealOrderDTO> payConfigPage = iAppealOrderService.listPage(req);
        return RestResult.page(payConfigPage);
    }

    @SneakyThrows
    @PostMapping("/listpageExport")
    @ApiOperation(value = "申诉列表导出")
    public RestResult<List<AppealOrderExportDTO>> listpageExport(@RequestBody @ApiParam AppealOrderPageListReq req) {
        PageReturn<AppealOrderExportDTO> payConfigPage = iAppealOrderService.listPageExport(req);
        return RestResult.page(payConfigPage);
    }
}
