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
import org.ar.wallet.Enum.MemberOperationModuleEnum;
import org.ar.wallet.annotation.LogMemberOperation;
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

import static org.ar.common.core.result.ResultCode.NOT_MATCHED_QUICK_BUY_ORDER;

/**
 * @author
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/kycBuyCenter")
@Api(description = "前台-买入控制器")
@Validated
@Slf4j
public class KycBuyController {

    private final IBuyService buyService;
    private final IUsdtBuyOrderService usdtBuyOrderService;
    private final ICollectionOrderService collectionOrderService;
    private final IAppealOrderService appealOrderService;
    private final QuickBuyService quickBuyService;



    @GetMapping("/getPaymentType")
    @ApiOperation(value = "前台-获取支付类型")
    public RestResult<List<PaymentTypeVo>> getPaymentType() {
        //获取支付类型
        return buyService.getPaymentType();
    }


    @PostMapping("/kycBuy")
    @ApiOperation(value = "前台-买入下单接口")
    @LogMemberOperation(value = MemberOperationModuleEnum.BUY_ORDER)
    public RestResult buy(@RequestBody @ApiParam @Valid BuyReq buyReq, HttpServletRequest request) {
        //买入处理
        return buyService.buyProcessor(buyReq, request);
    }



}
