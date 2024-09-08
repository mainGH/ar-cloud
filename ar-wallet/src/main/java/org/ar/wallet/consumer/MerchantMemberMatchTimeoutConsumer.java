package org.ar.wallet.consumer;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.entity.TaskInfo;
import org.ar.wallet.service.HandleOrderTimeoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * 商户会员匹配超时消费者 (监听死信队列)
 *
 * @author Simon
 * @date 2023/12/21
 */
@Service
public class MerchantMemberMatchTimeoutConsumer {

    //处理订单超时服务
    private final HandleOrderTimeoutService handleOrderTimeoutService;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    public MerchantMemberMatchTimeoutConsumer(
            HandleOrderTimeoutService handleOrderTimeoutService
    ) {
        this.handleOrderTimeoutService = handleOrderTimeoutService;
    }

    @RabbitListener(queues = RabbitMqConstants.AR_WALLET_MERCHANT_MEMBER_MATCH_DEAD_LETTER_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {
        String messageBody = new String(message.getBody());

        TaskInfo taskInfo = JSON.parseObject(messageBody, TaskInfo.class);

        try {
            //商户会员卖出匹配超时
            logger.info("Rabbit 消费延时队列消息: 商户会员卖出匹配超时: {}", taskInfo.getOrderNo());

            if (handleOrderTimeoutService.handleMerchantMemberSaleMatchTimeout(taskInfo.getOrderNo(), taskInfo.getLastUpdateTimestamp())) {
                //处理成功 手动确认消息
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                logger.info("商户会员卖出匹配超时, 消息消费成功: {}", taskInfo.getOrderNo());
            } else {
                logger.error("商户会员卖出匹配超时, 消息消费失败: {}", taskInfo.getOrderNo());
                //处理失败 抛出异常 进行重试消费
                throw new RuntimeException();
            }
        } catch (Exception e) {
            logger.error("Rabbit MQ处理延时订单超时任务失败: e: {}", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
