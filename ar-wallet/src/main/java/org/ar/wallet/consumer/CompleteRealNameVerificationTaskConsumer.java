package org.ar.wallet.consumer;

import com.rabbitmq.client.Channel;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.service.IMemberInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 完成实名认证任务 消费者
 *
 * @author Simon
 * @date 2023/1/13
 */
@Service
public class CompleteRealNameVerificationTaskConsumer {

    @Autowired
    private IMemberInfoService memberInfoService;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.WALLET_MEMBER_REAL_NAME_VERIFICATION_TASK_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {

        try {
            logger.info("Rabbit 消费队列消息: 完成实名认证任务");

            if (memberInfoService != null) {
                if (memberInfoService.completeRealNameVerificationTask()) {
                    logger.info("Rabbit 消费队列消息: 完成实名认证任务 消费成功");
                    //处理成功 手动确认消息
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                } else {
                    logger.error("Rabbit 消费队列消息: 完成实名认证任务 消费失败, 更新sql失败");
                    // 抛出异常, 触发重试机制
                    throw new RuntimeException();
                }
            } else {
                logger.error("Rabbit 消费队列消息: 完成实名认证任务 消费失败, memberInfoService 为null");
                // 抛出异常, 触发重试机制
                throw new RuntimeException();
            }
        } catch (Exception e) {
            logger.error("Rabbit 消费队列消息: 完成实名认证任务 消费失败, e: {}", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
