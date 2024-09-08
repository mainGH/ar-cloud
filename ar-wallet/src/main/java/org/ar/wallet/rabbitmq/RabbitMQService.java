package org.ar.wallet.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.entity.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.UUID;

@Service
@Slf4j
public class RabbitMQService {

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 使用Rabbit MQ死信队列机制 处理延时任务
     * 1.先把消息发送到主队列 并设置消息过期时间
     * 2.消息过期后 会被路由到死信队列 处理延时任务的消费者监听死信队列 进行处理延时任务
     *
     * @param taskInfo
     * @param delayMillis
     */
    public void sendTimeoutTask(TaskInfo taskInfo, long delayMillis) {

        String type = "";
        String queueName = "";

        switch (taskInfo.getTaskType()) {
            case "1":
                type = "钱包用户确认超时";
                queueName = RabbitMqConstants.AR_WALLET_MEMBER_CONFIRM_TIMEOUT_QUEUE;
                break;
            case "2":
                type = "商户会员确认超时";
                queueName = RabbitMqConstants.AR_WALLET_MERCHANT_MEMBER_CONFIRM_TIMEOUT_QUEUE;
                break;
            case "3":
                type = "钱包用户卖出匹配超时";
                queueName = RabbitMqConstants.AR_WALLET_MEMBER_MATCH_TIMEOUT_QUEUE;
                break;
            case "4":
                type = "商户会员卖出匹配超时";
                queueName = RabbitMqConstants.AR_WALLET_MERCHANT_MEMBER_MATCH_TIMEOUT_QUEUE;
                break;
            case "5":
                type = "支付超时";
                queueName = RabbitMqConstants.AR_WALLET_MEMBER_PAYMENT_TIMEOUT_QUEUE;
                break;
            case "6":
                type = "USDT支付超时";
                queueName = RabbitMqConstants.AR_WALLET_MEMBER_PAYMENT_TIMEOUT_QUEUE;
                break;
            case "9":
                type = "语音通知卖方";
                queueName = RabbitMqConstants.WALLET_MEMBER_NOTIFY_SELLER_BY_VOICE_QUEUE;
                break;
            case "10":
                type = "代收订单支付超时";
                queueName = RabbitMqConstants.WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_QUEUE;
                break;
            case "11":
                type = "次日自动领取每日任务奖励";
                queueName = RabbitMqConstants.WALLET_MERCHANT_AUTO_CLAIM_REWARD_QUEUE;
                break;
            case "12":
                type = "匹配超时自动取消订单";
                queueName = RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_QUEUE;
                break;
            case "13":
                type = "会员确认超时风控标记订单";
                queueName = RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_QUEUE;
                break;
            case "14":
                type = "提现交易延时通知";
                queueName = RabbitMqConstants.WITHDRAW_NOTIFY_TIMEOUT_QUEUE;
                break;
            case "15":
                type = "会员确认超时自动取消订单";
                queueName = RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_QUEUE;
                break;
            case "16":
                type = "人工审核超时自动确认完成订单";
                queueName = RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_QUEUE;
                break;
        }

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(UUID.randomUUID().toString(), taskInfo.getOrderNo(), taskInfo.getTaskType(), queueName);

        // 使用 Fastjson 将对象转换为 JSON 字符串
        String messageJson = JSON.toJSONString(taskInfo);

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        //设置消息的 TTL过期时间
        messageProperties.setExpiration(String.valueOf(delayMillis));

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 " + type + "消息: 延迟时间: {}, 消息内容: {}, taskInfo: {}", delayMillis / 1000 + "秒", message, taskInfo);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);
    }


    /**
     * 发送 充值成功 回调的MQ
     */
    public void sendRechargeSuccessCallbackNotification(TaskInfo taskInfo) {

        String queueName = RabbitMqConstants.AR_WALLET_TRADE_COLLECT_QUEUE_NAME;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                taskInfo.getOrderNo(),
                taskInfo.getTaskType(),
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        String messageJson = JSON.toJSONString(taskInfo);

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 充值成功 回调MQ消息成功 消息内容: {}, taskInfo: {}", message, taskInfo);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);
    }


