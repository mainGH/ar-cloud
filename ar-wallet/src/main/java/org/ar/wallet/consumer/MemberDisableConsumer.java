package org.ar.wallet.consumer;

import com.alibaba.fastjson.JSONObject;
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
 * 会员禁用 消费者
 *
 * @author Simon
 * @date 2023/1/13
 */
@Service
public class MemberDisableConsumer {

    @Autowired
    private IMemberInfoService memberInfoService;

    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.WALLET_MEMBER_DISABLE_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {
        try {
            String messageBody = new String(message.getBody());
            logger.info("会员禁用消费者, 收到消息: {}", messageBody);
            JSONObject jsonObject = JSONObject.parseObject(messageBody);
            memberInfoService.disableMember(jsonObject.getString("memberId"), jsonObject.getString("operator"), jsonObject.getString("remark"));
            logger.info("会员禁用消费者, 消费成功");
            //处理成功 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            logger.error("会员禁用消费者, 消费失败, e: ", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
