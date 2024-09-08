package org.ar.wallet.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.result.RestResult;
import org.ar.wallet.service.HandleOrderTimeoutService;
import org.ar.wallet.service.IMatchingOrderService;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/orderTimeOut")
@RequiredArgsConstructor
@ApiIgnore
public class OrderTimeoutController {

    private final HandleOrderTimeoutService handleOrderTimeoutService;
    private final IMatchingOrderService matchingOrderService;

    /**
     * 钱包用户确认超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @PostMapping("/walletUserConfirmationTimeout")
    public Boolean walletUserConfirmationTimeout(@RequestParam("platformOrder") String platformOrder) {
        return handleOrderTimeoutService.handleWalletUserConfirmationTimeout(platformOrder);
    }


    /**
     * 商户会员确认超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @PostMapping("/merchantMemberConfirmationTimeout")
    public Boolean handleMerchantMemberConfirmationTimeout(@RequestParam("platformOrder") String platformOrder) {
        return handleOrderTimeoutService.handleMerchantMemberConfirmationTimeout(platformOrder);
    }

    /**
     * 钱包用户卖出匹配超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @PostMapping("/walletUserSaleMatchTimeout")
    public Boolean handleWalletUserSaleMatchTimeout(@RequestParam("platformOrder") String platformOrder, @RequestParam("lastUpdateTimestamp") Long lastUpdateTimestamp) {
        return handleOrderTimeoutService.handleWalletUserSaleMatchTimeout(platformOrder, lastUpdateTimestamp);
    }

    /**
     * 商户会员卖出匹配超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @PostMapping("/merchantMemberSaleMatchTimeout")
    public Boolean handleMerchantMemberSaleMatchTimeout(@RequestParam("platformOrder") String platformOrder, @RequestParam("lastUpdateTimestamp") Long lastUpdateTimestamp) {
        return handleOrderTimeoutService.handleMerchantMemberSaleMatchTimeout(platformOrder, lastUpdateTimestamp);
    }

    /**
     * 支付超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @PostMapping("/paymentTimeout")
    public Boolean handlePaymentTimeout(@RequestParam("platformOrder") String platformOrder) {
        return handleOrderTimeoutService.handlePaymentTimeout(platformOrder);
    }

    /**
     * USDT支付超时处理
     *
     * @param platformOrder
     * @return {@link Boolean}
     */
    @PostMapping("/usdtPaymentTimeout")
    public Boolean handleUsdtPaymentTimeout(@RequestParam("platformOrder") String platformOrder) {
        return handleOrderTimeoutService.handleUsdtPaymentTimeout(platformOrder);
    }

    @GetMapping("/cancelConfirmTimeoutOrder")
    @ApiOperation(value = "取消员确认超时一定时间的订单")
    public RestResult cancelConfirmTimeoutOrder(@RequestParam(value = "startDays", required = false) Integer startDays) {
        matchingOrderService.cancelConfirmTimeoutOrder(startDays);
        return RestResult.ok();
    }
}
