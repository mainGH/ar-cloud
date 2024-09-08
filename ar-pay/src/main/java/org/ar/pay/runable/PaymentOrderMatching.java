package org.ar.pay.runable;

import com.alibaba.fastjson.JSON;
import org.ar.pay.entity.BankInfo;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.MerchantInfo;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.handler.HandlerChain;
import org.ar.pay.service.impl.CollectionOrderServiceImpl;
import org.ar.pay.util.HttpClientUtil;
import org.ar.pay.util.SignAPI;
import org.ar.pay.util.SignUtil;
import org.ar.pay.util.SpringContextUtil;
import org.ar.pay.vo.PaymentOrderVo;

import java.util.*;
import java.util.concurrent.CountDownLatch;


public class PaymentOrderMatching implements Runnable {
    private final CountDownLatch countDownLatch;
   // private final CallBack<String> callback;
    private PayConfig payConfig;

    private TreeSet<PayConfig> treeMap;
    private Map<String,List<BankInfo>> mapList;

    private PaymentOrderVo paymentOrderVo;


    public PaymentOrderMatching(CountDownLatch countDownLatch, PaymentOrderVo paymentOrderVo,PayConfig payConfig, TreeSet<PayConfig> treeMap, Map<String, List<BankInfo>> listMap) {
        this.countDownLatch = countDownLatch;
        this.payConfig=payConfig;
        this.treeMap = treeMap;
        this.mapList=listMap;
        this.paymentOrderVo = paymentOrderVo;



    }

    @Override
    public void run() {
        try {
            //定时任务执行 --回调商户
            doWork(paymentOrderVo,payConfig, treeMap,mapList);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            countDownLatch.countDown();
        }
    }

    /**
     * 回调商户
     */
    private void doWork(PaymentOrderVo paymentOrderVo,PayConfig payConfig,TreeSet treeMap,Map<String, List<BankInfo>> listMap) throws Exception {
        Thread.sleep(Math.abs(new Random().nextInt() % 10000));
        HandlerChain handlerChain = new HandlerChain("org.ar.pay.handler.check");
         if(handlerChain.handler(paymentOrderVo,payConfig,listMap)!=null) treeMap.add(payConfig);

    }

}
