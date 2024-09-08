package org.ar.wallet.service;

import org.ar.common.core.result.ApiResponse;
import org.ar.common.core.result.RestResult;
import org.ar.wallet.entity.ActivateWallet;
import org.ar.wallet.entity.PaymentInfo;
import org.ar.wallet.req.*;
import org.ar.wallet.vo.InitiateWalletActivationVo;

import javax.servlet.http.HttpServletRequest;

public interface IApiCenterService {

    /**
     * 获取钱包会员信息
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    ApiResponse getWalletMemberInfo(ApiRequest apiRequest, HttpServletRequest request);

    /**
     * 获取激活钱包地址接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    ApiResponse activateWallet(ApiRequest apiRequest, HttpServletRequest request);


    /**
     * 充值接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    ApiResponse depositApply(ApiRequest apiRequest, HttpServletRequest request);


    /**
     * 获取支付页面(收银台)信息接口
     *
     * @param token
     * @return {@link RestResult}<{@link PaymentInfo}>
     */
    RestResult<PaymentInfo> retrievePaymentDetails(String token);


    /**
     * 收银台 确认支付 提交接口
     *
     * @param confirmPaymentReq
     * @return {@link RestResult}
     */
    RestResult confirmPayment(ConfirmPaymentReq confirmPaymentReq);


    /**
     * 提现接口
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    ApiResponse withdrawalApply(ApiRequest apiRequest, HttpServletRequest request);


    /**
     * 进入钱包
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    ApiResponse accessWallet(ApiRequest apiRequest, HttpServletRequest request);


    /**
     * 查询充值订单
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    ApiResponse depositQuery(ApiRequest apiRequest, HttpServletRequest request);


    /**
     * 查询提现订单
     *
     * @param apiRequest
     * @param request
     * @return {@link ApiResponse}
     */
    ApiResponse withdrawalQuery(ApiRequest apiRequest, HttpServletRequest request);


    /**
     * 获取激活钱包页面信息接口
     *
     * @param token
     * @return {@link RestResult}<{@link PaymentInfo}>
     */
    RestResult<ActivateWallet> getWalletActivationPageInfo(String token);

    /**
     * 激活钱包
     *
     * @param initiateWalletActivationReq
     * @param request
     * @return {@link RestResult}<{@link InitiateWalletActivationVo}>
     */
    RestResult<InitiateWalletActivationVo> initiateWalletActivation(InitiateWalletActivationReq initiateWalletActivationReq, HttpServletRequest request);


    /**
     * 取消支付
     *
     * @param cancelPaymentReq
     * @return {@link RestResult}
     */
    RestResult cancelPayment(CancelPaymentReq cancelPaymentReq);


    /**
     * 激活钱包
     *
     * @param initiateWalletActivationReq
     * @param request
     * @return {@link RestResult}<{@link InitiateWalletActivationVo}>
     */
//    RestResult<InitiateWalletActivationVo> initiateAppWalletActivation(InitiateAppWalletActivationReq initiateWalletActivationReq, HttpServletRequest request);

    /**
     * 查看退回订单状态
     * @param apiRequest apiRequest
     * @param request request
     * @return {@link ApiResponse}
     */
    ApiResponse checkCashBack(ApiRequest apiRequest, HttpServletRequest request);
}
