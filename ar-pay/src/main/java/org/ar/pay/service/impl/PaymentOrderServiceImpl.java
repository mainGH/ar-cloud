package org.ar.pay.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.pay.Enum.PayTypeEnum;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.entity.PaymentOrder;
import org.ar.pay.mapper.PaymentOrderMapper;
import org.ar.pay.req.PaymentOrderReq;
import org.ar.pay.service.IMerchantInfoService;
import org.ar.pay.service.IPayConfigService;
import org.ar.pay.service.IPaymentOrderService;
import org.ar.pay.util.PageUtils;
import org.ar.pay.vo.PaymentOrderInfoVo;
import org.ar.pay.vo.PaymentOrderListVo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * @author
 */
@Service
@RequiredArgsConstructor
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder> implements IPaymentOrderService {
    private final IMerchantInfoService merchantInfoService;
    private final IPayConfigService payConfigService;

    @Override
    public PageReturn<PaymentOrderListVo> listPage(PaymentOrderReq req) {
        if (req == null) {
            req = new PaymentOrderReq();
        }
        Page<PaymentOrder> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<PaymentOrder> lambdaQuery = lambdaQuery();

        //--动态查询 商户号
        if (!StringUtils.isEmpty(req.getMerchantCode())) {
            lambdaQuery.eq(PaymentOrder::getMerchantCode, req.getMerchantCode());
        }

        //--动态查询 商户订单号
        if (!StringUtils.isEmpty(req.getMerchantOrder())) {
            lambdaQuery.eq(PaymentOrder::getMerchantOrder, req.getMerchantOrder());
        }

        //--动态查询 平台订单号
        if (!StringUtils.isEmpty(req.getPlatformOrder())) {
            lambdaQuery.eq(PaymentOrder::getPlatformOrder, req.getPlatformOrder());
        }

        //--动态查询 支付状态
        if (!StringUtils.isEmpty(req.getOrderStatus())) {
            lambdaQuery.eq(PaymentOrder::getOrderStatus, req.getOrderStatus());
        }

        //--动态查询 回调状态
        if (!StringUtils.isEmpty(req.getCallbackStatus())) {
            lambdaQuery.eq(PaymentOrder::getCallbackStatus, req.getCallbackStatus());
        }

        //--动态查询 币种
        if (!StringUtils.isEmpty(req.getCurrentcy())) {
            lambdaQuery.eq(PaymentOrder::getCurrentcy, req.getCurrentcy());
        }

        //--动态查询 类型(支付方式)
        if (!StringUtils.isEmpty(req.getPayType())) {
            lambdaQuery.eq(PaymentOrder::getPayType, req.getPayType());
        }

        //--动态查询 开始时间
        if (req.getStartTime() != null) {
            lambdaQuery.ge(PaymentOrder::getCreateTime, LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getStartTime() * 1000), ZoneId.systemDefault()));
        }

        //--动态查询 结束时间
        if (req.getEndTime() != null) {
            lambdaQuery.le(PaymentOrder::getCreateTime, LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getEndTime() * 1000), ZoneId.systemDefault()));
        }

        lambdaQuery.orderByDesc(PaymentOrder::getId);


        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<PaymentOrder> records = page.getRecords();

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<PaymentOrderListVo> paymentOrderListVos = new ArrayList<>();
        for (PaymentOrder record : records) {
            PaymentOrderListVo paymentOrderListVo = new PaymentOrderListVo();
            BeanUtil.copyProperties(record, paymentOrderListVo);
            paymentOrderListVo.setPayType(PayTypeEnum.getNameByCode(paymentOrderListVo.getPayType()));
            paymentOrderListVos.add(paymentOrderListVo);
        }

        return PageUtils.flush(page, paymentOrderListVos);
    }

    @Override
    public RestResult getPaymentOrderInfoByOrderNo(String merchantOrder) {
        //根据订单号查询代付订单详情
        QueryWrapper<PaymentOrder> paymentOrderQueryWrapper = new QueryWrapper<>();
        paymentOrderQueryWrapper.select("settlement_amount", "cost", "currentcy", "create_time",
                "callback_time", "order_status", "pay_type", "account_name", "account_number", "third_code", "merchant_code").eq("merchant_order", merchantOrder);
        PaymentOrder paymentOrder = getOne(paymentOrderQueryWrapper);

        System.out.println("paymentOrder: " + paymentOrder);

        if (paymentOrder != null) {
            PaymentOrderInfoVo paymentOrderInfoVo = new PaymentOrderInfoVo();
            BeanUtil.copyProperties(paymentOrder, paymentOrderInfoVo);
            //订单金额
            paymentOrderInfoVo.setAmount(paymentOrder.getSettlementAmount());
            //实际金额
            paymentOrderInfoVo.setCollectedAmount(paymentOrder.getSettlementAmount());

            //通过商户号查询商户名称
            QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
            merchantInfoQueryWrapper.select("username").eq("code", paymentOrder.getMerchantCode());
            MerchantInfo merchantInfo = merchantInfoService.getOne(merchantInfoQueryWrapper);
            if (merchantInfo != null) {
                paymentOrderInfoVo.setUsername(merchantInfo.getUsername());
            }
            System.out.println("paymentOrderInfoVo: " + paymentOrderInfoVo);

            //通过三方代码 查询支付通道名称
            QueryWrapper<PayConfig> payConfigQueryWrapper = new QueryWrapper<>();
            payConfigQueryWrapper.select("channel").eq("third_code", paymentOrder.getThirdCode());
            PayConfig payConfig = payConfigService.getOne(payConfigQueryWrapper);
            if (payConfig != null) {
                paymentOrderInfoVo.setChannel(payConfig.getChannel());
            }
            return RestResult.ok(paymentOrderInfoVo);
        } else {
            return RestResult.failed("该笔订单不存在");
        }


    }


}
