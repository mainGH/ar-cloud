package org.ar.pay.service;


import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.ar.pay.Enum.ContentTypeEnum;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.entity.PaymentOrder;
import org.ar.pay.util.HttpClientUtil;
import org.ar.pay.util.SpringContextUtil;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public abstract class PayRouteAbstract {




    /*
    * 根据三方支付的http_method和content_type选择对应的请求方式
    * 组装参数
    * 发送请求
    * */
    public JSONObject rountePayByParameter(PayConfig payConfig, CollectionOrder collectionOrder){
        RestTemplate restTemplate =   SpringContextUtil.getBean(RestTemplate.class);
        JSONObject jsonObject = null;
        HttpHeaders headers = new org.springframework.http.HttpHeaders();
        Object obj = new Object();
        //根据三方通道的http_method和content_type选择对应的请求方式
        if(payConfig.getContentType().equals(ContentTypeEnum.FORM.getContent())&&payConfig.getHttpMethod().equals("POST")) {
            Map<String, Object> params = getParameterForm(payConfig,collectionOrder);
            HttpResponse result = HttpClientUtil.doPost(payConfig.getPayUrl(), params, "utf-8");
            jsonObject = getResponseConver(result, collectionOrder.getMerchantOrder());
        }else if(payConfig.getContentType().equals(ContentTypeEnum.JSON.getContent())&&payConfig.getHttpMethod().equals("POST")){
            String  params = getParameterJson(payConfig,collectionOrder);
            try {
                HttpResponse result = HttpClientUtil.postJson(payConfig.getPayUrl(), params);
                jsonObject = getResponseConver(result, collectionOrder.getMerchantOrder());
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        return jsonObject;

    }

    /*
    * 组装form表单方式提交的请求参数
    * */
    public abstract Map<String,Object> getParameterForm(PayConfig payConfig,CollectionOrder collectionOrder);


    /*
     * 组装json方式提交的请求参数
     * */
    public abstract String getParameterJson(PayConfig payConfig,CollectionOrder collectionOrder);


    /*
     * 获取三方支付下单接口返回结果
     * */
    public abstract JSONObject getResponseConver(HttpResponse response, String merchantOrder);


    /*
     * 接收三方支付回调
     * */
    public abstract String notify(HttpServletRequest request, HttpServletResponse response, String thirdCode);


}
