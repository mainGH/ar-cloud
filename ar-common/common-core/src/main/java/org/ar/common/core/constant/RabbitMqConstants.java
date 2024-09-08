package org.ar.common.core.constant;

public class RabbitMqConstants {

    public static final Integer DELIVERING = 0;//消息投递中
    public static final Integer SUCCESS = 1;//消息投递成功
    public static final Integer FAILURE = 2;//消息投递失败
    public static final Integer MAX_TRY_COUNT = 3;//最大重试次数
    public static final Integer MSG_TIMEOUT = 1;//消息超时时间


    //钱包项目-充值交易通知-交换机
    public static final String AR_WALLET_TRADE_COLLECT_EXCHANGE = "ar.wallet.trade.collect.exchange";

    //钱包项目-充值交易通知-路由key
    public static final String AR_WALLET_TRADE_COLLECT_ROUTING_KEY = "ar.wallet.trade.collect.routing.key";

    //钱包项目-充值交易通知-队列
    public static final String AR_WALLET_TRADE_COLLECT_QUEUE_NAME = "ar.wallet.trade.collect.queue";


    //钱包项目-提现交易通知-交换机
    public static final String AR_WALLET_TRADE_PAYMENT_EXCHANGE = "ar.wallet.trade.payment.exchange";

    //钱包项目-提现交易通知-路由key
    public static final String AR_WALLET_TRADE_PAYMENT_ROUTING_KEY = "ar.wallet.trade.payment.routing.key";

    //钱包项目-提现交易通知-队列
    public static final String AR_WALLET_TRADE_PAYMENT_QUEUE_NAME = "ar.wallet.trade.payment.queue";


    //订单超时处理 start-------------------------------------------------
    //流程: 先将消息发送到主队列(无人监听)并设置消息过期时间 消息过期后被路由到死信队列 消费者监听死信队列进行处理延时任务


    // 钱包项目-延迟任务-订单超时-死信交换机
    public static final String AR_WALLET_DEAD_LETTER_EXCHANGE = "ar.wallet.dead_letter.exchange";


    // 1.钱包会员匹配超时死信队列
    public static final String AR_WALLET_MEMBER_MATCH_DEAD_LETTER_QUEUE = "ar.wallet.member.match.dead.letter.queue";

    // 2.商户会员匹配超时死信队列
    public static final String AR_WALLET_MERCHANT_MEMBER_MATCH_DEAD_LETTER_QUEUE = "ar.wallet.merchant.member.match.dead_letter.queue";

    // 3.钱包会员确认超时死信队列
    public static final String AR_WALLET_MEMBER_CONFIRM_DEAD_LETTER_QUEUE = "ar.wallet.member.confirm.dead_letter.queue";

    // 4.商户会员确认超时死信队列
    public static final String AR_WALLET_MERCHANT_MEMBER_CONFIRM_DEAD_LETTER_QUEUE = "ar.wallet.merchant.member.confirm.dead_letter.queue";


    // 5.钱包会员支付超时死信队列
    public static final String AR_WALLET_MEMBER_PAYMENT_DEAD_LETTER_QUEUE = "ar.wallet.member.payment.dead_letter.queue";


    // 1. 钱包会员匹配超时主队列
    public static final String AR_WALLET_MEMBER_MATCH_TIMEOUT_QUEUE = "ar.wallet.member.matchtimeout.queue";
    // 2. 商户会员匹配超时主队列
    public static final String AR_WALLET_MERCHANT_MEMBER_MATCH_TIMEOUT_QUEUE = "ar.wallet.merchant.member.match.timeout.queue";
    // 3. 钱包会员确认超时主队列
    public static final String AR_WALLET_MEMBER_CONFIRM_TIMEOUT_QUEUE = "ar.wallet.member.confirm.timeout.queue";
    // 4. 商户会员确认超时主队列
    public static final String AR_WALLET_MERCHANT_MEMBER_CONFIRM_TIMEOUT_QUEUE = "ar.wallet.merchant.member.confirm.timeout.queue";
    // 5. 钱包会员支付超时主队列
    public static final String AR_WALLET_MEMBER_PAYMENT_TIMEOUT_QUEUE = "ar.wallet.member.payment.timeout.queue";


    //1.将钱包会员匹配超时队列绑定到死信交换机路由key
    public static final String WALLET_MEMBER_MATCH_TIMEOUT = "wallet.member.match.timeout";
    //2.将商户会员匹配超时队列绑定到死信交换机路由key
    public static final String MERCHANT_MEMBER_MATCH_TIMEOUT = "merchant.member.match.timeout";
    //3.将钱包会员确认超时队列绑定到死信交换机路由key
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT = "wallet.member.confirm.timeout";
    //4.将商户会员确认超时队列绑定到死信交换机路由key
    public static final String MERCHANT_MEMBER_CONFIRM_TIMEOUT = "merchant.member.confirm.timeout";
    //5.将钱包会员支付超时队列绑定到死信交换机路由key
    public static final String WALLET_MEMBER_PAYMENT_TIMEOUT = "wallet.member.payment.timeout";


