package org.ar.pay.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.req.CollectionOrderReq;
import org.ar.pay.service.ICollectionOrderService;
import org.ar.pay.vo.CollectionOrderInfoVo;
import org.ar.pay.vo.CollectionOrderListVo;
import org.ar.pay.vo.CollectionOrderVo;
import org.ar.pay.vo.selectListVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/collectionOrder")
@Api(description = "代收订单控制器")
public class CollectionOrderController {

    private final ICollectionOrderService collectionOrderService;

    @PostMapping("/save")
    @ApiOperation(value = "保存代收订单")
    public RestResult<MerchantInfo> save(@RequestBody @ApiParam CollectionOrderVo collectionOrderVo) {
        CollectionOrder collectionOrder = new CollectionOrder();
        BeanUtils.copyProperties(collectionOrderVo, collectionOrder);
        collectionOrderService.save(collectionOrder);
        return RestResult.ok();
    }

    @PostMapping("/update")
    @ApiOperation(value = "更新代收订单")
    public RestResult update(@RequestBody @ApiParam CollectionOrder collectionOrder) {
        boolean su = collectionOrderService.updateById(collectionOrder);
        return RestResult.ok();
    }

    @PostMapping("/list")
    @ApiOperation(value = "查询代收订单列表")
    public RestResult<PageReturn> list(@RequestBody(required = false) @ApiParam CollectionOrderReq collectionOrderReq) {
        PageReturn<CollectionOrderListVo> collectionOrderPage = collectionOrderService.listPage(collectionOrderReq);
        return RestResult.ok(collectionOrderPage);
    }

    @GetMapping("/collectionOrderInfo")
    @ApiOperation(value = "查询代收订单详情")
    public RestResult<CollectionOrderInfoVo> collectionOrderInfo(@ApiParam(name = "商户订单号") String merchantOrder) {
        return collectionOrderService.getCollectionOrderInfoByOrderNo(merchantOrder);
    }

    @GetMapping("/selectList")
    @ApiOperation(value = "查询币种,支付类型列表")
    public RestResult<selectListVo> selectList() {
        return collectionOrderService.selectList();
    }
}
