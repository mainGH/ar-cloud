package org.ar.pay.service.paymentservice.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.entity.PaymentOrder;
import org.ar.pay.mapper.PayConfigMapper;
import org.ar.pay.property.ArProperty;
import org.ar.pay.service.ICollectionOrderService;

import org.ar.pay.service.paymentservice.PaymentRouteAbstract;
import org.ar.pay.util.AESUtil;
import org.ar.pay.util.DateUtil;
import org.ar.pay.util.SignAPI;
import org.ar.pay.util.SignUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RequiredArgsConstructor
@Service("luckyPayment")
public class LuckyPaymentServiceImpl extends PaymentRouteAbstract {

    private final ICollectionOrderService collectionOrderService;
    private final ArProperty arProperty;

    @Autowired
    private PayConfigMapper payConfigMapper;

    /*
     * Json方式发送支付下单请求
     * */
    public String getParameterJson(PayConfig payConfig, PaymentOrder paymentOrder) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("merchantNo", "2000041");
        params.put("payType", payConfig.getChannel());
        params.put("orderNo", paymentOrder.getPlatformOrder());
        params.put("orderAmount", paymentOrder.getSettlementAmount() == null ? "0" : String.valueOf(paymentOrder.getSettlementAmount()));
        params.put("notifyUrl", arProperty.getCallbackpaymenturl() + payConfig.getServiceName());
        params.put("version", "1.0");
        params.put("accNo", "111");
        params.put("accType", "UPI.");
        params.put("accName", "test");
        params.put("transferCode", "ifsc");

        Map<String, Object> sortedMap = new TreeMap<String, Object>();
        sortedMap.putAll(params);
        String signInfo = JSONObject.toJSONString(sortedMap);
        String sign = null;
        try {
            sign = AESUtil.Encrypt128(signInfo, payConfig.getMd5Key());
        } catch (Exception e) {
            e.printStackTrace();
        }
        params.put("sign", sign);
        params.put("signType", "AES");//不参与签名
        JSONObject jsonObject = new JSONObject(params);
        String content = jsonObject.toJSONString();
        log.info("platformOrder: {}, 请求参数: {}", paymentOrder.getPlatformOrder(), content);
        return content;

    }


    /*
     * form表单方式发送支付下单请求
     * */
    public Map<String, Object> getParameterForm(PayConfig payConfig, PaymentOrder paymentOrder) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("clientCode", "code");
        params.put("chainName", payConfig.getChannel());
        params.put("coinUnit", payConfig.getCurrency());
        params.put("bankCardNum", "922020051144824");
        params.put("callbackurl", arProperty.getCallbackpaymenturl() + payConfig.getServiceName());
        params.put("bankUserName", "MAHESH KHATIK");
