package org.ar.wallet.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.UsdtBuyOrderDTO;
import org.ar.common.pay.dto.UsdtBuyOrderInfoDTO;
import org.ar.common.pay.req.UsdtBuyOrderGetInfoReq;
import org.ar.common.pay.req.UsdtBuyOrderIdReq;
import org.ar.common.pay.req.UsdtBuyOrderReq;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.ChangeModeEnum;
import org.ar.wallet.Enum.CurrenceEnum;
import org.ar.wallet.Enum.MemberAccountChangeEnum;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.entity.TradeConfig;
import org.ar.wallet.entity.UsdtBuyOrder;
import org.ar.wallet.service.ICollectionOrderService;
import org.ar.wallet.service.ITradeConfigService;
import org.ar.wallet.service.IUsdtBuyOrderService;
import org.ar.wallet.util.AmountChangeUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author
 */

@Slf4j
@RestController
@RequiredArgsConstructor
@Api(description = "usdt买入订单")
@RequestMapping(value = {"/api/v1/usdtBuyOrder", "/usdtBuyOrder"})
@ApiIgnore
public class UsdtBuyOrderController {
    private final IUsdtBuyOrderService usdtBuyOrderService;
    private final ICollectionOrderService iCollectionOrderService;
    private final ITradeConfigService tradeConfigService;
    private final AmountChangeUtil amountChangeUtil;

    @PostMapping("/listpage")
    @ApiOperation(value = "usdt订单买入列表")
    public RestResult<List<UsdtBuyOrderDTO>> listpage(@RequestBody @ApiParam UsdtBuyOrderReq req) {
        PageReturn<UsdtBuyOrderDTO> payConfigPage = usdtBuyOrderService.listPage(req);
        return RestResult.page(payConfigPage);
    }

    @PostMapping("/getInfo")
    @ApiOperation(value = "查看")
    public RestResult<UsdtBuyOrderInfoDTO> getInfo(@RequestBody @ApiParam UsdtBuyOrderGetInfoReq req) {
        UsdtBuyOrder usdtBuyOrder = new UsdtBuyOrder();
        usdtBuyOrder.setId(req.getId());
        usdtBuyOrder = usdtBuyOrderService.getById(usdtBuyOrder);
        UsdtBuyOrderInfoDTO usdtBuyOrderInfoDTO = new UsdtBuyOrderInfoDTO();
        usdtBuyOrderInfoDTO.setUsdtProof(usdtBuyOrder.getUsdtProof());
        usdtBuyOrderInfoDTO.setId(usdtBuyOrder.getId());
        return RestResult.ok(usdtBuyOrderInfoDTO);
    }


    @PostMapping("/pay")
    @ApiOperation(value = "支付")
    public RestResult<UsdtBuyOrderDTO> pay(@RequestBody @ApiParam UsdtBuyOrderIdReq req) {

        TradeConfig tradeConfig = tradeConfigService.getById(1);
        req.setUpdateBy(UserContext.getCurrentUserName());
        UsdtBuyOrder usdtBuyOrder = new UsdtBuyOrder();
        usdtBuyOrder.setId(req.getId());
        usdtBuyOrder = usdtBuyOrderService.getById(usdtBuyOrder);
        if(usdtBuyOrder.getStatus().equals(OrderStatusEnum.SUCCESS.getCode())){
            return RestResult.failed();
        }
        BigDecimal calculatedArbAmount = req.getUsdtActualNum().multiply(tradeConfig.getUsdtCurrency()).setScale(2, RoundingMode.HALF_UP);
        usdtBuyOrder.setStatus(OrderStatusEnum.SUCCESS.getCode());
        usdtBuyOrder.setUsdtNum(req.getUsdtActualNum());
        usdtBuyOrder.setArbNum(calculatedArbAmount);
        usdtBuyOrder.setRemark(req.getRemark());
        usdtBuyOrderService.updateById(usdtBuyOrder);
        amountChangeUtil.insertMemberChangeAmountRecord(usdtBuyOrder.getMemberId(), calculatedArbAmount, ChangeModeEnum.ADD, CurrenceEnum.ARB.getCode(), usdtBuyOrder.getPlatformOrder(),  MemberAccountChangeEnum.USDT_RECHARGE, req.getUpdateBy());
        UsdtBuyOrderDTO usdtBuyOrderInfoDTO = new UsdtBuyOrderDTO();
        BeanUtils.copyProperties(usdtBuyOrder,usdtBuyOrderInfoDTO);
        return RestResult.ok(usdtBuyOrderInfoDTO);
    }

    @PostMapping("/nopay")
    @ApiOperation(value = "未支付")
    public RestResult<UsdtBuyOrderDTO> nopay(@RequestBody @ApiParam UsdtBuyOrderIdReq req) {
        UsdtBuyOrder usdtBuyOrder = new UsdtBuyOrder();
        usdtBuyOrder.setId(req.getId());

        usdtBuyOrder = usdtBuyOrderService.getById(usdtBuyOrder);
        if(usdtBuyOrder.getStatus().equals(OrderStatusEnum.SUCCESS.getCode())){
            return RestResult.failed();
        }
        usdtBuyOrder.setStatus("10");
        usdtBuyOrder.setRemark(req.getRemark());
        usdtBuyOrder.setUsdtActualNum(req.getUsdtActualNum());
        usdtBuyOrderService.updateById(usdtBuyOrder);
        UsdtBuyOrderDTO usdtBuyOrderDTO = new UsdtBuyOrderDTO();
        BeanUtils.copyProperties(usdtBuyOrder,usdtBuyOrderDTO);
        return RestResult.ok(usdtBuyOrderDTO);
    }

}
