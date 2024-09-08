package org.ar.wallet.consumer;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.entity.MemberOperationLogMessage;
import org.ar.wallet.service.impl.ProcessMemberLogServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 异步记录 前台会员操作日志
 *
 * @author Simon
 * @date 2023/1/13
 */
@Service
public class MemberOperationLogConsumer {

    @Autowired
    private ProcessMemberLogServiceImpl processMemberLogServiceImpl;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.WALLET_MEMBER_OPERATION_LOG_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {
        String messageBody = new String(message.getBody());

        MemberOperationLogMessage memberOperationLogMessage = JSON.parseObject(messageBody, MemberOperationLogMessage.class);

        try {
            //记录前台会员登录日志
            logger.info("Rabbit 消费队列消息: 记录前台会员操作日志, memberLoginLogMessage: {}", memberOperationLogMessage);

            if (processMemberLogServiceImpl.processMemberOperationLog(memberOperationLogMessage)) {
                //处理成功 手动确认消息
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                logger.info("记录前台会员操作日志, 消息消费成功: {}", memberOperationLogMessage);
            } else {
                logger.error("记录前台会员操作日志, 消息消费失败: {}", memberOperationLogMessage);
                //处理失败 抛出异常 进行重试消费
                throw new RuntimeException();
            }
        } catch (Exception e) {
            logger.error("Rabbit 消费 记录前台会员操作日志: e: {}", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
