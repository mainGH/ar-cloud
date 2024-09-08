package org.ar.wallet.config;

import org.ar.common.core.constant.RabbitMqConstants;
import org.ar.wallet.consumer.MsgConfirmCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitConfig {
    public final static Logger logger = LoggerFactory.getLogger(RabbitConfig.class);
    @Autowired
    private CachingConnectionFactory cachingConnectionFactory;


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        //设置消息确认回调
        //每当消息被 RabbitMQ 代理确认（无论成功还是失败），都会调用 MsgConfirmCallback 的 confirm 方法
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setConfirmCallback(new MsgConfirmCallback());
        return rabbitTemplate;
    }


    //充值交易回调通知
    @Bean
    Queue collectNotifyQueue() {
        return new Queue(RabbitMqConstants.AR_WALLET_TRADE_COLLECT_QUEUE_NAME, true);
    }

    @Bean
    DirectExchange collectNotifyExchange() {
        return new DirectExchange(RabbitMqConstants.AR_WALLET_TRADE_COLLECT_EXCHANGE, true, false);
    }

    @Bean
    Binding collectMailBinding() {
        return BindingBuilder.bind(collectNotifyQueue()).to(collectNotifyExchange()).with(RabbitMqConstants.AR_WALLET_TRADE_COLLECT_ROUTING_KEY);
    }

