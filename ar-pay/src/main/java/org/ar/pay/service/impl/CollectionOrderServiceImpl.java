package org.ar.pay.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.web.utils.UserContext;
import org.ar.pay.Enum.CollectionOrderStatusEnum;
import org.ar.pay.Enum.NotifyStatusEnum;
import org.ar.pay.Enum.PayTypeEnum;
import org.ar.pay.Enum.SendStatusEnum;
import org.ar.pay.entity.AccountChange;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.mapper.AccountChangeMapper;
import org.ar.pay.mapper.ArBalanceMapper;
import org.ar.pay.mapper.CollectionOrderMapper;
import org.ar.pay.req.CollectionOrderReq;
import org.ar.pay.service.ICollectionOrderService;
import org.ar.pay.service.IMerchantInfoService;
import org.ar.pay.service.IPayConfigService;
import org.ar.pay.util.*;
import org.ar.pay.vo.CollectionOrderInfoVo;
import org.ar.pay.vo.CollectionOrderListVo;
import org.ar.pay.vo.selectListVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class CollectionOrderServiceImpl extends ServiceImpl<CollectionOrderMapper, CollectionOrder> implements ICollectionOrderService {
    private final AccountChangeMapper accountChangeMapper;
    private final ArBalanceMapper arBalanceMapper;
    private final RedisUtils redisUtils;
    //    private final RedissonUtil redissonUtil;
    private final IMerchantInfoService merchantInfoService;
    private final IPayConfigService payConfigService;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public PageReturn<CollectionOrderListVo> listPage(CollectionOrderReq req) {
        if (req == null) {
            req = new CollectionOrderReq();
        }
        Page<CollectionOrder> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<CollectionOrder> lambdaQuery = lambdaQuery();

        //--动态查询 商户号
        if (!StringUtils.isEmpty(req.getMerchantCode())) {
            lambdaQuery.eq(CollectionOrder::getMerchantCode, req.getMerchantCode());
        }

        //--动态查询 商户订单号
        if (!StringUtils.isEmpty(req.getMerchantOrder())) {
            lambdaQuery.eq(CollectionOrder::getMerchantOrder, req.getMerchantOrder());
        }

        //--动态查询 平台订单号
        if (!StringUtils.isEmpty(req.getPlatformOrder())) {
            lambdaQuery.eq(CollectionOrder::getPlatformOrder, req.getPlatformOrder());
        }

        //--动态查询 支付状态
        if (!StringUtils.isEmpty(req.getOrderStatus())) {
            lambdaQuery.eq(CollectionOrder::getOrderStatus, req.getOrderStatus());
        }

        //--动态查询 回调状态
        if (!StringUtils.isEmpty(req.getCallbackStatus())) {
            lambdaQuery.eq(CollectionOrder::getCallbackStatus, req.getCallbackStatus());
        }

        //--动态查询 币种
        if (!StringUtils.isEmpty(req.getCurrency())) {
            lambdaQuery.eq(CollectionOrder::getCurrency, req.getCurrency());
        }

        //--动态查询 类型(支付方式)
        if (!StringUtils.isEmpty(req.getPayType())) {
            lambdaQuery.eq(CollectionOrder::getPayType, req.getPayType());
        }

        //--动态查询 开始时间
        if (req.getStartTime() != null) {
            lambdaQuery.ge(CollectionOrder::getCreateTime, LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getStartTime() * 1000), ZoneId.systemDefault()));
        }

        //--动态查询 结束时间
        if (req.getEndTime() != null) {
            lambdaQuery.le(CollectionOrder::getCreateTime, LocalDateTime.ofInstant(Instant.ofEpochMilli(req.getEndTime() * 1000), ZoneId.systemDefault()));
        }

        // 倒序排序
        lambdaQuery.orderByDesc(CollectionOrder::getId);

        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<CollectionOrder> records = page.getRecords();

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<CollectionOrderListVo> collectionOrderListVos = new ArrayList<>();
        for (CollectionOrder record : records) {
            CollectionOrderListVo collectionOrderListVo = new CollectionOrderListVo();
            BeanUtil.copyProperties(record, collectionOrderListVo);
            collectionOrderListVo.setPayType(PayTypeEnum.getNameByCode(collectionOrderListVo.getPayType()));
            collectionOrderListVos.add(collectionOrderListVo);
        }
