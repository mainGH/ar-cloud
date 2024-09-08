package org.ar.pay.config;

import org.ar.pay.service.ICollectionOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {
    public final static Logger logger = LoggerFactory.getLogger(RabbitConfig.class);
    @Autowired
     private  CachingConnectionFactory cachingConnectionFactory;
    @Autowired
    private   ICollectionOrderService collectionOrderService;


//    @Bean
//    RabbitTemplate rabbitTemplate() {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
//        rabbitTemplate.setConfirmCallback((data, ack, cause) -> {
//            String msgId = data.getId();
//            if (ack) {
//                logger.info(msgId + ":消息发送成功");
//
//            } else {
//                logger.info(msgId + ":消息发送失败");
//            }
//        });
//        rabbitTemplate.setReturnCallback((msg, repCode, repText, exchange, routingkey) -> {
//            logger.info("消息发送失败");
//        });
//        return rabbitTemplate;
//    }

//    @Bean
//    Queue notifyQueue() {
//        return new Queue(RabbitMqConstants.AR_PAY_QUEUE_NAME, true);
//    }
//
//    @Bean
//    DirectExchange notifyExchange() {
//        return new DirectExchange(RabbitMqConstants.AR_EXCHANGE_NAME, true, false);
//    }
//
//    @Bean
//    Binding mailBinding() {
//        return BindingBuilder.bind(notifyQueue()).to(notifyExchange()).with(RabbitMqConstants.AR_ROUTING_KEY_NAME);
//    }

}