    /**
     * 发送 提现成功 回调的MQ
     */
    public void sendWithdrawalSuccessCallbackNotification(TaskInfo taskInfo) {

        String queueName = RabbitMqConstants.AR_WALLET_TRADE_PAYMENT_QUEUE_NAME;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                taskInfo.getOrderNo(),
                taskInfo.getTaskType(),
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        String messageJson = JSON.toJSONString(taskInfo);

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 提现成功 回调MQ消息成功 消息内容: {}, taskInfo: {}", message, taskInfo);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);
    }


    /**
     * 发送 记录操作日志的MQ
     *
     * @param memberOperationLogMessage
     */
    public void sendMemberOperationLogMessage(MemberOperationLogMessage memberOperationLogMessage) {


        String queueName = RabbitMqConstants.WALLET_MEMBER_OPERATION_LOG_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                "1",
                "1",
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        String messageJson = JSON.toJSONString(memberOperationLogMessage);

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 记录前台会员操作日志的MQ 消息内容: {}, loginLogMessage: {}", message, memberOperationLogMessage);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);

    }

    /**
     * 发送 禁用会员的MQ
     *
     * @param memberId
     */
    public void sendMemberDisableMessage(String memberId, String remark) {

        String queueName = RabbitMqConstants.WALLET_MEMBER_DISABLE_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                "1",
                "1",
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("memberId", memberId);
        jsonObject.put("remark", remark);
        String messageJson = jsonObject.toJSONString();

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 禁用会员的MQ 消息内容: {}", messageJson);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);

    }

    /**
     * 发送 同步会员首页列表的MQ
     *
     * @param memberId
     */
    public void sendMemberSyncMessage(Long memberId) {

        String queueName = RabbitMqConstants.WALLET_MEMBER_PROCESSING_ORDER_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                "1",
                "1",
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("memberId", memberId);
        String messageJson = jsonObject.toJSONString();

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 首页同步进行中的订单的MQ 消息内容: {}", messageJson);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);

    }

    public void sendOrderTaggingMessage(OrderTaggingMessage orderTaggingMessage) {

        if(CollectionUtils.isEmpty(orderTaggingMessage.getPlatformOrderTags())){
            log.info("RabbitMQ发送 待标记订单列表为空, 不需要发送, orderTaggingMessage: {}", orderTaggingMessage);
            return;
        }

        String queueName = RabbitMqConstants.WALLET_ORDER_TAGGING_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                "1",
                "1",
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        String messageJson = JSON.toJSONString(orderTaggingMessage, SerializerFeature.WriteMapNullValue);

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 订单标记的MQ 消息内容: {}", messageJson);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);

    }

    public void sendTradeIpBlackAddMessage(TradIpBlackMessage tradIpBlackMessage) {

        String queueName = RabbitMqConstants.WALLET_TRADE_IP_BLACK_ADD_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                "1",
                "1",
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        String messageJson = JSON.toJSONString(tradIpBlackMessage);

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 添加交易IP黑名单的MQ 消息内容: {}", messageJson);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);

    }



    /**
     * 发送 会员升级的MQ
     *
     * @param memberId
     */
    public void sendMemberUpgradeMessage(String memberId) {

        String queueName = RabbitMqConstants.WALLET_MEMBER_UPGRADE_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                "1",
                "1",
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("memberId", memberId);
        String messageJson = jsonObject.toJSONString();

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);
        log.info("RabbitMQ发送 会员升级的MQ 消息内容: {}", messageJson);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);

    }


    /**
     * 发送获取KYC银行交易记录的MQ
     *
     * @param kycTransactionMessage
     */
    public void sendKycTransactionMessage(KycTransactionMessage kycTransactionMessage) {


        String queueName = RabbitMqConstants.WALLET_MEMBER_KYC_TRANSACTION_QUEUE;

        // 创建自定义CorrelationData
        CustomCorrelationData correlationData = new CustomCorrelationData(
                UUID.randomUUID().toString(),
                "1",
                "1",
                queueName
        );

        // 使用 Fastjson 将对象转换为 JSON 字符串
        String messageJson = JSON.toJSONString(kycTransactionMessage);

        // 创建消息属性
        MessageProperties messageProperties = new MessageProperties();
        //设置消息为持久化
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        // 创建消息
        Message message = new Message(messageJson.getBytes(), messageProperties);

        log.info("RabbitMQ发送 获取KYC银行交易记录的MQ 消息内容: {}, kycTransactionMessage: {}", message, kycTransactionMessage);

        // 发送消息到默认交换机，并使用队列名称作为路由键
        rabbitTemplate.convertAndSend("", queueName, message, correlationData);

    }
}
