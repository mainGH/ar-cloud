package org.ar.wallet.consumer;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.entity.TaskInfo;
import org.ar.wallet.service.HandleOrderTimeoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 会员确认超时风控标记订单 (监听死信队列)
 *
 * @author Simon
 * @date 2024/03/25
 */
@Service
@RequiredArgsConstructor
public class MemberConfirmTimeoutRiskTagConsumer {

    @Autowired
    private HandleOrderTimeoutService handleOrderTimeoutService;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {
        String messageBody = new String(message.getBody());
        logger.info("会员确认超时风控标记订单, 收到的消息: {}", messageBody);
        TaskInfo taskInfo = JSON.parseObject(messageBody, TaskInfo.class);
        try {
            if (handleOrderTimeoutService.taggingOrderOnMemberConfirmTimeout(taskInfo.getOrderNo())) {
                //处理成功 手动确认消息
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

                logger.info("会员确认超时风控标记订单, 消息消费成功: {}", taskInfo.getOrderNo());
            } else {
                logger.error("会员确认超时风控标记订单, 消息消费失败: {}", taskInfo.getOrderNo());
                //处理失败 抛出异常 进行重试消费
                throw new RuntimeException();
            }
        } catch (Exception e) {
            logger.error("会员确认超时风控标记订单 Rabbit MQ处理会员确认超时风控标记订单失败: e: ", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
