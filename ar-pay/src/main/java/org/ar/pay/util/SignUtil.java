package org.ar.pay.util;


import com.alibaba.cloud.commons.lang.StringUtils;
import org.ar.pay.vo.CollectionOrderVo;
import org.ar.pay.vo.PaymentOrderVo;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;


public class SignUtil {
    public static String sortObject(CollectionOrderVo collectionOrderVo) {
        //log.info("sortData sourceMap:" + sourceMap);
        Map<String, Object> sourceMap = new HashMap<String, Object>();
        sourceMap.put("country", collectionOrderVo.getCountry());
        sourceMap.put("payType", collectionOrderVo.getPayType());
        sourceMap.put("merchantOrder", collectionOrderVo.getMerchantOrder());
        sourceMap.put("goodsName", collectionOrderVo.getGoodsName());
        sourceMap.put("amount", collectionOrderVo.getAmount());
        sourceMap.put("merchantCode", collectionOrderVo.getMerchantCode());
        sourceMap.put("currency", collectionOrderVo.getCurrency());
        sourceMap.put("notifyUrl", collectionOrderVo.getNotifyUrl());
        sourceMap.put("timestamp", collectionOrderVo.getTimestamp());
        sourceMap.put("clientIp", collectionOrderVo.getClientIp());

        String returnStr = sortData(sourceMap, "&");
        //log.info("sortData returnStr:" + returnStr);
        return returnStr;
    }


    public static String sortPayment(PaymentOrderVo paymentOrderVo) {
        //log.info("sortData sourceMap:" + sourceMap);
        Map<String, Object> sourceMap = new HashMap<String, Object>();
        sourceMap.put("country", paymentOrderVo.getCountry());
        sourceMap.put("payType", paymentOrderVo.getPayType());
        sourceMap.put("merchantOrder", paymentOrderVo.getMerchantCode());
        sourceMap.put("goodsName", paymentOrderVo.getGoodsName());
        sourceMap.put("amount", paymentOrderVo.getAmount());
        sourceMap.put("merchantCode", paymentOrderVo.getMerchantOrder());
        sourceMap.put("currency", paymentOrderVo.getCurrency());
        sourceMap.put("notifyUrl", paymentOrderVo.getCurrency());
        sourceMap.put("timestamp", paymentOrderVo.getTimestamp());
        sourceMap.put("clientIp", paymentOrderVo.getClientIp());

        String returnStr = sortData(sourceMap, "&");
        //log.info("sortData returnStr:" + returnStr);
        return returnStr;
    }


    public static String sortData(Map<String, ?> sourceMap) {
        //log.info("sortData sourceMap:" + sourceMap);
        String returnStr = sortData(sourceMap, "&");
        //log.info("sortData returnStr:" + returnStr);
        return returnStr;
    }

    public static String sortData(Map<String, ?> sourceMap, String link) {
        //log.info("start sortData method()");
        if (StringUtils.isEmpty(link)) {
            link = "&";
        }
        Map<String, Object> sortedMap = new TreeMap<String, Object>();
        sortedMap.putAll(sourceMap);
        Set<Entry<String, Object>> entrySet = sortedMap.entrySet();
        StringBuffer sbf = new StringBuffer();
        for (Entry<String, Object> entry : entrySet) {
            if (null != entry.getValue() && StringUtils.isNotEmpty(entry.getValue().toString())) {
                sbf.append(entry.getKey()).append("=").append(entry.getValue()).append(link);
            }
        }
        String returnStr = sbf.toString();
        if (returnStr.endsWith(link)) {
            returnStr = returnStr.substring(0, returnStr.length() - 1);
        }
        return returnStr;
    }

    /**
     * 灏嗚姹傚瓧绗︿覆瑙ｆ瀽涓篗ap
     *
     * @param strParams 璇锋眰瀛楃涓�    key1=value1&key2=value2....&keyN=valueN
     * @return
     */
    public static Map parseParams(String strParams) {
        Map<String, String> map = new HashMap();
        if (!strParams.equals("")) {
            String[] list = strParams.split("&");
            for (int i = 0; i < list.length; i++) {
                String tmp = list[i];
                map.put(tmp.substring(0, tmp.indexOf("=")), tmp.substring(tmp.indexOf("=") + 1));
            }
        }
        return map;
    }
}

