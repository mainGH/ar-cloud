package org.ar.wallet.controller;


import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.pay.dto.MemberOrderOverviewDTO;
import org.ar.common.pay.dto.PaymentOrderExportDTO;
import org.ar.common.pay.dto.PaymentOrderInfoDTO;
import org.ar.common.pay.dto.PaymentOrderListPageDTO;
import org.ar.common.pay.req.CommonDateLimitReq;
import org.ar.common.pay.req.PaymentOrderGetInfoReq;
import org.ar.common.pay.req.PaymentOrderIdReq;
import org.ar.common.pay.req.PaymentOrderListPageReq;
import org.ar.common.web.exception.BizException;
import org.ar.wallet.Enum.ChangeModeEnum;
import org.ar.wallet.Enum.MemberAccountChangeEnum;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.entity.CollectionOrder;
import org.ar.wallet.entity.MatchingOrder;
import org.ar.wallet.entity.PaymentOrder;
import org.ar.wallet.mapper.CollectionOrderMapper;
import org.ar.wallet.mapper.MatchingOrderMapper;
import org.ar.wallet.req.SellOrderListReq;
import org.ar.wallet.service.ICollectionOrderService;
import org.ar.wallet.service.IPaymentOrderService;
import org.ar.wallet.util.AmountChangeUtil;
import org.ar.wallet.vo.CollectionOrderInfoVo;
import org.ar.wallet.vo.SellOrderListVo;
import org.ar.wallet.vo.ViewSellOrderDetailsVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;


