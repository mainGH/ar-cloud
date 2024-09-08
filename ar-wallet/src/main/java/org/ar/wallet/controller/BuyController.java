package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageRequestHome;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.web.exception.BizException;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.MemberAuthenticationStatusEnum;
import org.ar.wallet.Enum.MemberOperationModuleEnum;
import org.ar.wallet.annotation.LogMemberOperation;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.req.*;
import org.ar.wallet.service.*;
import org.ar.wallet.vo.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.List;

import static org.ar.common.core.result.ResultCode.*;

/**
 * @author
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/buyCenter")
@Api(description = "前台-买入控制器")
@Validated
@Slf4j
public class BuyController {

    private final IBuyService buyService;
    private final IUsdtBuyOrderService usdtBuyOrderService;
    private final ICollectionOrderService collectionOrderService;
    private final IAppealOrderService appealOrderService;
    private final QuickBuyService quickBuyService;
    private final IMemberInfoService memberInfoService;



    @GetMapping("/getPaymentType")
    @ApiOperation(value = "前台-获取支付类型")
    public RestResult<List<PaymentTypeVo>> getPaymentType() {
        //获取支付类型
        return buyService.getPaymentType();
    }

    @PostMapping("/buyList")
    @ApiOperation(value = "前台-买入金额列表")
    public RestResult<PageReturn<BuyListVo>> buyList(@RequestBody(required = false) @ApiParam @Valid BuyListReq buyListReq) {
        /*MemberInfo memberInfo = memberInfoService.getMemberInfo();
        if (memberInfo == null) {
            return RestResult.failure(MEMBER_NOT_EXIST);
        }
        if (MemberAuthenticationStatusEnum.UNAUTHENTICATED.getCode().equals(memberInfo.getAuthenticationStatus())) {
            return RestResult.failure(MEMBER_NOT_VERIFIED);
        }*/
        //查询买入列表数据
        return RestResult.ok(buyService.getBuyList(buyListReq, String.valueOf(UserContext.getCurrentUserId())));
    }

    @PostMapping("/buy")
    @ApiOperation(value = "前台-买入下单接口")
    @LogMemberOperation(value = MemberOperationModuleEnum.BUY_ORDER)
    public RestResult buy(@RequestBody @ApiParam @Valid BuyReq buyReq, HttpServletRequest request) {
        //买入处理
        return buyService.buyProcessor(buyReq, request);
    }

    @GetMapping("/getPaymentPageData")
    @ApiOperation(value = "前台-获取支付页面数据")
    public RestResult<BuyVo> getPaymentPageData() {
        //获取支付页面数据
        return buyService.getPaymentPageData();
    }


    @GetMapping("/getUsdtPaymentPageData")
    @ApiOperation(value = "前台-获取USDT支付页面数据")
    public RestResult<UsdtBuyVo> getUsdtPaymentPageData() {
        //获取支付页面数据
        return buyService.getUsdtPaymentPageData();
    }

    @PostMapping("/getBuyOrderList")
    @ApiOperation(value = "前台-获取买入订单列表")
    public RestResult<PageReturn<BuyOrderListVo>> getBuyOrderList(@RequestBody(required = false) @ApiParam @Valid BuyOrderListReq buyOrderListReq) {
        //获取买入订单列表
        return collectionOrderService.buyOrderList(buyOrderListReq);
    }

    @PostMapping("/getBuyOrderDetails")
    @ApiOperation(value = "前台-获取买入订单详情")
    public RestResult<BuyOrderDetailsVo> getBuyOrderDetails(@RequestBody(required = false) @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //获取买入订单详情
        return collectionOrderService.getBuyOrderDetails(platformOrderReq);
    }


    @GetMapping("/getUsdtBuyPageData")
    @ApiOperation(value = "前台-获取USDT买入页面数据")
    public RestResult<UsdtBuyPageDataVo> getUsdtBuyPageData() {
        //获取USDT买入页面数据
        return usdtBuyOrderService.getUsdtBuyPageData();
    }

    @PostMapping("/usdtBuy")
    @ApiOperation(value = "前台-USDT买入下单接口")
    @LogMemberOperation(value = MemberOperationModuleEnum.USDT_BUY_ORDER)
    public RestResult usdtBuy(@RequestBody @ApiParam @Valid UsdtBuyReq usdtBuyReq) {
        //USDT买入处理
        return buyService.usdtBuyProcessor(usdtBuyReq);
    }

    @PostMapping("/getUsdtPurchaseRecords")
    @ApiOperation(value = "前台-获取USDT全部买入记录")
    public RestResult<PageReturn<UsdtBuyOrderVo>> getUsdtPurchaseRecords(@RequestBody(required = false) @ApiParam @Valid PageRequestHome pageRequestHome) {
        //查询所有USDT买入记录
        return usdtBuyOrderService.findAllUsdtPurchaseRecords(pageRequestHome);
    }

    @PostMapping("/getUsdtPurchaseOrderDetails")
    @ApiOperation(value = "前台-获取USDT买入订单详情")
    public RestResult<UsdtPurchaseOrderDetailsVo> getUsdtPurchaseOrderDetails(@RequestBody(required = false) @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //获取买入订单详情
        return usdtBuyOrderService.getUsdtPurchaseOrderDetails(platformOrderReq);
    }

    @PostMapping("/usdtBuyCompleted")
    @ApiOperation(value = "前台-USDT完成转账")
    @LogMemberOperation(value = MemberOperationModuleEnum.USDT_COMPLETE_TRANSFER)
    public RestResult usdtBuyCompleted(
            @NotBlank(message = "Order number cannot be empty")
            @Pattern(regexp = "^[A-Za-z0-9]{5}\\d{1,30}$", message = "Order number format is incorrect")
            @ApiParam(value = "订单号", required = true) @RequestParam("platformOrder") @Valid String platformOrder,
            @ApiParam(value = "凭证截图文件", required = true) @RequestParam("voucherImage") String voucherImage
    ) {
        //USDT完成转账处理
        return usdtBuyOrderService.usdtBuyCompleted(platformOrder, voucherImage);
    }

    @PostMapping("/buyCompleted")
    @ApiOperation(value = "前台-完成支付")
    @LogMemberOperation(value = MemberOperationModuleEnum.COMPLETE_PAYMENT)
    public RestResult buyCompleted(
            @NotBlank(message = "Order number cannot be empty")
            @Pattern(regexp = "^[A-Za-z0-9]{5}\\d{1,30}$", message = "Order number format is incorrect")
            @ApiParam(value = "订单号", required = true) @RequestParam("platformOrder") @Valid String platformOrder,

            @NotBlank(message = "UTR cannot be empty")
            @Pattern(regexp = "^\\d{12}$", message = "UTR format is incorrect")
            @ApiParam(value = "UTR (格式为12位长度纯数字的UTR)", required = true) @RequestParam("utr") @Valid String utr,

            @ApiParam(value = "凭证截图文件", required = true) @RequestParam("voucherImage") String voucherImage
    ) {
        //完成支付处理
        return buyService.buyCompletedProcessor(platformOrder, voucherImage, utr);
    }

    @PostMapping("/getCancelBuyPageData")
    @ApiOperation(value = "前台-获取取消买入页面数据")
    public RestResult<CancelBuyPageDataVo> getCancelBuyPageData(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //获取取消买入页面数据
        return buyService.getCancelBuyPageData(platformOrderReq);
    }

    @PostMapping("/cancelPurchaseOrder")
    @ApiOperation(value = "前台-取消买入订单")
    @LogMemberOperation(value = MemberOperationModuleEnum.CANCEL_BUY_ORDER)
    public RestResult cancelPurchaseOrder(@RequestBody @ApiParam @Valid CancelOrderReq cancelOrderReq) {
        //取消买入订单处理
        cancelOrderReq.setSourceType(1);
        return buyService.cancelPurchaseOrder(cancelOrderReq);
    }

    @PostMapping("/cancelPayment")
    @ApiOperation(value = "前台-取消支付 (此接口是在支付页面中 点击取消订单 调用的)")
    @LogMemberOperation(value = MemberOperationModuleEnum.CANCEL_PAYMENT)
    public RestResult cancelPayment(@RequestBody @ApiParam @Valid CancelOrderReq cancelOrderReq) {
        //取消支付处理
        return buyService.cancelPayment(cancelOrderReq);
    }

    @PostMapping("/submitBuyOrderAppeal")
    @ApiOperation(value = "前台-提交买入订单申诉")
    @LogMemberOperation(value = MemberOperationModuleEnum.SUBMIT_BUY_APPEAL)
    public RestResult submitBuyOrderAppeal(@NotBlank(message = "Order number cannot be empty") @ApiParam(value = "订单号", required = true)
                                           @Pattern(regexp = "^[A-Za-z0-9]{5}\\d{1,30}$", message = "Order number format is incorrect")
                                           @RequestParam("platformOrder") @Valid String platformOrder,
                                           @ApiParam(value = "申诉原因") @Pattern(regexp = "^.{0,60}$", message = "Please fill in no more than 60 characters") @RequestParam("appealReason") @Valid String appealReason,
                                           @ApiParam(value = "申诉图片", required = true) @RequestParam("images") List<String> images,
                                           @ApiParam(value = "申诉视频") @RequestParam(value = "video", required = false) String video) {

        //买入申诉 处理
        return buyService.buyOrderAppealProcess(platformOrder, appealReason, images, video);
    }


    @PostMapping("/viewBuyOrderAppealDetails")
    @ApiOperation(value = "前台-查看买入订单申诉详情")
    public RestResult<AppealDetailsVo> viewBuyOrderAppealDetails(@RequestBody @ApiParam @Valid PlatformOrderReq platformOrderReq) {
        //查看买入订单申诉详情
        return appealOrderService.viewAppealDetails(platformOrderReq, "1");
    }


    @PostMapping("/quickBuyConfirmBuy")
    @ApiOperation(value = "前台-快捷买入确定买入下单接口")
    @LogMemberOperation(value = MemberOperationModuleEnum.QUICK_BUY)
    public RestResult quickBuyConfirmBuy(@RequestBody @ApiParam @Valid BuyReq buyReq, HttpServletRequest request) {
        try {
            return quickBuyService.confirmBuy(buyReq, request);
        } catch (BizException e) {
            return RestResult.failure(e.getResultCode());
        }
    }

    @PostMapping("/quickBuyMatchSellOrder")
    @ApiOperation(value = "前台-快捷买入匹配卖出订单")
    public RestResult<QuickBuyMatchResult> quickBuyMatchSellOrder(@RequestBody @ApiParam @Valid QuickBuyMatchReq quickBuyMatchReq) {
        QuickBuyMatchResult result;
        try {
            result = quickBuyService.matchSellOrder(quickBuyMatchReq.getAmount());
        } catch (BizException e) {
            return RestResult.failure(e.getResultCode());
        }
        if (result == null) {
            return RestResult.failure(NOT_MATCHED_QUICK_BUY_ORDER);
        }
        return RestResult.ok(result);
    }



}