//        params.put("ifsc", "UTIB0000241");
//        params.put("bankName", "YESBANK BANK");
//        params.put("amount", String.valueOf(paymentOrder.getSettlementAmount()));
//        params.put("clientNo", paymentOrder.getPlatformOrder());
//        params.put("requestTimestamp", String.valueOf(System.currentTimeMillis()));
//        //String signInfo = SignUtil.sortData(params);
//       // String sign = SignAPI.sign(signInfo, "2525aceede1c4d108474964ddae8794f"); // 签名   signInfo签名参数排序，  merchant_key商户私钥
//       String signInfo = "code"+"&"+payConfig.getChannel()+"&"+paymentOrder.getPlatformOrder()+"&"+String.valueOf(params.get("requestTimestamp"))+"priKey";
//        String sign = SignAPI.calculate(signInfo);
//        params.put("sign", sign);

        System.out.println("请求参数：" + params.toString());
        return params;

    }

    /*
     * 接收三方支付回调
     * */
    public String notify(HttpServletRequest request, HttpServletResponse response, String thirdCode) {

        //根据商户号查询对应的密钥
        QueryWrapper<PayConfig> queryWrapperPayConfig = new QueryWrapper<>();
        queryWrapperPayConfig.select("private_key", "public_key", "md5_key").eq("third_code",thirdCode);
        PayConfig payConfigKey = payConfigMapper.selectOne(queryWrapperPayConfig);


        String merchantNo = (String) request.getParameter("merchantNo");
        String payType = (String) request.getParameter("payType");
        String orderNo = (String) request.getParameter("orderNo");
        String platOrder = (String) request.getParameter("platOrder");
        String orderAmount = (String) request.getParameter("orderAmount");
        String realAmount = (String) request.getParameter("realAmount");
        String orderStatus = (String) request.getParameter("orderStatus");
        String signCallback = (String) request.getParameter("sign");
        Map<String, String> params = new HashMap<String, String>();
        params.put("merchantNo", merchantNo);
        params.put("payType", payType);
        params.put("platOrder", platOrder);
        params.put("orderAmount", orderAmount);
        params.put("realAmount", realAmount);
        params.put("orderStatus", orderStatus);
        params.put("orderNo", orderNo);

        Map<String, Object> sortedMap = new TreeMap<String, Object>();
        sortedMap.putAll(params);
        String signInfo = JSONObject.toJSONString(sortedMap);
        String sign = null;
        try {
            sign = AESUtil.Encrypt128(signInfo, payConfigKey.getMd5Key());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!sign.equals(signCallback)) {
            log.info("platformOrder: {}, 回调验签失败: {},", merchantNo, sign);
            return "false";
        }
        System.out.println("签名参数排序：" + signInfo.length() + " -->" + signInfo);
        System.out.println("签名：" + sign.length() + " -->" + sign);


        boolean status = collectionOrderService.updateOrderByOrderNo(merchantNo, orderNo, realAmount, thirdCode);
        if (status) {
            return "SUCCESS";
        } else {
            log.info("platformOrder: {}, 回调失败", merchantNo);
            return "false";
        }
    }

    /*
     * 获取三方支付下单接口返回结果
     * */
    public JSONObject getResponseConver(HttpResponse response) {
        JSONObject returnJosn = new JSONObject();


        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            System.out.println("HTTP请求未成功！HTTP Status Code:" + response.getStatusLine());
        }
        HttpEntity httpEntity = response.getEntity();
        try {
            String reponseContent = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);//释放资源
            JSONObject jsonObject = JSON.parseObject(reponseContent);
            System.out.println("jsonObject: " + jsonObject);
            String code = jsonObject.getString("code");
            if ("200".equals(code)) {
                JSONObject dataObj = jsonObject.getJSONObject("data");
                String payUrl = dataObj.getString("paymentUrl");
                String orderNo = dataObj.getString("orderNo");
                String orderId = dataObj.getString("orderId");
                //业务状态码
                returnJosn.put("code", 1);
                //支付地址
                returnJosn.put("payUrl", payUrl);
                //商户订单号
                returnJosn.put("orderNo", orderNo);
                //平台订单号
                returnJosn.put("platformOrder", orderId);
                //提示信息
                returnJosn.put("msg", "success");
                return returnJosn;
            } else {
                //业务状态码
                returnJosn.put("code", 9999);
                //三方返回的错误信息
                returnJosn.put("errorMsg", jsonObject.getString("message"));
                return returnJosn;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
        //用Gson将对象转化为实体类
//            Gson gson = new Gson();
//            Class= gson.fromJson(reponseContent, Class.class);


        // System.out.println("响应内容：" + reponseContent);


//        int statusCode = response.getStatusLine().getStatusCode();
//        if (statusCode == 200) {
//            HttpEntity entity = response.getEntity();
//            if (entity != null) {
//                try {
//                    return JSONObject.parseObject(EntityUtils.toString(entity));
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
//            }
//        }
//        return null;
//        // return response;


//        JSONObject jsonObject = new JSONObject();
//        String result = "";
//        if (response != null) {
//            HttpEntity resEntity = response.getEntity();
//            if (resEntity != null) {
//                try {
//                    result = EntityUtils.toString(resEntity, "utf-8");
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//            jsonObject.put("result",result);
//        }
//        return jsonObject;
//
    }



    /*
     * Json方式发送支付下单请求
     * */
    public String getQueryParameterJson(PayConfig payConfig, PaymentOrder paymentOrder) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("merchantNo", "2000041");
        params.put("payType", payConfig.getChannel());
        params.put("orderNo", paymentOrder.getPlatformOrder());
        params.put("orderAmount", paymentOrder.getSettlementAmount() == null ? "0" : String.valueOf(paymentOrder.getSettlementAmount()));
        params.put("notifyUrl", arProperty.getCallbackpaymenturl() + payConfig.getServiceName());
        params.put("version", "1.0");
        params.put("accNo", "111");
        params.put("accType", "UPI.");
        params.put("accName", "test");
        params.put("transferCode", "ifsc");

        Map<String, Object> sortedMap = new TreeMap<String, Object>();
        sortedMap.putAll(params);
        String signInfo = JSONObject.toJSONString(sortedMap);
        String sign = null;
        try {
            sign = AESUtil.Encrypt128(signInfo, payConfig.getMd5Key());
        } catch (Exception e) {
            e.printStackTrace();
        }
        params.put("sign", sign);
        params.put("signType", "AES");//不参与签名
        JSONObject jsonObject = new JSONObject(params);
        String content = jsonObject.toJSONString();
        log.info("platformOrder: {}, 请求参数: {}", paymentOrder.getPlatformOrder(), content);
        return content;

    }


    /*
     * form表单方式发送支付下单请求
     * */
    public Map<String, Object> getQueryParameterForm(PayConfig payConfig, PaymentOrder paymentOrder) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("version", "1.0");
        params.put("goods_name", paymentOrder.getGoodsName());
        params.put("mch_id", "100900001");
        params.put("mch_order_no", paymentOrder.getPlatformOrder());
        params.put("notify_url", "http://91.74.44.172:20008/notify/callback/" + payConfig.getThirdCode());
        params.put("order_date", DateUtil.formatDate(new Date(), DateUtil.yyyy_MM_dd_HH_mm_ss));
        params.put("pay_type", payConfig.getChannel());
        params.put("trade_amount", paymentOrder.getSettlementAmount().toString());
        String signInfo = SignUtil.sortData(params);
        String sign = SignAPI.sign(signInfo, "2525aceede1c4d108474964ddae8794f"); // 签名   signInfo签名参数排序，  merchant_key商户私钥
        params.put("sign", sign);
        params.put("sign_type", "MD5");//不参与签名
        System.out.println("请求参数：" + params.toString());
        return params;

    }



    public JSONObject getQueryResponseConver(HttpResponse response) {
        JSONObject returnJosn = new JSONObject();


        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            System.out.println("HTTP请求未成功！HTTP Status Code:" + response.getStatusLine());
        }
        HttpEntity httpEntity = response.getEntity();
        try {
            String reponseContent = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);//释放资源
            JSONObject jsonObject = JSON.parseObject(reponseContent);
            System.out.println("jsonObject: " + jsonObject);
            String code = jsonObject.getString("code");
            if ("200".equals(code)) {
                JSONObject dataObj = jsonObject.getJSONObject("data");
                String payUrl = dataObj.getString("paymentUrl");
                String orderNo = dataObj.getString("orderNo");
                String orderId = dataObj.getString("orderId");
                //业务状态码
                returnJosn.put("code", 1);
                //支付地址
                returnJosn.put("payUrl", payUrl);
                //商户订单号
                returnJosn.put("orderNo", orderNo);
                //平台订单号
                returnJosn.put("platformOrder", orderId);
                //提示信息
                returnJosn.put("msg", "success");
                return returnJosn;
            } else {
                //业务状态码
                returnJosn.put("code", 9999);
                //三方返回的错误信息
                returnJosn.put("errorMsg", jsonObject.getString("message"));
                return returnJosn;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

}
