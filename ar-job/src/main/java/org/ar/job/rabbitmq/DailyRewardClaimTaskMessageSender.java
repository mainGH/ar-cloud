package org.ar.job.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.job.entity.CustomCorrelationData;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class DailyRewardClaimTaskMessageSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送 定时任务领取昨日任务奖励
     *
     * @param dailyRewardClaimTaskMessage
     */
    public void sendDailyRewardClaimTaskMessage(String dailyRewardClaimTaskMessage) {

        String queueName = RabbitMqConstants.WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                queueName
        );

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(dailyRewardClaimTaskMessage.getBytes(), messageProperties);

        log.info("RabbitMQ发送 定时任务领取昨日任务奖励 消息内容: {}, message: {}", message, dailyRewardClaimTaskMessage);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);
    }
}