    //1.钱包会员匹配超时队列 超时后 路由到死信交换机的key
    public static final String WALLET_MEMBER_MATCH_PROCESS = "wallet.member.match.process";
    //2.将商户会员匹配超时队 超时后 路由到死信交换机的key
    public static final String MERCHANT_MEMBER_MATCH_PROCESS = "merchant.member.match.process";
    //3.将钱包会员确认超时队 超时后 路由到死信交换机的key
    public static final String WALLET_MEMBER_CONFIRM_PROCESS = "wallet.member.confirm.process";
    //4.将商户会员确认超时队 超时后 路由到死信交换机的key
    public static final String MERCHANT_MEMBER_CONFIRM_PROCESS = "merchant.member.confirm.process";
    //5.将钱包会员支付超时队 超时后 路由到死信交换机的key
    public static final String WALLET_MEMBER_PAYMENT_PROCESS = "wallet.member.payment.process";


    //订单超时处理 end-------------------------------------------------

    // 会员登录日志 和 操作日志记录 MQ

    // 队列名称
    public static final String WALLET_MEMBER_LOGIN_LOG_QUEUE = "wallet.member.login.log.queue";
    public static final String WALLET_MEMBER_OPERATION_LOG_QUEUE = "wallet.member.operation.log.queue";

    // 交换机名称
    public static final String WALLET_MEMBER_LOGIN_LOG_EXCHANGE = "wallet.member.login.log.exchange";
    public static final String WALLET_MEMBER_OPERATION_LOG_EXCHANGE = "wallet.member.operation.log.exchange";

    // 路由键
    public static final String WALLET_MEMBER_ROUTING_KEY_LOGIN_LOG = "wallet.member.routing.key.login.log";
    public static final String WALLET_MEMBER_ROUTING_KEY_OPERATION_LOG = "wallet.member.routing.key.operation.log";


    // 语音通知卖方 普通队列名称
    public static final String WALLET_MEMBER_NOTIFY_SELLER_BY_VOICE_QUEUE = "wallet.member.notify.seller.by.voice.queue";

    //语音通知卖方 死信队列名称
    public static final String WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_QUEUE = "wallet.member.dead.letter.notify.seller.by.voice.queue";

    //语音通知卖方 死信交换机名称
    public static final String WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_EXCHANGE = "wallet.member.dead.letter.notify.seller.by.voice.exchange";

    //语音通知卖方 死信路由键
    public static final String WALLET_MEMBER_DEAD_LETTER_NOTIFY_SELLER_BY_VOICE_ROUTING_KEY = "wallet.member.dead.letter.notify.seller.by.voice.routing.key";


    // 代收订单支付超时 普通队列名称
    public static final String WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_QUEUE = "wallet.merchant.collect.order.payment.timeout.queue";

    //代收订单支付超时 死信队列名称
    public static final String WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_QUEUE = "wallet.merchant.collect.order.payment.timeout.dead.letter.queue";

    //代收订单支付超时 死信交换机名称
    public static final String WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_EXCHANGE = "wallet.merchant.collect.order.payment.timeout.dead.letter.exchange";

    //代收订单支付超时 死信路由键
    public static final String WALLET_MERCHANT_COLLECT_ORDER_PAYMENT_TIMEOUT_DEAD_LETTER_ROUTING_KEY = "wallet.merchant.collect.order.payment.timeout.dead.letter.routing.key";


    // 统计在线人数 MQ-----------------------------
    // 队列名称
    public static final String WALLET_MEMBER_ONLINE_COUNT_QUEUE = "wallet.member.online.count.queue";

    // 交换机名称
    public static final String WALLET_MEMBER_ONLINE_COUNT_EXCHANGE = "wallet.member.online.count.exchange";

    // 路由键
    public static final String WALLET_MEMBER_ONLINE_COUNT_ROUTINGKEY = "wallet.member.online.count.routingkey";
    //------------------------------------------


    // 清空每日交易数据 MQ-----------------------------
    // 清空每日交易数据队列
    public static final String WALLET_MEMBER_DAILY_TRADE_CLEAR_QUEUE = "wallet.member.daily.trade.clear.queue";

    // 清空每日交易数据交换机
    public static final String WALLET_MEMBER_DAILY_TRADE_CLEAR_EXCHANGE = "wallet.member.daily.trade.clear.exchange";