/**
 * @author Admin
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/api/v1/paymentOrder", "/paymentOrder"})
@Api(description = "代付订单控制器")
@ApiIgnore
public class PaymentOrderController {

    private final IPaymentOrderService paymentOrderService;
    private final MatchingOrderMapper matchingOrderMapper;
    private final CollectionOrderMapper collectionOrderMapper;
    private final ICollectionOrderService iCollectionOrderService;
    private final AmountChangeUtil amountChangeUtil;


    @PostMapping("/sellOrderList")
    @ApiOperation(value = "查询卖出订单列表")
    public RestResult<List<SellOrderListVo>> sellOrderList(@RequestBody(required = false) @ApiParam SellOrderListReq sellOrderListReq) {
        List<SellOrderListVo> sellOrderListVo = paymentOrderService.sellOrderList(sellOrderListReq);
        return RestResult.ok(sellOrderListVo);
    }

    @GetMapping("/viewSellOrderDetails")
    @ApiOperation(value = "查看卖出订单详情")
    public RestResult<ViewSellOrderDetailsVo> viewSellOrderDetails(@ApiParam("订单号") String platformOrder) {
        ViewSellOrderDetailsVo viewBuyOrderDetailsVo = paymentOrderService.viewSellOrderDetails(platformOrder);
        return RestResult.ok(viewBuyOrderDetailsVo);
    }

    @PostMapping("/save")
    @ApiOperation(value = "保存代付订单")
    public RestResult<PaymentOrder> save(@RequestBody @ApiParam PaymentOrder paymentOrder) {
        paymentOrderService.save(paymentOrder);
        return RestResult.ok(paymentOrder);
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新代付订单")
    public RestResult update(@RequestBody @ApiParam PaymentOrder paymentOrder) {
        boolean su = paymentOrderService.updateById(paymentOrder);
        return RestResult.ok();
    }

    @PostMapping("/listPage")
    @ApiOperation(value = "订单列表")
    public RestResult<List<PaymentOrderListPageDTO>> listPage(@RequestBody(required = false) @ApiParam PaymentOrderListPageReq req) {
        PageReturn<PaymentOrderListPageDTO> roleVOPage = paymentOrderService.listPage(req);
        return RestResult.page(roleVOPage);
    }

    @PostMapping("/listPageExport")
    @ApiOperation(value = "订单列表")
    public RestResult<List<PaymentOrderExportDTO>> listPageExport(@RequestBody(required = false) @ApiParam PaymentOrderListPageReq req) {
        PageReturn<PaymentOrderExportDTO> roleVOPage = paymentOrderService.listPageExport(req);
        return RestResult.page(roleVOPage);
    }


    @PostMapping("/listRecordPage")
    @ApiOperation(value = "订单记录")
    public RestResult<List<PaymentOrderListPageDTO>> listRecordPage(@RequestBody(required = false) @ApiParam PaymentOrderListPageReq req) {
        PageReturn<PaymentOrderListPageDTO> roleVOPage = paymentOrderService.listRecordPage(req);
        return RestResult.page(roleVOPage);
    }


    @PostMapping("/listRecordTotalPage")
    @ApiOperation(value = "订单总计")
    public RestResult<PaymentOrderListPageDTO> listRecordTotalPage(@RequestBody(required = false) @ApiParam PaymentOrderListPageReq req) {
        PaymentOrderListPageDTO paymentOrderListPageDTO = paymentOrderService.listRecordTotalPage(req);
        return RestResult.ok(paymentOrderListPageDTO);
    }

    @PostMapping("/getInfo")
    @ApiOperation(value = "查看")
    public RestResult<PaymentOrderInfoDTO> getInfo(@RequestBody(required = false) @ApiParam PaymentOrderGetInfoReq req) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(req.getId());
        paymentOrder = paymentOrderService.getById(paymentOrder);
        //paymentOrder.setRemark(req.getRemark());
        //paymentOrderService.updateById(paymentOrder);
        PaymentOrderInfoDTO paymentOrderInfoDTO = new PaymentOrderInfoDTO();
        BeanUtils.copyProperties(paymentOrder,paymentOrderInfoDTO);
        return RestResult.ok(paymentOrderInfoDTO);
    }


    @PostMapping("/cancel")
    @ApiOperation(value = "取消订单")
    public RestResult<PaymentOrderListPageDTO> cancel(@RequestBody(required = false) @ApiParam PaymentOrderIdReq req) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setId(req.getId());
        paymentOrder = paymentOrderService.getById(paymentOrder);
        paymentOrder.setRemark(req.getRemark());
        paymentOrder.setCancelBy(req.getOpName());
        paymentOrder.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());
        paymentOrder.setCancelTime(LocalDateTime.now(ZoneId.systemDefault()));
        paymentOrderService.updateById(paymentOrder);
        amountChangeUtil.insertMemberChangeAmountRecord(paymentOrder.getMemberId(), paymentOrder.getAmount(), ChangeModeEnum.ADD, "ARB", paymentOrder.getPlatformOrder(),  MemberAccountChangeEnum.CANCEL_RETURN, req.getOpName());
        MatchingOrder matchingOrder = matchingOrderMapper.selectMatchingOrderByWithdrawOrder(paymentOrder.getPlatformOrder());
        if(!ObjectUtils.isEmpty(matchingOrder)){
            matchingOrder.setStatus(OrderStatusEnum.WAS_CANCELED.getCode());
            matchingOrder.setCancelBy(req.getOpName());
            matchingOrder.setCancelTime(LocalDateTime.now());
            matchingOrderMapper.updateById(matchingOrder);

            CollectionOrder collectionOrder = collectionOrderMapper.getOrderByOrderNo(matchingOrder.getCollectionPlatformOrder());
            if(ObjectUtils.isEmpty(collectionOrder)){
                throw new BizException(ResultCode.RECHARGE_ORDER_NOT_EXIST);
            }
            collectionOrder.setCancelBy(req.getOpName());
            collectionOrder.setCancelTime(LocalDateTime.now(ZoneId.systemDefault()));
            collectionOrder.setOrderStatus(OrderStatusEnum.WAS_CANCELED.getCode());
            collectionOrderMapper.updateById(collectionOrder);
        }

        PaymentOrderListPageDTO paymentOrderInfoDTO = new PaymentOrderListPageDTO();
        BeanUtils.copyProperties(paymentOrder,paymentOrderInfoDTO);
        return RestResult.ok(paymentOrderInfoDTO);
    }

    @PostMapping("/manualCallback")
    @ApiOperation(value = "卖出手动回调成功")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "记录id", required = true, dataType = "Long"),
            @ApiImplicitParam(name = "opName", value = "操作id", required = true, dataType = "String")
    })
    public RestResult<Boolean> manualCallback(Long id, String opName) {

        Boolean result = paymentOrderService.manualCallback(id, opName);
        return RestResult.ok(result);
    }


    @GetMapping("/paymentOrderInfo")
    @ApiOperation(value = "查询代付订单详情")
    public RestResult<CollectionOrderInfoVo> paymentOrderInfo(@ApiParam(name = "商户订单号") String merchantOrder) {
        return paymentOrderService.getPaymentOrderInfoByOrderNo(merchantOrder);
    }

    @PostMapping("/getUsdtData")
    @ApiOperation(value = "查询usdt概览数据")
    public RestResult<MemberOrderOverviewDTO> getUsdtData(@RequestBody @ApiParam CommonDateLimitReq req) {
        MemberOrderOverviewDTO usdtData = paymentOrderService.getUsdtData(req);
        return RestResult.ok(usdtData);
    }



}