/*    //提现交易回调通知
    @Bean
    Queue paymentNotifyQueue() {
        return new Queue(RabbitMqConstants.AR_WALLET_TRADE_PAYMENT_QUEUE_NAME, true);
    }

    @Bean
    DirectExchange paymentNotifyExchange() {
        return new DirectExchange(RabbitMqConstants.AR_WALLET_TRADE_PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    Binding paymentMailBinding() {
        return BindingBuilder.bind(paymentNotifyQueue()).to(paymentNotifyExchange()).with(RabbitMqConstants.AR_WALLET_TRADE_PAYMENT_ROUTING_KEY);
    }*/


    //订单超时处理-----------------------------------

    // 死信交换机
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.AR_WALLET_DEAD_LETTER_EXCHANGE);
    }


    // 钱包会员匹配超时死信队列
    @Bean
    public Queue walletMemberMatchDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MEMBER_MATCH_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bindingWalletMemberMatchDeadLetterQueue() {
        return BindingBuilder.bind(walletMemberMatchDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMqConstants.WALLET_MEMBER_MATCH_PROCESS);
    }

    // 商户会员匹配超时死信队列
    @Bean
    public Queue merchantMemberMatchDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MERCHANT_MEMBER_MATCH_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bindingMerchantMemberMatchDeadLetterQueue() {
        return BindingBuilder.bind(merchantMemberMatchDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMqConstants.MERCHANT_MEMBER_MATCH_PROCESS);
    }

    // 钱包会员确认超时死信队列
    @Bean
    public Queue walletMemberConfirmDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MEMBER_CONFIRM_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bindingWalletMemberConfirmDeadLetterQueue() {
        return BindingBuilder.bind(walletMemberConfirmDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMqConstants.WALLET_MEMBER_CONFIRM_PROCESS);
    }

    // 商户会员确认超时死信队列
    @Bean
    public Queue merchantMemberConfirmDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MERCHANT_MEMBER_CONFIRM_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bindingMerchantMemberConfirmDeadLetterQueue() {
        return BindingBuilder.bind(merchantMemberConfirmDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMqConstants.MERCHANT_MEMBER_CONFIRM_PROCESS);
    }

    // 钱包会员支付超时死信队列
    @Bean
    public Queue walletMemberPaymentDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MEMBER_PAYMENT_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding bindingWalletMemberPaymentDeadLetterQueue() {
        return BindingBuilder.bind(walletMemberPaymentDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMqConstants.WALLET_MEMBER_PAYMENT_PROCESS);
    }

    // 创建六个主队列，每个队列对应一个延时任务类型

    // 1. 钱包会员匹配超时主队列
    @Bean
    public Queue walletMemberMatchTimeoutQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MEMBER_MATCH_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.AR_WALLET_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MEMBER_MATCH_PROCESS)
                .build();
    }

    // 2. 商户会员匹配超时主队列
    @Bean
    public Queue merchantMemberMatchTimeoutQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MERCHANT_MEMBER_MATCH_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.AR_WALLET_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.MERCHANT_MEMBER_MATCH_PROCESS)
                .build();
    }

    // 3. 钱包会员确认超时主队列
    @Bean
    public Queue walletMemberConfirmTimeoutQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MEMBER_CONFIRM_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.AR_WALLET_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MEMBER_CONFIRM_PROCESS)
                .build();
    }

    // 4. 商户会员确认超时主队列
    @Bean
    public Queue merchantMemberConfirmTimeoutQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MERCHANT_MEMBER_CONFIRM_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.AR_WALLET_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.MERCHANT_MEMBER_CONFIRM_PROCESS)
                .build();
    }

    // 5. 钱包会员支付超时主队列
    @Bean
    public Queue walletMemberPaymentTimeoutQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AR_WALLET_MEMBER_PAYMENT_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.AR_WALLET_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MEMBER_PAYMENT_PROCESS)
                .build();
    }


    // 绑定主队列到死信交换机的辅助方法
    private Binding bindingQueueToExchange(Queue queue, String routingKey) {
        return BindingBuilder.bind(queue).to(deadLetterExchange()).with(routingKey);
    }

    //将钱包会员匹配超时队列绑定到死信交换机
    @Bean
    public Binding bindingWalletMemberMatchTimeoutQueue() {
        return bindingQueueToExchange(walletMemberMatchTimeoutQueue(), RabbitMqConstants.WALLET_MEMBER_MATCH_TIMEOUT);
    }

    //将商户会员匹配超时队列绑定到死信交换机
    @Bean
    public Binding bindingMerchantMemberMatchTimeoutQueue() {
        return bindingQueueToExchange(merchantMemberMatchTimeoutQueue(), RabbitMqConstants.MERCHANT_MEMBER_MATCH_TIMEOUT);
    }

    //将钱包会员确认超时队列绑定到死信交换机
    @Bean
    public Binding bindingWalletMemberConfirmTimeoutQueue() {
        return bindingQueueToExchange(walletMemberConfirmTimeoutQueue(), RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT);
    }

    //将商户会员确认超时队列绑定到死信交换机
    @Bean
    public Binding bindingMerchantMemberConfirmTimeoutQueue() {
        return bindingQueueToExchange(merchantMemberConfirmTimeoutQueue(), RabbitMqConstants.MERCHANT_MEMBER_CONFIRM_TIMEOUT);
    }

    //将钱包会员支付超时队列绑定到死信交换机
    @Bean
    public Binding bindingWalletMemberPaymentTimeoutQueue() {
        return bindingQueueToExchange(walletMemberPaymentTimeoutQueue(), RabbitMqConstants.WALLET_MEMBER_PAYMENT_TIMEOUT);
    }


    //会员登录日志记录队列
    @Bean
    public Queue loginLogQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_LOGIN_LOG_QUEUE, true);
    }

    //会员操作日志记录队列
    @Bean
    public Queue operationLogQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_OPERATION_LOG_QUEUE, true);
    }

    //会员登录日志记录交换机
    @Bean
    public DirectExchange loginLogExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_LOGIN_LOG_EXCHANGE);
    }

    //会员操作日志记录交换机
    @Bean
    public DirectExchange operationLogExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_OPERATION_LOG_EXCHANGE);
    }

    //会员登录日志记录路由键
    @Bean
    public Binding bindingLoginLogQueue(Queue loginLogQueue, DirectExchange loginLogExchange) {
        return BindingBuilder.bind(loginLogQueue).to(loginLogExchange).with(RabbitMqConstants.WALLET_MEMBER_ROUTING_KEY_LOGIN_LOG);
    }

    //会员操作日志记录路由键
    @Bean
    public Binding bindingOperationLogQueue(Queue operationLogQueue, DirectExchange operationLogExchange) {
        return BindingBuilder.bind(operationLogQueue).to(operationLogExchange).with(RabbitMqConstants.WALLET_MEMBER_ROUTING_KEY_OPERATION_LOG);
    }


    @Bean
    Queue regularQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MEMBER_NOTIFY_SELLER_BY_VOICE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_ROUTING_KEY)
                .build();
    }


    //语音通知会员死信队列
    @Bean
    Queue notifySellerByVoiceDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_QUEUE).build();
    }

    //语音通知会员死信交换机
    @Bean
    DirectExchange notifySellerByVoiceDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_EXCHANGE);
    }

    //语音通知会员死信路由键
    @Bean
    Binding bindingNotifySellerByVoiceQueue() {
        return BindingBuilder.bind(notifySellerByVoiceDeadLetterQueue()).to(notifySellerByVoiceDeadLetterExchange()).with(RabbitMqConstants.WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_ROUTING_KEY);
    }


    //----------------------------------------------------
    /**
     * 代收订单支付超时 普通队列
     * 定义一个用于接收代收订单支付超时通知的普通队列。
     * 配置该队列的死信交换机和死信路由键，意味着当消息因为某些原因（如消息被拒绝或过期）无法处理时，会被发送到指定的死信交换机。
     *
     * @return {@link Queue}
     */
    @Bean
    Queue paymentCollectionTimeoutQueue() {
        // 创建一个持久的队列，如果RabbitMQ重启，队列仍然存在
        return QueueBuilder.durable(
                RabbitMqConstants.WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_ROUTING_KEY).build();
    }

    /**
     * 代收订单支付超时 死信队列
     * 定义一个死信队列，用于接收来自普通队列的无法处理的消息。
     *
     * @return {@link Queue}
     */
    @Bean
    Queue paymentCollectionTimeoutDeadLetterQueue() {
        // 创建一个持久的死信队列
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_QUEUE).build();
    }

    /**
     * 代收订单支付超时 死信交换机
     * 定义一个直接类型的死信交换机，用于接收来自普通队列的死信消息并根据路由键路由到死信队列。
     * @return {@link DirectExchange}
     */
    @Bean
    DirectExchange paymentCollectionTimeoutDeadLetterExchange() {
        // 创建一个直接类型的交换机
        return new DirectExchange(RabbitMqConstants.WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_EXCHANGE);
    }

    /**
     * 代收订单支付超时 绑定关系
     * 定义死信队列和死信交换机之间的绑定关系，使用死信路由键作为绑定键。
     * @return {@link Binding}
     */
    @Bean
    Binding bindingPaymentCollectionTimeoutQueue() {
        // 绑定死信队列到死信交换机，使用死信路由键
        return BindingBuilder.bind(paymentCollectionTimeoutDeadLetterQueue()).to(paymentCollectionTimeoutDeadLetterExchange()).with(
                RabbitMqConstants.WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_ROUTING_KEY);
    }
    //----------------------------------------------------




    //----------------------------------------------------
    // 清空每日交易数据队列
    @Bean
    public Queue dailyTradeClearQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_DAILY_TRADE_CLEAR_QUEUE, true);
    }

    // 清空每日交易数据交换机
    @Bean
    public DirectExchange dailyTradeClearExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_DAILY_TRADE_CLEAR_EXCHANGE);
    }

    // 绑定清空每日交易数据队列与交换机
    @Bean
    public Binding bindingDailyTradeClearQueue(Queue dailyTradeClearQueue, DirectExchange dailyTradeClearExchange) {
        return BindingBuilder.bind(dailyTradeClearQueue).to(dailyTradeClearExchange).with(RabbitMqConstants.WALLET_MEMBER_DAILY_TRADE_CLEAR_ROUTINGKEY);
    }
    //----------------------------------------------------


    //----------------------------------------------------
    // 完成实名认证任务队列
    @Bean
    public Queue realNameVerificationTaskQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_REAL_NAME_VERIFICATION_TASK_QUEUE, true);
    }

    // 完成实名认证任务交换机
    @Bean
    public DirectExchange realNameVerificationTaskExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_REAL_NAME_VERIFICATION_TASK_EXCHANGE);
    }

    // 绑定完成实名认证任务队列与交换机
    @Bean
    public Binding bindingRealNameVerificationTaskQueue(Queue realNameVerificationTaskQueue, DirectExchange realNameVerificationTaskExchange) {
        return BindingBuilder.bind(realNameVerificationTaskQueue).to(realNameVerificationTaskExchange).with(RabbitMqConstants.WALLET_MEMBER_REAL_NAME_VERIFICATION_TASK_ROUTINGKEY);
    }
    //----------------------------------------------------



    //----------------------------------------------------
    // 次日自动领取任务奖励 普通队列
    @Bean
    Queue autoClaimRewardQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MERCHANT_AUTO_CLAIM_REWARD_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_ROUTING_KEY).build();
    }

    // 次日自动领取任务奖励 死信队列
    @Bean
    Queue autoClaimRewardDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_QUEUE).build();
    }

    // 次日自动领取任务奖励 死信交换机
    @Bean
    DirectExchange autoClaimRewardDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_EXCHANGE);
    }

    // 次日自动领取任务奖励 绑定关系
    @Bean
    Binding bindingAutoClaimRewardQueue() {
        return BindingBuilder.bind(autoClaimRewardDeadLetterQueue()).to(autoClaimRewardDeadLetterExchange())
                .with(RabbitMqConstants.WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_ROUTING_KEY);
    }
    //----------------------------------------------------



    //----------------------------------------------------
    // 定时任务领取昨日任务奖励队列
    @Bean
    public Queue dailyRewardClaimTaskQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_QUEUE, true);
    }

    // 定时任务领取昨日任务奖励交换机
    @Bean
    public DirectExchange dailyRewardClaimTaskExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_EXCHANGE);
    }

    // 绑定定时任务领取昨日任务奖励队列与交换机
    @Bean
    public Binding bindingDailyRewardClaimTaskQueue(Queue dailyRewardClaimTaskQueue, DirectExchange dailyRewardClaimTaskExchange) {
        return BindingBuilder.bind(dailyRewardClaimTaskQueue).to(dailyRewardClaimTaskExchange).with(RabbitMqConstants.WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_ROUTINGKEY);
    }
    //----------------------------------------------------





    //----------------------------------------------------
    // 匹配超时自动取消订单 普通队列
    @Bean
    Queue matchTimeoutAutoCancelOrderQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY).build();
    }

    // 匹配超时自动取消订单 死信队列
    @Bean
    Queue matchTimeoutAutoCancelOrderDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_QUEUE).build();
    }

    // 匹配超时自动取消订单 死信交换机
    @Bean
    DirectExchange matchTimeoutAutoCancelOrderDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_EXCHANGE);
    }

    // 匹配超时自动取消订单 绑定关系
    @Bean
    Binding bindingMatchTimeoutAutoCancelOrderQueue() {
        return BindingBuilder.bind(matchTimeoutAutoCancelOrderDeadLetterQueue()).to(matchTimeoutAutoCancelOrderDeadLetterExchange())
                .with(RabbitMqConstants.WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY);
    }
