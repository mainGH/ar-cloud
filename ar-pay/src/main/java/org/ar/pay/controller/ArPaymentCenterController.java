package org.ar.pay.controller;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.pay.entity.*;
import org.ar.pay.req.PayConfigReq;
import org.ar.pay.runable.CollectionOrderCallback;
import org.ar.pay.runable.PayConfigCompare;
import org.ar.pay.runable.PaymentOrderMatching;
import org.ar.pay.service.*;
import org.ar.pay.service.paymentservice.PaymentRouteAbstract;
import org.ar.pay.util.SignAPI;
import org.ar.pay.util.SignUtil;
import org.ar.pay.util.SpringContextUtil;
import org.ar.pay.vo.CollectionOrderVo;
import org.ar.pay.vo.PaymentOrderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/paymentcenter")
@Api(description = "代付下单控制器")
public class ArPaymentCenterController {
    private final IMerchantInfoService merchantInfoService;
    private final IPayConfigService payConfigService;
    private final IPaymentOrderService paymentOrderService;
    private final ICollectionOrderService collectionOrderService;

    private final IBankInfoService   iBankInfoService;



    @PostMapping("/payment")
    @ApiOperation(value = "代付下单接口")
    public RestResult<JSONObject> pay(@RequestBody @ApiParam PaymentOrderVo paymentOrderVo) throws Exception {

        PayConfigReq req = new PayConfigReq();
        //国家
        req.setCountry(paymentOrderVo.getCountry());

        //支付方式
        req.setPayType(paymentOrderVo.getPayType());

        //币种
        req.setCurrency(paymentOrderVo.getCurrency());

        //商户号
        req.setMerchantCode(paymentOrderVo.getMerchantCode());

        //状态
        req.setStatus("1");


        JSONObject json = null;


        //判断是否存在该商户
        MerchantInfo merchantInfo = merchantInfoService.getMerchantInfoByCode(paymentOrderVo.getMerchantCode());
        if (merchantInfo == null || StringUtils.isEmpty(merchantInfo.getCode())) {
            JSONObject errorJson = new JSONObject();
            //业务状态码
            errorJson.put("code", 9999);
            //错误信息
            errorJson.put("errorMsg", "商户号不存在");
            return RestResult.ok(errorJson);
        }

        //md5验签
        String signInfo = SignUtil.sortPayment(paymentOrderVo);
        String sign = SignAPI.sign(signInfo, merchantInfo.getMd5Key());

//        if (!sign.equals(paymentOrderVo.getSign())) {
//            JSONObject errorJson = new JSONObject();
//            //业务状态码
//            errorJson.put("code", 9999);
//            //错误信息
//            errorJson.put("errorMsg", "签名错误");
//            return RestResult.ok(errorJson);
//        }

        //设置平台订单号
        paymentOrderVo.setPlatformOrder("AR" + paymentOrderVo.getMerchantOrder());

        //根据条件过滤查询到符合的三方支付
        List<PayConfig> list = payConfigService.getPaymentConfigByCondtion(req);

        ExecutorService threadPool = Executors.newFixedThreadPool(list.size());


        CountDownLatch downLatch = new CountDownLatch(list.size());

        TreeSet<PayConfig> treeMap = new TreeSet<PayConfig>(new PayConfigCompare());



        List<BankInfo> listbank = iBankInfoService.list();
        Map<String,List<BankInfo>>  map = listbank.stream().collect(Collectors.groupingBy(item->item.getThirdCode()+"_"+item.getCounty()));

          for(PayConfig payConfig : list){
              PaymentOrderMatching paymentOrderMatching     =  new PaymentOrderMatching(downLatch,paymentOrderVo,payConfig,treeMap,map);
              threadPool.submit(paymentOrderMatching);
            }
            downLatch.await();



            if (list.size() > 0) {
            //选择一个三方支付
            //PayConfig payConfig = (PayConfig) list.get(0);
                 PayConfig payConfig  = treeMap.first();
            paymentOrderVo.setThirdCode(payConfig.getThirdCode());
                PaymentOrder paymentOrder = new PaymentOrder();
            BeanUtils.copyProperties(paymentOrderVo, paymentOrder);


            paymentOrderService.save(paymentOrder);

            //获取到三方支付的实现类
            PaymentRouteAbstract route = (PaymentRouteAbstract) SpringContextUtil.getBean(payConfig.getServiceName());

            //三方支付实现类调用三方下单接口
            json = route.rountePayByParameter(payConfig, paymentOrder);
        }
        //将支付下单接口数据返回给商户
        return RestResult.ok(json);
    }

}


