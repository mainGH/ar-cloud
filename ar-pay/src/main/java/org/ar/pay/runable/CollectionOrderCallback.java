package org.ar.pay.runable;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.ResultCode;
import org.ar.pay.Enum.NotifyStatusEnum;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.service.impl.CollectionOrderServiceImpl;
import org.ar.pay.util.RequestUtil;
import org.ar.pay.util.SignAPI;
import org.ar.pay.util.SignUtil;
import org.ar.pay.util.SpringContextUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;


@Slf4j
public class CollectionOrderCallback implements Runnable {
    private final CountDownLatch countDownLatch;

    private CollectionOrder collectionOrder;
    private MerchantInfo merchantInfo;

    public CollectionOrderCallback(CountDownLatch countDownLatch, MerchantInfo merchantInfo, CollectionOrder collectionOrder) {
        this.countDownLatch = countDownLatch;
        this.merchantInfo = merchantInfo;
        this.collectionOrder = collectionOrder;


    }

    @Override
    public void run() {
        try {
            //定时任务执行 --回调商户
            doWork(collectionOrder, merchantInfo);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            countDownLatch.countDown();
        }
    }

//    public static void main(String[] args) {
//        Map<String, Object> dataMap = new HashMap<String, Object>();
//        //商户号
//        dataMap.put("merchantCode", "test");
//
//        //平台订单号
//        dataMap.put("platformOrder", "AR2023100414354986978");
//
//        //商户订单号
//        dataMap.put("merchantOrder", "2023100414354986978");
//
//        //回调地址
//        dataMap.put("notifyUrl", "https://7f1b-91-74-44-172.ngrok.io");
//
//        //回调金额
//        dataMap.put("amount", "200");
//
//        //md5签名
//        String signinfo = SignUtil.sortData(dataMap, "&");
//        System.out.println("signinfo: " + signinfo);
//        String sign = SignAPI.sign(signinfo, "d95f8be038b565f1deb5e251a8941bd5");
//        dataMap.put("sign", sign);
//        System.out.println("sign: " + sign);
//    }

    /**
     * 定时任务回调商户
     */
    private void doWork(CollectionOrder collectionOrder, MerchantInfo merchantInfo) throws Exception {

        log.info("定时任务执行回调商户, collectionOrder: {}, merchantInfo: {}", collectionOrder, merchantInfo);

        Thread.sleep(Math.abs(new Random().nextInt() % 10000));

        Map<String, Object> dataMap = new HashMap<String, Object>();
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

        log.info("定时任务执行回调商户, 平台订单号: {}, dataMap: {}", collectionOrder.getPlatformOrder(), dataMap);

        //md5签名
        String signinfo = SignUtil.sortData(dataMap, "&");
        String sign = SignAPI.sign(signinfo, merchantInfo.getMd5Key());

        log.info("定时任务执行回调商户, 平台订单号: {}, 签名串: {}, 签名值: {}", collectionOrder.getPlatformOrder(), signinfo, sign);

        dataMap.put("sign", sign);

        //封装整体参数
        HashMap<String, Object> reqMap = new HashMap<>();
        reqMap.put("code", ResultCode.SUCCESS.getCode());
        reqMap.put("data", dataMap);
        reqMap.put("msg", "OK");

        String reqinfo = JSON.toJSONString(reqMap);

        log.info("定时任务执行回调商户, 平台订单号: {}, 回调地址: {}, 回调参数: {}", collectionOrder.getPlatformOrder(), collectionOrder.getNotifyUrl(), reqinfo);

        String resultCallBack = RequestUtil.HttpRestClientToJson(collectionOrder.getNotifyUrl(), reqinfo);

        log.info("定时任务执行回调商户, 平台订单号: {}, 商户返回数据: {}", collectionOrder.getPlatformOrder(), resultCallBack);

        CollectionOrderServiceImpl collectionOrderService = SpringContextUtil.getBean(CollectionOrderServiceImpl.class);
        if (resultCallBack.equals("SUCCESS")) {
            //更新订单回调状态为2 --自动回调成功
            log.info("定时任务执行回调商户, 平台订单号: {}, 自动回调成功: {}", collectionOrder.getPlatformOrder(), resultCallBack);
            collectionOrder.setCallbackStatus(NotifyStatusEnum.SUCCESS.getCode());

            //更新订单回调时间
            collectionOrder.setCallbackTime(LocalDateTime.now(ZoneId.systemDefault()));
            collectionOrderService.updateById(collectionOrder);
        }else{
            //更新订单回调状态为3 --自动回调失败
            log.info("定时任务执行回调商户, 平台订单号: {}, 自动回调失败: {}, 商户未返回SUCCESS", collectionOrder.getPlatformOrder(), resultCallBack);
            collectionOrder.setCallbackStatus(NotifyStatusEnum.FAILED.getCode());
            collectionOrderService.updateById(collectionOrder);
        }
        String res = "请求结果完成";
        System.out.println(res);

    }

}