//        IPage<CollectionOrderListVo> convert = page.convert(CollectionOrder -> BeanUtil.copyProperties(CollectionOrder, CollectionOrderListVo.class));
        return PageUtils.flush(page, collectionOrderListVos);
    }

    @Override
    public List<CollectionOrder> getCollectionOrderBySatus() {

        LambdaQueryChainWrapper<CollectionOrder> lambdaQuery = lambdaQuery();
        List<CollectionOrder> list = lambdaQuery().eq(CollectionOrder::getOrderStatus, "2").list();
        return list;
    }

    /*
     * 更新订单状态和账变
     * p1 商户号
     * p2 平台订单号
     * p3 实际支付金额
     * p4 支付方式
     * */
    @Override
    @Transactional
    public boolean updateOrderByOrderNo(String merchantCode, String platformOrder, String realAmount, String payType) {

        log.info("商户号: {}, 平台订单号: {}, 更新账变...", merchantCode, platformOrder);

        boolean req = false;
        //根据平台订单号查询到该笔订单
//        LambdaQueryChainWrapper<CollectionOrder> lambdaQuery = lambdaQuery();
        CollectionOrder collectionOrder = lambdaQuery().eq(CollectionOrder::getPlatformOrder, platformOrder).one();

        if (StringUtils.isEmpty(collectionOrder.getCurrency())) {
            log.info("商户号: {}, 平台订单号: {}, 该笔订单币种不存在", merchantCode, platformOrder);
            return false;
        }

        //判断该笔订单提单金额和实际支付金额是否一致
        if (new BigDecimal(String.valueOf(collectionOrder.getAmount())).compareTo(new BigDecimal(realAmount)) != 0){
            log.info("商户号: {}, 平台订单号: {}, 该笔订单提单金额和支付金额不一致!", merchantCode, platformOrder);
            return false;
        }

        //加分布式锁
        String key = "AR-PAY" + collectionOrder.getCurrency();
        RedissonClient redissonClient = SpringContextUtil.getBean("redissonClient");
        RLock lock = redissonClient.getLock(key);
        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);
            AccountChange accountChange = new AccountChange();
            if (req) {
                //三方回调过来 判断该笔订单是否被处理过了 如果是处理过的 就直接返回True
                if (CollectionOrderStatusEnum.PAID.getCode().equals(collectionOrder.getOrderStatus())) {
                    log.info("商户号: {}, 平台订单号: {}, 更新账变接口: 该笔订单已被处理过了", merchantCode, platformOrder);
                    return true;
                }

                //查询币种余额
                BigDecimal balance = arBalanceMapper.getCurrentBlanceByCurrentce(collectionOrder.getCurrency());
                //设置账变前余额
                accountChange.setBeforeChange(balance);

                log.info("平台订单号: {}, 回调金额: {}, 商户号: {}, 币种: {}, 更新前金额: {}", platformOrder, realAmount, merchantCode, collectionOrder.getCurrency(), balance);

                //根据币种类型 更新币种余额
                boolean updateFlag = arBalanceMapper.updateBalanceByCurrence(balance, collectionOrder.getCurrency());

                BigDecimal amountChange = new BigDecimal(realAmount);
                balance = balance.add(amountChange);
                accountChange.setAfterChange(balance);
                accountChange.setAmountChange(amountChange);
                log.info("商户订单{}回调金额{}商户号{}币种{}更新后金额{}", platformOrder, realAmount, merchantCode, collectionOrder.getCurrency(), balance);
//                 if(updateFlag==true){
//                     log.info("商户订单{}回调金额{}商户号{}币种{}更新后金额{}",orderId,realAmount,merchantNo,collectionOrder.getCurrency(),balance);
//                 }else{
//                     throw new RuntimeException("商户号："+merchantNo+"订单号："+orderId+"更新失败");
//                 }

                accountChange.setMerchantCode(merchantCode);
                accountChange.setCurrentcy(collectionOrder.getCurrency());
                accountChange.setType(payType);
                accountChange.setOrderNo(platformOrder);
                accountChange.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));


                int i = accountChangeMapper.insert(accountChange);
