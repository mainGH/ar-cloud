package org.ar.wallet.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.service.IMemberTaskStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 定时任务领取昨日任务奖励
 *
 * @author Simon
 * @date 2024/03/25
 */
@Service
@RequiredArgsConstructor
public class DailyRewardClaimTaskConsumer {

    @Autowired
    private IMemberTaskStatusService memberTaskStatusService;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {

        try {
            logger.info("Rabbit 消费队列消息: 定时任务领取昨日任务奖励");

            if (memberTaskStatusService != null) {
                if (memberTaskStatusService.dailyRewardClaimTask()) {
                    logger.info("Rabbit 消费队列消息: 定时任务领取昨日任务奖励 消费成功");
                    //处理成功 手动确认消息
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                } else {
                    logger.error("Rabbit 消费队列消息: 定时任务领取昨日任务奖励 消费失败, 更新sql失败");
                    // 抛出异常, 触发重试机制
                    throw new RuntimeException();
                }
            } else {
                logger.error("Rabbit 消费队列消息: 定时任务领取昨日任务奖励 消费失败, memberInfoService 为null");
                // 抛出异常, 触发重试机制
                throw new RuntimeException();
            }
        } catch (Exception e) {
            logger.error("Rabbit 消费队列消息: 定时任务领取昨日任务奖励 消费失败, e: {}", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
