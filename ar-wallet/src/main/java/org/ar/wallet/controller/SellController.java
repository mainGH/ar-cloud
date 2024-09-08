package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.MemberOperationModuleEnum;
import org.ar.wallet.annotation.LogMemberOperation;
import org.ar.wallet.req.*;
import org.ar.wallet.service.IAppealOrderService;
import org.ar.wallet.service.ICollectionInfoService;
import org.ar.wallet.service.ISellService;
import org.ar.wallet.vo.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sellCenter")
@Api(description = "前台-卖出控制器")
@Validated
@Slf4j
public class SellController {

    private final ISellService sellService;
    private final IAppealOrderService appealOrderService;
    private final ICollectionInfoService collectionInfoService;


    @GetMapping("/fetchPageData")
    @ApiOperation(value = "前台-卖出页面接口")
    public RestResult<SellListVo> fetchPageData() {
        return sellService.fetchPageData();
    }

    @PostMapping("/getSellOrderList")
    @ApiOperation(value = "前台-获取卖出订单列表")
    public RestResult<PageReturn<SellOrderListVo>> getSellOrderList(@RequestBody(required = false) @ApiParam @Valid SellOrderListReq sellOrderListReq) {
        //获取卖出订单列表
        return sellService.sellOrderList(sellOrderListReq);
    }

    @PostMapping("/cancelMatching")
    @ApiOperation(value = "前台-取消匹配")
    public RestResult cancelMatching(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //取消匹配
        return sellService.cancelMatching(platformOrderReq);
    }

    @PostMapping("/getSellOrderDetails")
    @ApiOperation(value = "前台-查看卖出订单详情")
    public RestResult<SellOrderDetailsVo> getSellOrderDetails(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //查看卖出订单详情
        return sellService.getSellOrderDetails(platformOrderReq);
    }

    @PostMapping("/sell")
    @ApiOperation(value = "前台-卖出下单接口")
    @LogMemberOperation(value = MemberOperationModuleEnum.SELL_ORDER)
    public RestResult<SellOrderVo> sell(@RequestBody @ApiParam @Valid SellReq sellReq, HttpServletRequest request) {
        //卖出处理
        return sellService.sellProcessor(sellReq, request);
    }

    @PostMapping("/buyCompleted")
    @ApiOperation(value = "前台-确认到账")
    @LogMemberOperation(value = MemberOperationModuleEnum.CONFIRM_ARRIVAL)
    public RestResult<BuyCompletedVo> buyCompleted(@RequestBody @ApiParam @Valid SellPlatformOrderReq sellPlatformOrderReq) {
        //交易成功处理
        return sellService.transactionSuccessHandler(sellPlatformOrderReq.getPlatformOrder(), UserContext.getCurrentUserId(), null, null, "1", sellPlatformOrderReq.getPaymentPassword());
    }

    @PostMapping("/getCancelSellPageData")
    @ApiOperation(value = "前台-获取取消卖出页面数据")
    public RestResult<CancelSellPageDataVo> getCancelSellPageData(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //获取取消卖出页面数据
        return sellService.getCancelSellPageData(platformOrderReq);
    }

    @PostMapping("/cancelSellOrder")
    @ApiOperation(value = "前台-取消卖出订单")
    @LogMemberOperation(value = MemberOperationModuleEnum.CANCEL_SELL_ORDER)
    public RestResult cancelSellOrder(@RequestBody @ApiParam @Valid CancelOrderReq cancelOrderReq) {
        //取消卖出订单处理
        return sellService.cancelSellOrder(cancelOrderReq);
    }

    @PostMapping("/continueMatching")
    @ApiOperation(value = "前台-继续匹配")
    @LogMemberOperation(value = MemberOperationModuleEnum.CONTINUE_MATCHING)
    public RestResult continueMatching(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //继续匹配
        return sellService.continueMatching(platformOrderReq);
    }

