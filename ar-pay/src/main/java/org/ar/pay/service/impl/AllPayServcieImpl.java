package org.ar.pay.service.impl;


import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;

import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.PayConfig;

import org.ar.pay.service.ICollectionOrderService;
import org.ar.pay.service.PayRouteAbstract;
import org.ar.pay.util.DateUtil;
import org.ar.pay.util.SignAPI;
import org.ar.pay.util.SignUtil;
import org.springframework.stereotype.Service;

import java.beans.Transient;
import java.util.*;

import org.apache.http.HttpEntity;

import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
@Slf4j
@RequiredArgsConstructor
@Service("allPay")
public class AllPayServcieImpl extends PayRouteAbstract {

    private final ICollectionOrderService collectionOrderService;
    public String getParameterJson(PayConfig payConfig, CollectionOrder collectionOrder) {
        Map<String,Object> params = new TreeMap<String, Object>();
        params.put("version","1.0");
        params.put("goods_name", collectionOrder.getGoodsName());
        params.put("mch_id", "100900001");
        params.put("mch_order_no", collectionOrder.getPlatformOrder());
        params.put("notify_url", "http://91.74.44.172:20008/notify/callback/"+payConfig.getThirdCode());
        params.put("order_date", DateUtil.yyyy_MM_dd_HH_mm_ss);
        params.put("pay_type", payConfig.getChannel());
        params.put("trade_amount", collectionOrder.getAmount());
        String signInfo = SignUtil.sortData(params);
        String sign = SignAPI.sign(signInfo, "2525aceede1c4d108474964ddae8794f"); // 签名   signInfo签名参数排序，  merchant_key商户私钥
        params.put("sign", sign);
        params.put("sign_type", "MD5");//不参与签名
        JSONObject jsonObject = new JSONObject(params);
        String content = jsonObject.toJSONString();
        return content;

    }


    public Map<String, Object> getParameterForm(PayConfig payConfig, CollectionOrder collectionOrder) {
        Map<String,Object> params = new TreeMap<String, Object>();
        params.put("version","1.0");
        params.put("goods_name", collectionOrder.getGoodsName());
        params.put("mch_id", "100900001");
        params.put("mch_order_no", collectionOrder.getPlatformOrder());
        params.put("notify_url", "http://91.74.44.172:20008/notify/callback/"+payConfig.getThirdCode());
        params.put("order_date", DateUtil.formatDate(new Date(),DateUtil.yyyy_MM_dd_HH_mm_ss));
        params.put("pay_type", payConfig.getChannel());
        params.put("trade_amount", collectionOrder.getAmount().toString());
        String signInfo = SignUtil.sortData(params);
        String sign = SignAPI.sign(signInfo, "2525aceede1c4d108474964ddae8794f"); // 签名   signInfo签名参数排序，  merchant_key商户私钥
        params.put("sign", sign);
        params.put("sign_type", "MD5");//不参与签名
        System.out.println("请求参数：" + params.toString());
        return params;

    }
    @Transient
    public  String notify(HttpServletRequest request, HttpServletResponse response, String thirdCode){
        String tradeResult = (String) request.getParameter("tradeResult");
        String mchId = (String) request.getParameter("mchId");
        String merchant_key = "";//支付秘钥，商户后台可以查看
        String mchOrderNo  = (String) request.getParameter("mchOrderNo");
        String oriAmount = (String) request.getParameter("oriAmount");
        String amount = (String) request.getParameter("amount");
        String orderDate= (String) request.getParameter("orderDate");
        String orderNo = (String) request.getParameter("orderNo");
        String sign = (String) request.getParameter("sign");
        String signType = (String) request.getParameter("signType");
        Map<String,String> params = new LinkedHashMap<String,String>();
        params.put("tradeResult", tradeResult);
        params.put("mchId", mchId);
        params.put("mchOrderNo", mchOrderNo);
        params.put("oriAmount", oriAmount);
        params.put("amount", amount);
        params.put("orderDate", orderDate);
        params.put("orderNo", orderNo);
        params.put("amount", amount);
        String signStr = SignUtil.sortData(params);
        String signInfo =signStr.toString();
        System.out.println("签名参数排序：" + signInfo.length() + " -->" + signInfo);
        System.out.println("签名：" + sign.length() + " -->" + sign);
        boolean result = false;
        if("MD5".equals(signType)){
            result = SignAPI.validateSignByKey(signInfo, merchant_key, sign);

        }
       boolean status = collectionOrderService.updateOrderByOrderNo(mchOrderNo,orderNo,amount,"allpay");
        if(result&&status){
            log.info("验签结果result的值：" + result + " -->success");
            return "success";
        }else{
            log.error("验签结果result的值：" + result + " -->Signature Error");
            return "Signature Error";
        }
    }

    public JSONObject getResponseConver(HttpResponse response, String merchantOrder) {
        JSONObject jsonObject = new JSONObject();
        String result = "";
        if (response != null) {
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                try {
                    result = EntityUtils.toString(resEntity, "utf-8");
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            jsonObject.put("result",result);
        }
        return jsonObject;

    }
}
