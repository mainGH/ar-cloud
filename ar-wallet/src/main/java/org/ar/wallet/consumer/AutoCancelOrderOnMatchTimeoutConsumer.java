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
 * 匹配超时自动取消订单 (监听死信队列)
 *
 * @author Simon
 * @date 2024/03/25
 */
@Service
@RequiredArgsConstructor
public class AutoCancelOrderOnMatchTimeoutConsumer {

    @Autowired
    private HandleOrderTimeoutService handleOrderTimeoutService;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {
        String messageBody = new String(message.getBody());

        TaskInfo taskInfo = JSON.parseObject(messageBody, TaskInfo.class);


        try {
            //代收订单支付超时处理
            logger.info("Rabbit 消费延时队列消息: 匹配超时自动取消订单: {}", taskInfo.getOrderNo());

            //查看如果会员没有领取前一日奖励的话 系统自动帮他领取
            if (handleOrderTimeoutService.autoCancelOrderOnMatchTimeout(taskInfo.getOrderNo())) {
                //处理成功 手动确认消息
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

                logger.info("匹配超时自动取消订单, 消息消费成功: {}", taskInfo.getOrderNo());
            } else {
                logger.error("匹配超时自动取消订单, 消息消费失败: {}", taskInfo.getOrderNo());
                //处理失败 抛出异常 进行重试消费
                throw new RuntimeException();
            }
        } catch (Exception e) {
            logger.error("匹配超时自动取消订单 Rabbit MQ处理匹配超时自动取消订单失败: e: {}", e.getMessage());
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