    // 清空每日交易数据路由键
    public static final String WALLET_MEMBER_DAILY_TRADE_CLEAR_ROUTINGKEY = "wallet.member.daily.trade.clear.routingkey";
    //------------------------------------------


    // 完成实名认证任务 MQ-----------------------------
    // 完成实名认证任务队列
    public static final String WALLET_MEMBER_REAL_NAME_VERIFICATION_TASK_QUEUE = "wallet.member.real.name.verification.task.queue";

    // 完成实名认证任务交换机
    public static final String WALLET_MEMBER_REAL_NAME_VERIFICATION_TASK_EXCHANGE = "wallet.member.real.name.verification.task.exchange";

    // 完成实名认证任务路由键
    public static final String WALLET_MEMBER_REAL_NAME_VERIFICATION_TASK_ROUTINGKEY = "wallet.member.real.name.verification.task.routingkey";
    //------------------------------------------


    //------------------------------------------
    // 次日自动领取任务奖励 普通队列名称
    public static final String WALLET_MERCHANT_AUTO_CLAIM_REWARD_QUEUE = "wallet.merchant.auto.claim.reward.queue";

    // 次日自动领取任务奖励 死信队列名称
    public static final String WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_QUEUE = "wallet.merchant.auto.claim.reward.dead.letter.queue";

    // 次日自动领取任务奖励 死信交换机名称
    public static final String WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_EXCHANGE = "wallet.merchant.auto.claim.reward.dead.letter.exchange";

    // 次日自动领取任务奖励 死信路由键
    public static final String WALLET_MERCHANT_AUTO_CLAIM_REWARD_DEAD_LETTER_ROUTING_KEY = "wallet.merchant.auto.claim.reward.dead.letter.routing.key";
    //------------------------------------------


    //------------------------------------------
    // 定时任务领取昨日任务奖励队列
    public static final String WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_QUEUE = "wallet.member.daily.reward.claim.task.queue";

    // 定时任务领取昨日任务奖励交换机
    public static final String WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_EXCHANGE = "wallet.member.daily.reward.claim.task.exchange";

    // 定时任务领取昨日任务奖励路由键
    public static final String WALLET_MEMBER_DAILY_REWARD_CLAIM_TASK_ROUTINGKEY = "wallet.member.daily.reward.claim.task.routingkey";
    //------------------------------------------



    //------------------------------------------
    // 匹配超时自动取消订单 普通队列名称
    public static final String WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_QUEUE = "wallet.match.timeout.auto.cancel.order.queue";

    // 匹配超时自动取消订单 死信队列名称
    public static final String WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_QUEUE = "wallet.match.timeout.auto.cancel.order.dead.letter.queue";

    // 匹配超时自动取消订单 死信交换机名称
    public static final String WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_EXCHANGE = "wallet.match.timeout.auto.cancel.order.dead.letter.exchange";

    // 匹配超时自动取消订单 死信路由键
    public static final String WALLET_MATCH_TIMEOUT_AUTO_CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY = "wallet.match.timeout.auto.cancel.order.dead.letter.routing.key";
//------------------------------------------


    // 禁用会员 MQ-----------------------------
    // 禁用会员队列
    public static final String WALLET_MEMBER_DISABLE_QUEUE = "wallet.member.disable.queue";

    // 禁用会员交换机
    public static final String WALLET_MEMBER_DISABLE_EXCHANGE = "wallet.member.disable.exchange";

    // 禁用会员路由键
    public static final String WALLET_MEMBER_DISABLE_ROUTINGKEY = "wallet.member.disable.routingkey";
    //------------------------------------------


    // 订单标记 MQ-----------------------------
    // 订单标记队列
    public static final String WALLET_ORDER_TAGGING_QUEUE = "wallet.order.tagging.queue";

    // 订单标记交换机
    public static final String WALLET_ORDER_TAGGING_EXCHANGE = "wallet.order.tagging.exchange";

    // 订单标记路由键
    public static final String WALLET_ORDER_TAGGING_ROUTINGKEY = "wallet.order.tagging.routingkey";
    //------------------------------------------


    // 添加交易IP黑名单 MQ-----------------------------
    // 添加交易IP黑名单队列
    public static final String WALLET_TRADE_IP_BLACK_ADD_QUEUE = "wallet.trade.ip.black.add.queue";

    // 添加交易IP黑名单交换机
    public static final String WALLET_TRADE_IP_BLACK_ADD_EXCHANGE = "wallet.trade.ip.black.add.exchange";

    // 添加交易IP黑名单路由键
    public static final String WALLET_TRADE_IP_BLACK_ADD_ROUTINGKEY = "wallet.trade.ip.black.add.routingkey";
    //------------------------------------------

