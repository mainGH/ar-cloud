package org.ar.wallet.consumer;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.entity.MatchingOrder;
import org.ar.wallet.entity.TaskInfo;
import org.ar.wallet.service.IMatchingOrderService;
import org.ar.wallet.service.ISellService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 人工审核超时自动确认完成订单 (监听死信队列)
 *
 * @author Simon
 * @date 2024/03/25
 */
@Service
@RequiredArgsConstructor
public class AuditTimoutConfirmFinishOrderConsumer {

    @Autowired
    private ISellService sellService;
    @Autowired
    private IMatchingOrderService matchingOrderService;


    private static final Logger logger = LoggerFactory.getLogger(MsgConfirmCallback.class);

    @RabbitListener(queues = RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_QUEUE, concurrency = "5-10")
    public void onTimeoutTask(Message message, Channel channel) {
        String messageBody = new String(message.getBody());
        logger.info("人工审核超时自动确认完成订单, 收到的消息: {}", messageBody);
        TaskInfo task = JSON.parseObject(messageBody, TaskInfo.class);
        String taskInfo = task.getOrderNo();
        try {
            MatchingOrder matchingOrder = matchingOrderService.getMatchingOrder(taskInfo);
            if(matchingOrder==null){
                logger.info("人工审核超时自动确认完成订单, matchingOrder is null and then return direct");
                return;
            }
            // 检查字符串是否包含分隔符"|"
            if (!(OrderStatusEnum.CONFIRMATION.getCode().equals(matchingOrder.getStatus()) || OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode().equals(matchingOrder.getStatus()))) {
                // 没有分隔符 直接消费成功
                logger.info("人工审核超时自动确认完成订单, 订单状态不合法, orderNo: {}, status:{}", taskInfo, matchingOrder.getStatus());
                //处理成功 手动确认消息
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            RestResult restResult = sellService.transactionSuccessHandler(matchingOrder.getPaymentPlatformOrder(), Long.parseLong(matchingOrder.getPaymentMemberId()), null, null, "2", null);
            // 只重试系统级异常, 校验类的跳过
            if (restResult.getCode().equals(ResultCode.SYSTEM_EXECUTION_ERROR.getCode())) {
                logger.error("人工审核超时自动确认完成订单, 消息消费失败: {}", taskInfo);
                //处理失败 抛出异常 进行重试消费
                throw new RuntimeException();
            }
            //处理成功 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            logger.info("人工审核超时自动确认完成订单, 消息消费成功: {}", taskInfo);
        } catch (Exception e) {
            logger.error("人工审核超时自动确认完成订单 Rabbit MQ处理人工审核超时自动确认完成订单失败: e: ", e);
            // 抛出异常, 触发重试机制
            throw new RuntimeException();
        }
    }
}
