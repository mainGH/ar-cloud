package org.ar.wallet.consumer;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.service.OrderChangeEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 同步会员进行中订单缓存 消费者
 *
 * @author Simon
 * @date 2023/1/13
 */
@Service
public class MemberProcessingOrderCacheConsumer {

    @Autowired
    private OrderChangeEventService orderChangeEventService;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.WALLET_MEMBER_PROCESSING_ORDER_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {
        try {
            String messageBody = new String(message.getBody());
            logger.info("同步会员进行中订单缓存消费者, 收到消息: {}", messageBody);
            JSONObject jsonObject = JSONObject.parseObject(messageBody);
            Long memberId = jsonObject.getLong("memberId");
            if (memberId == null) {
                logger.error("同步会员进行中订单缓存消费者, 会员ID为空");
                return;
            }
            orderChangeEventService.syncMemberProcessingOrderCacheByMember(memberId);
            //处理成功 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            logger.error("同步会员进行中订单缓存消费者, 消费失败, e: ", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