    //------------------------------------------
    // 会员确认超时风控标记 普通队列名称
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_QUEUE = "wallet.member.confirm.timeout.risk.tag.queue";

    // 会员确认超时风控标记 死信队列名称
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_QUEUE = "wallet.member.confirm.timeout.risk.tag.dead.letter.queue";

    // 会员确认超时风控标记 死信交换机名称
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_EXCHANGE = "wallet.member.confirm.timeout.risk.tag.dead.letter.exchange";

    // 会员确认超时风控标记 死信路由键
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_RISK_TAG_DEAD_LETTER_ROUTING_KEY = "wallet.member.confirm.timeout.risk.tag.dead.letter.routing.key";
//------------------------------------------

    //------------------------------------------
    // 提现交易延时回调通知 普通队列名称
    public static final String WITHDRAW_NOTIFY_TIMEOUT_QUEUE = "withdraw.notify.timeout.queue";

    // 提现交易延时回调通知 死信队列名称
    public static final String WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_QUEUE = "withdraw.notify.timeout.dead.letter.queue";

    // 提现交易延时回调通知 死信交换机名称
    public static final String WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_EXCHANGE = "withdraw.notify.timeout.dead.letter.exchange";

    // 提现交易延时回调通知 死信路由键
    public static final String WITHDRAW_NOTIFY_TIMEOUT_DEAD_LETTER_ROUTING_KEY = "withdraw.notify.timeout.dead.letter.routing.key";
//------------------------------------------

    //------------------------------------------
    // 会员确认超时自动取消订单 普通队列名称
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_QUEUE = "wallet.member.confirm.timeout.cancel.order.queue";

    // 会员确认超时自动取消订单 死信队列名称
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_QUEUE = "wallet.member.confirm.timeout.cancel.order.dead.letter.queue";

    // 会员确认超时自动取消订单 死信交换机名称
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_EXCHANGE = "wallet.member.confirm.timeout.cancel.order.dead.letter.exchange";

    // 会员确认超时自动取消订单 死信路由键
    public static final String WALLET_MEMBER_CONFIRM_TIMEOUT_CANCEL_ORDER_DEAD_LETTER_ROUTING_KEY = "wallet.member.confirm.timeout.cancel.order.dead.letter.routing.key";
//------------------------------------------

    //------------------------------------------
    // 人工审核超时自动确认完成订单 普通队列名称
    public static final String AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_QUEUE = "audit.timeout.confirm.finish.order.queue";

    // 人工审核超时自动确认完成订单 死信队列名称
    public static final String AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_QUEUE = "audit.timeout.confirm.finish.order.dead.letter.queue";

    // 人工审核超时自动确认完成订单 死信交换机名称
    public static final String AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_EXCHANGE = "audit.timeout.confirm.finish.order.dead.letter.exchange";

    // 人工审核超时自动确认完成订单 死信路由键
    public static final String AUDIT_TIMEOUT_CONFIRM_FINISH_ORDER_DEAD_LETTER_ROUTING_KEY = "audit.timeout.confirm.finish.order.dead.letter.routing.key";
//------------------------------------------


//------------------------------------------

    // 同步会员进行中订单缓存 MQ-----------------------------
    // 同步会员进行中订单缓存队列
    public static final String WALLET_MEMBER_PROCESSING_ORDER_QUEUE = "wallet.member.processing.order.queue";

    // 同步会员进行中订单缓存交换机
    public static final String WALLET_MEMBER_PROCESSING_ORDER_EXCHANGE = "wallet.member.processing.order.exchange";

    // 同步会员进行中订单缓存路由键
    public static final String WALLET_MEMBER_PROCESSING_ORDER_ROUTINGKEY = "wallet.member.processing.order.routingkey";


    // 会员升级 MQ-----------------------------
    // 会员升级队列
    public static final String WALLET_MEMBER_UPGRADE_QUEUE = "wallet.member.upgrade.queue";

    // 会员升级交换机
    public static final String WALLET_MEMBER_UPGRADE_EXCHANGE = "wallet.member.upgrade.exchange";

    // 会员升级路由键
    public static final String WALLET_MEMBER_UPGRADE_ROUTINGKEY = "wallet.member.upgrade.routingkey";
    //------------------------------------------



    // 统计KYC交易记录 MQ-----------------------------
    // 队列名称
    public static final String WALLET_MEMBER_KYC_TRANSACTION_QUEUE = "wallet.member.kyc.transaction.queue";

    // 交换机名称
    public static final String WALLET_MEMBER_KYC_TRANSACTION_EXCHANGE = "wallet.member.kyc.transaction.exchange";

    // 路由键
    public static final String WALLET_MEMBER_KYC_TRANSACTION_ROUTINGKEY = "wallet.member.kyc.transaction.routingkey";
    //------------------------------------------

}
