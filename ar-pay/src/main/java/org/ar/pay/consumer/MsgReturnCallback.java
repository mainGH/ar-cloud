package org.ar.pay.consumer;

import lombok.extern.slf4j.Slf4j;
import org.ar.pay.service.ICollectionOrderService;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
@Component
@Slf4j
public class MsgReturnCallback implements RabbitTemplate.ReturnsCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private ICollectionOrderService collectionOrderService;
    @PostConstruct
    public void init(){
        rabbitTemplate.setReturnsCallback(this);
    }

    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        System.out.println("消息成功到达队列");
        log.info("{}消息成功到达队列",returnedMessage.getMessage().getMessageProperties().getMessageId());

    }
}
