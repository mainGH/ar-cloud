package org.ar.wallet.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.ApiResponse;
import org.ar.common.core.result.ApiResponseEnum;
import org.ar.common.core.result.RestResult;
import org.ar.wallet.entity.ActivateWallet;
import org.ar.wallet.entity.PaymentInfo;
import org.ar.wallet.req.*;
import org.ar.wallet.service.IApiCenterService;
import org.ar.wallet.vo.ApiResponseVo;
import org.ar.wallet.vo.InitiateWalletActivationVo;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/apiCenter")
@Api(description = "商户接口控制器")
@ApiIgnore
public class ApiCenterController {

    private final IApiCenterService apiCenterService;

    /**
     * 获取钱包会员信息
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}<{@link ApiResponseVo}>
     */
    @ApiIgnore
    @PostMapping("/getWalletMemberInfo")
    public ApiResponse getWalletMemberInfo(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.getWalletMemberInfo(apiRequest, request);
    }


    /**
     * 激活钱包接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}<{@link ApiResponseVo}>
     */
    @ApiIgnore
    @PostMapping("/activateWallet")
    public ApiResponse activateWallet(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.activateWallet(apiRequest, request);
    }


    /**
     * 充值接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @ApiIgnore
    @PostMapping("/deposit/apply")
    public ApiResponse depositApply(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.depositApply(apiRequest, request);
    }


    /**
     * 提现接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @ApiIgnore
    @PostMapping("/withdrawal/apply")
    public ApiResponse withdrawalApply(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.withdrawalApply(apiRequest, request);
    }


    /**
     * 获取支付页面(收银台)信息接口
     *
     * @param token
     * @return {@link ApiResponse}
     */
    @GetMapping("/retrievePaymentDetails")
    @ApiOperation(value = "获取支付页面(收银台)信息接口")
    public RestResult<PaymentInfo> retrievePaymentDetails(@ApiParam(value = "订单token", required = true) @RequestParam("token") String token) {
        return apiCenterService.retrievePaymentDetails(token);
    }


    /**
     * 获取激活钱包页面信息接口
     *
     * @param token
     * @return {@link ApiResponse}
     */
    @GetMapping("/getWalletActivationPageInfo")
    @ApiOperation(value = "获取激活钱包页面信息接口")
    public RestResult<ActivateWallet> getWalletActivationPageInfo(@ApiParam(value = "token", required = true) @RequestParam("token") String token) {
        return apiCenterService.getWalletActivationPageInfo(token);
    }


    /**
     * 激活钱包接口
     *
     * @param initiateWalletActivationReq
     * @param request
     * @return {@link RestResult}<{@link InitiateWalletActivationVo}>
     */
    @PostMapping("/initiateWalletActivation")
    @ApiOperation(value = "激活钱包接口")
    public RestResult<InitiateWalletActivationVo> initiateWalletActivation(@RequestBody @ApiParam InitiateWalletActivationReq initiateWalletActivationReq, HttpServletRequest request) {
        return apiCenterService.initiateWalletActivation(initiateWalletActivationReq, request);
    }


    /**
     * 收银台 确认支付 接口
     *
     * @param confirmPaymentReq
     * @param request
     * @return {@link RestResult}
     */
    @PostMapping("/confirmPayment")
    @ApiOperation(value = "收银台 确认支付 接口")
    public RestResult confirmPayment(@RequestBody @ApiParam ConfirmPaymentReq confirmPaymentReq, HttpServletRequest request) {
        return apiCenterService.confirmPayment(confirmPaymentReq);
    }


    /**
     * 收银台 取消支付 接口
     *
     * @param cancelPaymentReq
     * @param request
     * @return {@link RestResult}
     */
    @PostMapping("/cancelPayment")
    @ApiOperation(value = "收银台 取消支付 接口")
    public RestResult cancelPayment(@RequestBody @ApiParam CancelPaymentReq cancelPaymentReq, HttpServletRequest request) {
        return apiCenterService.cancelPayment(cancelPaymentReq);
    }


    /**
     * 进入钱包 接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @ApiIgnore
    @PostMapping("/accessWallet")
    public ApiResponse accessWallet(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.accessWallet(apiRequest, request);
    }


    /**
     * 查询充值订单
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @ApiIgnore
    @PostMapping("/deposit/query")
    public ApiResponse depositQuery(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.depositQuery(apiRequest, request);
    }


    /**
     * 查询提现订单
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    @ApiIgnore
    @PostMapping("/withdrawal/query")
    public ApiResponse withdrawalQuery(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.withdrawalQuery(apiRequest, request);
    }


    /**
     * 捕获该控制器下的参数校验异常
     *
     * @param e
     * @return {@link ApiResponse}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleValidationExceptions(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        System.out.println("Error Message: " + errorMessage);
        return ApiResponse.ofMsg(ApiResponseEnum.PARAM_VALID_FAIL, errorMessage, null);
    }

    /**
     * 查看退回订单状态
     * @param apiRequest apiRequest
     * @param request request
     * @return {@link ApiResponse}
     */
    @ApiIgnore
    @PostMapping("/checkCashBack")
    public ApiResponse checkCashBack(@RequestBody @ApiParam ApiRequest apiRequest, HttpServletRequest request) {
        return apiCenterService.checkCashBack(apiRequest, request);
    }



    /**
     * 激活钱包接口
     *
     * @param initiateWalletActivationReq
     * @param request
     * @return {@link RestResult}<{@link InitiateWalletActivationVo}>
     */
//    @PostMapping("/initiateAppWalletActivation")
//    @ApiOperation(value = "激活app钱包接口")
//    public RestResult<InitiateWalletActivationVo> initiateAppWalletActivation(@RequestBody @ApiParam InitiateAppWalletActivationReq initiateWalletActivationReq, HttpServletRequest request) {
//        return apiCenterService.initiateAppWalletActivation(initiateWalletActivationReq, request);
//    }
}


