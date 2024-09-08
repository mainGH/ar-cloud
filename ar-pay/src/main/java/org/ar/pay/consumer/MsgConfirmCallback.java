package org.ar.pay.consumer;

import lombok.extern.slf4j.Slf4j;
import org.ar.pay.service.ICollectionOrderService;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
@Slf4j
@Component
public class MsgConfirmCallback implements RabbitTemplate.ConfirmCallback {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ICollectionOrderService collectionOrderService;
    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback(this);
    }

    public void confirm(CorrelationData correlationData,boolean ack,String message){

        String msgId = correlationData.getId();
        if (ack) {
            log.info(msgId + ":消息发送成功");
            //将订单改为已发送状态
            collectionOrderService.updateOrderSendById(msgId);
        } else {
            //这里做MQ重发消息机制
            log.info(msgId + ":消息发送失败");
        }
    }
}