//             if(i==0){
//                 throw new RuntimeException("商户号："+merchantNo+"订单号："+orderId+"账变出错");
//             }
                //将订单状态改为代收成功 --2
                collectionOrder.setOrderStatus(CollectionOrderStatusEnum.PAID.getCode());

                //添加该笔订单实际支付金额
                collectionOrder.setCollectedAmount(amountChange);
                boolean updateOrderStatus = this.updateById(collectionOrder);
                return updateOrderStatus;
            }
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    /*
     * 手动回调
     * */
    @Override
    public RestResult manualCallback(String merchantOrder) {

        log.info("手动回调商户订单号: {}, 手动回调...", merchantOrder);

        //根据商户订单号定位到该笔订单
        QueryWrapper<CollectionOrder> collectionOrderQueryWrapper = new QueryWrapper<>();
        collectionOrderQueryWrapper.eq("merchant_order", merchantOrder);
        CollectionOrder collectionOrder = getOne(collectionOrderQueryWrapper);
        if (collectionOrder != null) {
            Map<String, Object> dataMap = new HashMap<>();
            //商户号
            dataMap.put("merchantCode", collectionOrder.getMerchantCode());

            //平台订单号
            dataMap.put("platformOrder", collectionOrder.getPlatformOrder());

            //商户订单号
            dataMap.put("merchantOrder", collectionOrder.getMerchantOrder());

            //回调地址
            dataMap.put("notifyUrl", collectionOrder.getNotifyUrl());

            //回调金额
            dataMap.put("amount", collectionOrder.getAmount());

            //md5签名
            String signinfo = SignUtil.sortData(dataMap, "&");

            log.info("手动回调平台订单号: {}, dataMap: {}, 手动回调商户签名串: {}", collectionOrder.getPlatformOrder(), dataMap, signinfo);

            String sign = SignAPI.sign(signinfo, merchantInfoService.getMd5KeyByCode(collectionOrder.getMerchantCode()));
            dataMap.put("sign", sign);

            //封装整体参数
            HashMap<String, Object> reqMap = new HashMap<>();
            reqMap.put("code", ResultCode.SUCCESS.getCode());
            reqMap.put("data", dataMap);
            reqMap.put("msg", "OK");

            String reqinfo = JSON.toJSONString(reqMap);

            log.info("手动回调平台订单号: {}, 手动回调商户请求地址: {}, 手动回调商户请求数据: {}", collectionOrder.getPlatformOrder(), collectionOrder.getNotifyUrl(), reqinfo);

            CollectionOrderServiceImpl collectionOrderService = SpringContextUtil.getBean(CollectionOrderServiceImpl.class);

            try {
                String resultCallBack = RequestUtil.HttpRestClientToJson(collectionOrder.getNotifyUrl(), reqinfo);
                log.info("手动回调平台订单号: {}, 手动回调商户返回数据: {}", collectionOrder.getPlatformOrder(), resultCallBack);
                if ("SUCCESS".equals(resultCallBack)) {
                    log.info("手动回调平台订单号: {}, 手动回调成功: {}", collectionOrder.getPlatformOrder(), resultCallBack);
                    //更新订单回调状态为4 --手动回调成功
                    collectionOrder.setCallbackStatus(NotifyStatusEnum.MANUAL_SUCCESS.getCode());
                    //更新订单回调时间
                    collectionOrder.setCallbackTime(LocalDateTime.now(ZoneId.systemDefault()));
                    collectionOrderService.updateById(collectionOrder);
                    return RestResult.ok();
                } else {
                    //更新订单回调状态为5 --手动回调失败
                    log.info("手动回调平台订单号: {}, 手动回调失败: {}, 商户未返回SUCCESS", collectionOrder.getPlatformOrder(), resultCallBack);
                    collectionOrder.setCallbackStatus(NotifyStatusEnum.MANUAL_FAILED.getCode());
                    collectionOrderService.updateById(collectionOrder);
                    return RestResult.failed("回调失败");
                }
            } catch (Exception e) {
                //更新订单回调状态为5 --手动回调失败
                log.info("手动回调平台订单号: {}, 手动回调失败", collectionOrder.getPlatformOrder());
                collectionOrder.setCallbackStatus(NotifyStatusEnum.MANUAL_FAILED.getCode());
                collectionOrderService.updateById(collectionOrder);
                throw new RuntimeException(e);
            }
        } else {
            log.info("手动回调商户订单号: {}, 手动回调失败, 该订单不存在", merchantOrder);
            return RestResult.failed("该订单不存在");
        }
    }


    @Override
    public RestResult getCollectionOrderInfoByOrderNo(String merchantOrder) {
        //根据订单号查询代收订单详情
        QueryWrapper<CollectionOrder> collectionOrderQueryWrapper = new QueryWrapper<>();
        collectionOrderQueryWrapper.select("merchant_code", "amount", "collected_amount", "order_rate", "currency",
                "create_time", "callback_time", "order_status", "pay_type", "cost", "third_code").eq("merchant_order", merchantOrder);
        CollectionOrder collectionOrder = getOne(collectionOrderQueryWrapper);

        if (collectionOrder != null) {
            CollectionOrderInfoVo collectionOrderInfoVo = new CollectionOrderInfoVo();
            BeanUtil.copyProperties(collectionOrder, collectionOrderInfoVo);

            //通过商户号查询商户名称
            QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
            merchantInfoQueryWrapper.select("username").eq("code", collectionOrder.getMerchantCode());
            MerchantInfo merchantInfo = merchantInfoService.getOne(merchantInfoQueryWrapper);
            if (merchantInfo != null) {
                collectionOrderInfoVo.setUsername(merchantInfo.getUsername());
            }
            System.out.println("collectionOrderInfoVo: " + collectionOrderInfoVo);

            //通过三方代码 查询支付通道名称
            QueryWrapper<PayConfig> payConfigQueryWrapper = new QueryWrapper<>();
            payConfigQueryWrapper.select("channel").eq("third_code", collectionOrder.getThirdCode());
            PayConfig payConfig = payConfigService.getOne(payConfigQueryWrapper);
            if (payConfig != null) {
                collectionOrderInfoVo.setChannel(payConfig.getChannel());
            }
            //匹配支付类型枚举值 将支付类型名称返回给前端
            collectionOrderInfoVo.setPayType(PayTypeEnum.getNameByCode(collectionOrder.getPayType()));
            return RestResult.ok(collectionOrderInfoVo);
        } else {

            return RestResult.failed("该笔订单不存在");
        }

    }

    /*
     * 查询下拉列表数据(币种,支付类型)
     * */
    @Override
    public RestResult selectList() {
        //获取当前用户的商户ID
        Long currentUserId = UserContext.getCurrentUserId();


        //查询该商户存在的币种和支付类型
        //币种  一个商户只对应一个币种
        //查询该商户的币种字段
        QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
        merchantInfoQueryWrapper.select("currency").eq("id", currentUserId);
        MerchantInfo merchantInfo = merchantInfoService.getOne(merchantInfoQueryWrapper);

        selectListVo selectListVo = new selectListVo();

        if (merchantInfo != null) {
            //设置币种
            JSONObject currencyJson = new JSONObject();
            currencyJson.put("value", merchantInfo.getCurrency());
            currencyJson.put("label", merchantInfo.getCurrency());
            ArrayList<JSONObject> currencyList = new ArrayList<>();
            currencyList.add(currencyJson);
            selectListVo.setCurrency(currencyList);

            //根据用户的币种 查询目前使用该币种的所有三方通道 取到三方通道的支付类型列表
            QueryWrapper<PayConfig> payConfigQueryWrapper = new QueryWrapper<>();
            payConfigQueryWrapper.select("pay_type").in("currency", merchantInfo.getCurrency());
            List<Map<String, Object>> maps = payConfigService.listMaps(payConfigQueryWrapper);

            List<JSONObject> payTypeList = new ArrayList<>();
            for (Map<String, Object> map : maps) {
                JSONObject payTypeJson = new JSONObject();
                payTypeJson.put("value", map.get("pay_type"));
                payTypeJson.put("label", PayTypeEnum.getNameByCode(String.valueOf(map.get("pay_type"))));
                payTypeList.add(payTypeJson);
            }
            //设置支付类型
            selectListVo.setPayType(payTypeList);
            return RestResult.ok(selectListVo);
        } else {
            return RestResult.failed("商户不存在");
        }
    }

    /*
     * 根据id更改订单已发送状态
     * */
    @Override
    public int updateOrderSendById(String id) {
        LambdaUpdateWrapper<CollectionOrder> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(CollectionOrder::getId, id).set(CollectionOrder::getSend, SendStatusEnum.HAS_BEEN_SENT.getCode());
        return getBaseMapper().update(null, lambdaUpdateWrapper);
    }
}
