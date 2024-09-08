package org.ar.pay.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.ar.common.core.result.ResultCode;
import org.ar.pay.entity.CollectionOrder;
import org.ar.pay.entity.PayConfig;
import org.ar.pay.property.ArProperty;
import org.ar.pay.service.ICollectionOrderService;
import org.ar.pay.service.IPayConfigService;
import org.ar.pay.service.PayRouteAbstract;
import org.ar.pay.util.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RequiredArgsConstructor
@Service("tbPay")
public class TbPayServiceImpl extends PayRouteAbstract {

    private final ICollectionOrderService collectionOrderService;
    private final ArProperty arProperty;
    private final IPayConfigService payConfigService;
    private final RabbitTemplate rabbitTemplate;

    /*
     * Json方式发送支付下单请求
     * */
    public String getParameterJson(PayConfig payConfig, CollectionOrder collectionOrder) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("merchantNo", "2000041");
        params.put("payType", payConfig.getChannel());
        params.put("orderNo", collectionOrder.getPlatformOrder());
        params.put("orderAmount", collectionOrder.getAmount() == null ? "0" : String.valueOf(collectionOrder.getAmount()));
        params.put("notifyUrl", arProperty.getNotifyurl() + payConfig.getThirdCode());
        params.put("version", "1.0");
        params.put("memberId", collectionOrder.getMerchantOrder());
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
        log.info("tbPay下单接口 平台订单号: {}, 请求参数: {}", collectionOrder.getPlatformOrder(), content);
        return content;

    }


    /*
     * form表单方式发送支付下单请求
     * */
    public Map<String, Object> getParameterForm(PayConfig payConfig, CollectionOrder collectionOrder) {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("version", "1.0");
        params.put("goods_name", collectionOrder.getGoodsName());
        params.put("mch_id", "100900001");
        params.put("mch_order_no", collectionOrder.getPlatformOrder());
        params.put("notify_url", "http://91.74.44.172:20008/notify/callback/" + payConfig.getThirdCode());
        params.put("order_date", DateUtil.formatDate(new Date(), DateUtil.yyyy_MM_dd_HH_mm_ss));
        params.put("pay_type", payConfig.getChannel());
        params.put("trade_amount", collectionOrder.getAmount().toString());
        String signInfo = SignUtil.sortData(params);
        String sign = SignAPI.sign(signInfo, "2525aceede1c4d108474964ddae8794f"); // 签名   signInfo签名参数排序，  merchant_key商户私钥
        params.put("sign", sign);
        params.put("sign_type", "MD5");//不参与签名
        log.info("请求单号:{}, 请求参数:{}", params.get("mch_order_no"), params);
        System.out.println("请求参数：" + params.toString());
        return params;

    }

    /*
     * 接收三方支付回调
     * */
    public String notify(HttpServletRequest request, HttpServletResponse response, String thirdCode) {

        //获取json流数据
        JSONObject jsonParameters = RequestUtil.getJsonParameters(request);

        log.info("接收tbPay支付回调...  jsonParameters: {}", jsonParameters);

        String merchantNo;
        String payType;
        String orderNo;
        String platOrder;
        String orderAmount;
        String realAmount;
        String orderStatus;
        String signCallback;
        //json流没接到 用key=value形式接收
        if (jsonParameters == null) {
            merchantNo = request.getParameter("merchantNo");
            payType = request.getParameter("payType");
            orderNo = request.getParameter("orderNo");
            platOrder = request.getParameter("platOrder");
            orderAmount = request.getParameter("orderAmount");
            realAmount = request.getParameter("realAmount");
            orderStatus = request.getParameter("orderStatus");
            signCallback = request.getParameter("sign");
        } else {
            merchantNo = jsonParameters.getString("merchantNo");
            payType = jsonParameters.getString("payType");
            orderNo = jsonParameters.getString("orderNo");
            platOrder = jsonParameters.getString("platOrder");
            orderAmount = jsonParameters.getString("orderAmount");
            realAmount = jsonParameters.getString("realAmount");
            orderStatus = jsonParameters.getString("orderStatus");
            signCallback = jsonParameters.getString("sign");
        }

        log.info("接收tbPay支付回调商户订单号: {}, 平台订单号: {}", orderNo, platOrder);

        //判断该笔订单提单金额和实际支付金额是否一致
        //根据平台订单号查询到该笔订单
        QueryWrapper<CollectionOrder> collectionOrderQueryWrapper = new QueryWrapper<>();
        collectionOrderQueryWrapper.eq("platform_order", orderNo);
        CollectionOrder collectionOrder = collectionOrderService.getOne(collectionOrderQueryWrapper);

        if (new BigDecimal(String.valueOf(collectionOrder.getAmount())).compareTo(new BigDecimal(realAmount)) != 0) {
            log.info("商户号: {}, 平台订单号: {}, 该笔订单提单金额和支付金额不一致!", orderNo);
            return "false";
        }

        //根据商户号查询对应的密钥
        QueryWrapper<PayConfig> queryWrapperPayConfig = new QueryWrapper<>();
        queryWrapperPayConfig.select("private_key", "public_key", "md5_key").eq("third_code", thirdCode);
        PayConfig payConfigKey = payConfigService.getOne(queryWrapperPayConfig);

        Map<String, String> params = new HashMap<String, String>();
        params.put("merchantNo", merchantNo);
        params.put("payType", payType);
        params.put("platOrder", platOrder);
        params.put("orderAmount", orderAmount);
        params.put("realAmount", realAmount);
        params.put("orderStatus", orderStatus);
        params.put("orderNo", orderNo);

        log.info("接收tbPay支付回调商户订单号: {}, 平台订单号: {}, params: {}", orderNo, platOrder, params);

        Map<String, Object> sortedMap = new TreeMap<String, Object>();
        sortedMap.putAll(params);

        String signInfo = JSONObject.toJSONString(sortedMap);

        log.info("接收tbPay支付回调商户订单号: {}, 平台订单号: {}, sortedMap: {}, signInfo: {}", orderNo, platOrder, sortedMap, signInfo);

        String sign = null;
        try {
            sign = AESUtil.Encrypt128(signInfo, payConfigKey.getMd5Key());
            log.info("sign:{},", sign);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!sign.equals(signCallback)) {

            log.info("接收tbPay支付回调商户订单号: {}, 平台订单号: {}, 验签失败签名串: {}, 验签失败sign: {}", orderNo, platOrder, signInfo, sign);

            return "false";
        }

        //更新订单状态和账变
        boolean status = collectionOrderService.updateOrderByOrderNo(merchantNo, orderNo, realAmount, payType);

        if (status) {
            //发送MQ消息 异步回调商户
            //添加订单实际支付金额
            collectionOrder.setCollectedAmount(new BigDecimal(realAmount));
            //添加该笔订单回调验签key
            collectionOrder.setKey(payConfigKey.getMd5Key());
//            rabbitTemplate.convertAndSend(RabbitMqConstants.AR_PAY_QUEUE_NAME, collectionOrder, new CorrelationData(String.valueOf(collectionOrder.getId())));
            log.info("接收tbPay支付回调商户订单号: {}, 回调成功!", orderNo);
            return "SUCCESS";
        } else {
            log.info("接收tbPay支付回调商户订单号: {}, 平台订单号: {}, 更新账变失败", orderNo, platOrder);
            return "false";
        }
    }

    /*
     * 获取三方支付下单接口返回结果
     * */
    public JSONObject getResponseConver(HttpResponse response, String merchantOrder) {
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

            log.info("tbPay支付下单接口返回数据: {}", jsonObject);

            String code = jsonObject.getString("code");
            if ("200".equals(code)) {
                JSONObject dataObj = jsonObject.getJSONObject("data");
                String payUrl = dataObj.getString("paymentUrl");
                //业务状态码
                returnJosn.put("code", ResultCode.SUCCESS.getCode());
                //支付地址
                returnJosn.put("payUrl", payUrl);
                //商户订单号
                returnJosn.put("orderNo", merchantOrder);
                //平台订单号
                returnJosn.put("platformOrder", dataObj.getString("orderNo"));
                //提示信息
                returnJosn.put("msg", "success");
                //三方订单号
                returnJosn.put("thirdOrder", dataObj.getString("orderId"));
                return returnJosn;
            } else {
                //业务状态码
                returnJosn.put("code", ResultCode.SYSTEM_EXECUTION_ERROR.getCode());
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