//----------------------------------------------------

    //----------------------------------------------------
    // 会员禁用队列
    @Bean
    public Queue memberDisableQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_DISABLE_QUEUE, true);
    }

    // 会员禁用交换机
    @Bean
    public DirectExchange memberDisableExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_DISABLE_EXCHANGE);
    }

    // 会员禁用队列与交换机
    @Bean
    public Binding bindingMemberDisableQueue(Queue memberDisableQueue, DirectExchange memberDisableExchange) {
        return BindingBuilder.bind(memberDisableQueue).to(memberDisableExchange).with(RabbitMqConstants.WALLET_MEMBER_DISABLE_ROUTINGKEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------
    // 订单标记队列
    @Bean
    public Queue orderTaggingQueue() {
        return new Queue(RabbitMqConstants.WALLET_ORDER_TAGGING_QUEUE, true);
    }

    // 订单标记交换机
    @Bean
    public DirectExchange orderTaggingExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_ORDER_TAGGING_EXCHANGE);
    }

    // 订单标记队列与交换机
    @Bean
    public Binding bindingorderTaggingQueue(Queue orderTaggingQueue, DirectExchange orderTaggingExchange) {
        return BindingBuilder.bind(orderTaggingQueue).to(orderTaggingExchange).with(RabbitMqConstants.WALLET_ORDER_TAGGING_ROUTINGKEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------
    // 添加交易IP黑名单队列
    @Bean
    public Queue tradeIpBlackAddQueue() {
        return new Queue(RabbitMqConstants.WALLET_TRADE_IP_BLACK_ADD_QUEUE, true);
    }

    // 添加交易IP黑名单交换机
    @Bean
    public DirectExchange tradeIpBlackAddExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_TRADE_IP_BLACK_ADD_EXCHANGE);
    }

    // 添加交易IP黑名单队列与交换机
    @Bean
    public Binding bindingtradeIpBlackAddQueue(Queue tradeIpBlackAddQueue, DirectExchange tradeIpBlackAddExchange) {
        return BindingBuilder.bind(tradeIpBlackAddQueue).to(tradeIpBlackAddExchange).with(RabbitMqConstants.WALLET_TRADE_IP_BLACK_ADD_ROUTINGKEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------
    // 会员确认超时风控标记 普通队列
    @Bean
    Queue memberConfirmTimeoutRiskTagQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_ROUTING_KEY).build();
    }
    // 会员确认超时风控标记 死信队列
    @Bean
    Queue memberConfirmTimeoutRiskTagDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_QUEUE).build();
    }

    // 会员确认超时风控标记 死信交换机
    @Bean
    DirectExchange memberConfirmTimeoutRiskTagDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_EXCHANGE);
    }

    // 会员确认超时风控标记 绑定关系
    @Bean
    Binding bindingmemberConfirmTimeoutRiskTagQueue() {
        return BindingBuilder.bind(memberConfirmTimeoutRiskTagDeadLetterQueue()).to(memberConfirmTimeoutRiskTagDeadLetterExchange())
                .with(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_ROUTING_KEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------
    // 提现交易延时回调通知 普通队列
    @Bean
    Queue withdrawNotifyTimeoutQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WITHDRAW_NOTIFY_TIMEOUT_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_ROUTING_KEY).build();
    }
    // 提现交易延时回调通知 死信队列
    @Bean
    Queue withdrawNotifyTimeoutDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_QUEUE).build();
    }

    // 提现交易延时回调通知 死信交换机
    @Bean
    DirectExchange withdrawNotifyTimeoutDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_EXCHANGE);
    }

    // 提现交易延时回调通知 绑定关系
    @Bean
    Binding bindingWithdrawNotifyTimeoutQueue() {
        return BindingBuilder.bind(withdrawNotifyTimeoutDeadLetterQueue()).to(withdrawNotifyTimeoutDeadLetterExchange())
                .with(RabbitMqConstants.WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_ROUTING_KEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------
    // 会员确认超时自动取消订单 普通队列
    @Bean
    Queue walletMemberConfirmTimeoutCancelOrderQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY).build();
    }
    // 会员确认超时自动取消订单 死信队列
    @Bean
    Queue walletMemberConfirmTimeoutCancelOrderDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_QUEUE).build();
    }

    // 会员确认超时自动取消订单 死信交换机
    @Bean
    DirectExchange walletMemberConfirmTimeoutCancelOrderDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_EXCHANGE);
    }

    // 会员确认超时自动取消订单 绑定关系
    @Bean
    Binding bindingWalletMemberConfirmTimeoutCancelOrderQueue() {
        return BindingBuilder.bind(walletMemberConfirmTimeoutCancelOrderDeadLetterQueue()).to(walletMemberConfirmTimeoutCancelOrderDeadLetterExchange())
                .with(RabbitMqConstants.WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------
    // 人工审核超时自动确认完成订单 普通队列
    @Bean
    Queue auditTimoutConfirmFinishOrderQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_ROUTING_KEY).build();
    }
    // 人工审核超时自动确认完成订单 死信队列
    @Bean
    Queue auditTimoutConfirmFinishOrderDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_QUEUE).build();
    }

    // 人工审核超时自动确认完成订单 死信交换机
    @Bean
    DirectExchange auditTimoutConfirmFinishOrderDeadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_EXCHANGE);
    }

    // 人工审核超时自动确认完成订单 绑定关系
    @Bean
    Binding bindingAuditTimoutConfirmFinishOrderQueue() {
        return BindingBuilder.bind(auditTimoutConfirmFinishOrderDeadLetterQueue()).to(auditTimoutConfirmFinishOrderDeadLetterExchange())
                .with(RabbitMqConstants.AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_ROUTING_KEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------

    //----------------------------------------------------
    // 同步会员进行中订单缓存队列
    @Bean
    public Queue memberProcessingOrderQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_PROCESSING_ORDER_QUEUE, true);
    }

    // 同步会员进行中订单缓存交换机
    @Bean
    public DirectExchange memberProcessingOrderExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_PROCESSING_ORDER_EXCHANGE);
    }

    // 同步会员进行中订单缓存队列与交换机
    @Bean
    public Binding bindingMemberProcessingOrderQueue(Queue memberProcessingOrderQueue, DirectExchange memberProcessingOrderExchange) {
        return BindingBuilder.bind(memberProcessingOrderQueue).to(memberProcessingOrderExchange).with(RabbitMqConstants.WALLET_MEMBER_PROCESSING_ORDER_ROUTINGKEY);
    }
    //----------------------------------------------------

    //----------------------------------------------------
    // 会员升级队列
    @Bean
    public Queue memberUpgradeQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_UPGRADE_QUEUE, true);
    }

    // 会员升级交换机
    @Bean
    public DirectExchange memberUpgradeExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_UPGRADE_EXCHANGE);
    }

    // 会员升级队列与交换机
    @Bean
    public Binding bindingMemberUpgradeQueue(Queue memberUpgradeQueue, DirectExchange memberUpgradeExchange) {
        return BindingBuilder.bind(memberUpgradeQueue).to(memberUpgradeExchange).with(RabbitMqConstants.WALLET_MEMBER_UPGRADE_ROUTINGKEY);
    }
    //----------------------------------------------------


    //----------------------------------------------------
    //获取KYC银行交易记录 队列
    @Bean
    public Queue kycTransactionQueue() {
        return new Queue(RabbitMqConstants.WALLET_MEMBER_KYC_TRANSACTION_QUEUE, true);
    }

    //获取KYC银行交易记录 交换机
    @Bean
    public DirectExchange kycTransactionExchange() {
        return new DirectExchange(RabbitMqConstants.WALLET_MEMBER_KYC_TRANSACTION_EXCHANGE);
    }

    //绑定获取KYC银行交易记录队列与交换机
    @Bean
    public Binding bindingKycTransactionQueue(Queue kycTransactionQueue, DirectExchange kycTransactionExchange) {
        return BindingBuilder.bind(kycTransactionQueue).to(kycTransactionExchange).with(RabbitMqConstants.WALLET_MEMBER_KYC_TRANSACTION_ROUTINGKEY);
    }
    //----------------------------------------------------

}
