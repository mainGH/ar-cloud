//package org.ar.pay.consumer;
//
//
//import com.alibaba.fastjson.JSON;
//import com.rabbitmq.client.Channel;
//import lombok.extern.slf4j.Slf4j;
//import org.ar.common.core.constant.RabbitMqConstants;
//import org.ar.common.core.result.ResultCode;
//import org.ar.pay.Enum.NotifyStatusEnum;
//import org.ar.pay.entity.CollectionOrder;
//import org.ar.pay.service.impl.CollectionOrderServiceImpl;
//import org.ar.pay.util.RequestUtil;
//import org.ar.pay.util.SignAPI;
//import org.ar.pay.util.SignUtil;
//import org.ar.pay.util.SpringContextUtil;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//
//@Component
//@Slf4j
//public class MerchantOrderConsumer {
//    @RabbitListener(queues = RabbitMqConstants.AR_PAY_QUEUE_NAME, ackMode = "MANUAL")
//    public void process(@Payload CollectionOrder collectionOrder, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag, Message message) {
//
//        log.info("MQ回调商户...  collectionOrder: {}, channel: {},", collectionOrder, channel);
//
//        Map<String, Object> dataMap = new HashMap<>();
//        //商户号
//        dataMap.put("merchantCode", collectionOrder.getMerchantCode());
//
//        //平台订单号
//        dataMap.put("platformOrder", collectionOrder.getPlatformOrder());
//
//        //商户订单号
//        dataMap.put("merchantOrder", collectionOrder.getMerchantOrder());
//
//        //回调地址
//        dataMap.put("notifyUrl", collectionOrder.getNotifyUrl());
//
//        //回调金额 --实际支付的金额 三方回调过来的
//        dataMap.put("amount", collectionOrder.getCollectedAmount());
//
//        //md5签名
//        String signinfo = SignUtil.sortData(dataMap, "&");
//
//        log.info("MQ回调平台订单号: {}, dataMap: {}, MQ回调商户签名串: {}", collectionOrder.getPlatformOrder(), dataMap, signinfo);
//
//        String sign = SignAPI.sign(signinfo, collectionOrder.getKey());
//
//        log.info("MQ回调平台订单号: {}, MQ回调验签Key: {}", collectionOrder.getPlatformOrder(), collectionOrder.getKey());
//
//        dataMap.put("sign", sign);
//
//        //封装整体参数
//        HashMap<String, Object> reqMap = new HashMap<>();
//        reqMap.put("code", ResultCode.SUCCESS.getCode());
//        reqMap.put("data", dataMap);
//        reqMap.put("msg", "OK");
//
//        String reqinfo = JSON.toJSONString(reqMap);
//
//        log.info("MQ回调平台订单号: {}, MQ回调商户请求地址: {}, MQ回调商户请求数据: {}", collectionOrder.getPlatformOrder(), collectionOrder.getNotifyUrl(), reqinfo);
//
//        CollectionOrderServiceImpl collectionOrderService = SpringContextUtil.getBean(CollectionOrderServiceImpl.class);
//        try {
//            String resultCallBack = RequestUtil.HttpRestClientToJson(collectionOrder.getNotifyUrl(), reqinfo);
//            log.info("MQ回调平台订单号: {}, MQ回调商户返回数据: {}", collectionOrder.getPlatformOrder(), resultCallBack);
//            if ("SUCCESS".equals(resultCallBack)) {
//                //更新回调订单状态为2 --自动回调成功
//                log.info("MQ回调平台订单号: {}, MQ回调成功: {}", collectionOrder.getPlatformOrder(), resultCallBack);
//                collectionOrder.setCallbackStatus(NotifyStatusEnum.SUCCESS.getCode());
//
//                //更新回调时间
//                collectionOrder.setCallbackTime(LocalDateTime.now());
//                collectionOrderService.updateById(collectionOrder);
//                //消费成功, 删除队列里面的消息
//                channel.basicAck(deliveryTag, false);
//            } else {
//                //更新订单回调状态为3 --自动回调失败
//                log.info("MQ回调平台订单号: {}, MQ回调失败: {}, 商户未返回SUCCESS", collectionOrder.getPlatformOrder(), resultCallBack);
//                collectionOrder.setCallbackStatus(NotifyStatusEnum.FAILED.getCode());
//                collectionOrderService.updateById(collectionOrder);
//            }
//        } catch (Exception e) {
//            //更新订单回调状态为3 --自动回调失败
//            log.info("MQ回调平台订单号: {}, MQ回调失败", collectionOrder.getPlatformOrder());
//            collectionOrder.setCallbackStatus(NotifyStatusEnum.FAILED.getCode());
//            collectionOrderService.updateById(collectionOrder);
//            throw new RuntimeException(e);
//        }
//    }
//}