    @PostMapping("/processAmountError")
    @ApiOperation(value = "前台-提交金额错误")
    @LogMemberOperation(value = MemberOperationModuleEnum.SUBMIT_AMOUNT_ERROR)
    public RestResult processAmountError(@NotBlank(message = "Order number cannot be empty") @ApiParam(value = "订单号", required = true) @RequestParam("platformOrder") @Valid String platformOrder,

                                         @NotNull(message = "Actual amount cannot be empty") @DecimalMin(value = "0.00", message = "Actual amount format is incorrect") @ApiParam(value = "实际金额", required = true) @Valid BigDecimal orderActualAmount,

                                         @NotEmpty(message = "Image cannot be empty") @ApiParam(value = "金额错误图片URL", required = true) @RequestParam("images") List<String> images,
                                         @ApiParam(value = "金额错误视频URL") @RequestParam(value = "video", required = false) String video)
    {
        //提交金额错误处理
        return sellService.processAmountError(platformOrder, images, video, orderActualAmount);
    }


    @PostMapping("/cancelApplication")
    @ApiOperation(value = "前台-取消申请金额错误")
    @LogMemberOperation(value = MemberOperationModuleEnum.CANCEL_AMOUNT_ERROR_REQUEST)
    public RestResult cancelApplication(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //取消申请
        return sellService.cancelApplication(platformOrderReq);
    }


    @PostMapping("/submitSellOrderAppeal")
    @ApiOperation(value = "前台-提交卖出申诉")
    @LogMemberOperation(value = MemberOperationModuleEnum.SUBMIT_SELL_APPEAL)
    public RestResult submitBuyOrderAppeal(@NotBlank(message = "Order number cannot be empty") @ApiParam(value = "订单号", required = true) @RequestParam("platformOrder") @Valid String platformOrder,
                                           @ApiParam(value = "申诉原因") @Pattern(regexp = "^.{0,60}$", message = "Please fill in no more than 60 characters") @RequestParam("appealReason") @Valid String appealReason,
                                           @ApiParam(value = "申诉图片", required = true) @RequestParam("images") List<String> images,
                                           @ApiParam(value = "申诉视频")  @RequestParam(value = "video", required = false) String video) {
        //卖出申诉 处理
        return sellService.sellOrderAppealProcess(platformOrder,  appealReason, images, video);
    }

    @PostMapping("/viewSellOrderAppealDetails")
    @ApiOperation(value = "前台-查看卖出订单申诉详情")
    public RestResult<AppealDetailsVo> viewSellOrderAppealDetails(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //查看买入订单申诉详情
        return appealOrderService.viewAppealDetails(platformOrderReq, "2");
    }

    @PostMapping("/normalCollectionInfo")
    @ApiOperation(value = "前台-获取当前用户在正常收款的收款信息")
    public RestResult<PageReturn<NormalCollectionInfoVo>> currentNormalCollectionInfo(@RequestBody(required = false) @ApiParam @Valid PageRequestHome pageRequestHome) {
        return collectionInfoService.currentNormalCollectionInfo(pageRequestHome);
    }

    @PostMapping("/createcollectionInfo")
    @ApiOperation(value = "前台-添加收款信息")
    @LogMemberOperation(value = MemberOperationModuleEnum.ADD_PAYMENT_INFO)
    public RestResult createcollectionInfo(@RequestBody @ApiParam @Valid FrontendCollectionInfoReq frontendCollectionInfoReq) {
        //添加收款信息处理
        return collectionInfoService.createcollectionInfoProcessing(frontendCollectionInfoReq);
    }


    @PostMapping("/setDefaultCollectionInfo")
    @ApiOperation(value = "前台-设置默认收款信息")
    @LogMemberOperation(value = MemberOperationModuleEnum.SET_DEFAULT_COLLECTION_INFO)
    public RestResult setDefaultCollectionInfo(@RequestBody @ApiParam @Valid CollectioninfoIdReq collectioninfoIdReq) {
        //设置默收款信息处理
        return collectionInfoService.setDefaultCollectionInfoReq(collectioninfoIdReq);
    }


    @PostMapping("/checkUpiIdDuplicate")
    @ApiOperation(value = "前台-校验UPI_ID是否重复")
    public RestResult<CheckUpiIdDuplicateVo> checkUpiIdDuplicate(@RequestBody @ApiParam @Valid CheckUpiIdDuplicateReq checkUpiIdDuplicateReq) {
        //校验UPI_ID是否重复
        return collectionInfoService.checkUpiIdDuplicate(checkUpiIdDuplicateReq);
    }
}
