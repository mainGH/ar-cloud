package org.ar.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 交易配置表
 *
 * @author
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("trade_config")
public class TradeConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付过期时间
     */
    private Integer rechargeExpirationTime;

    /**
     * 失败次数
     */
    private Integer numberFailures;

    /**
     * 禁用买入时间(小时)
     */
    private Integer disabledTime;

    /**
     * USDT汇率
     */
    private BigDecimal usdtCurrency;

    /**
     * 商户会员最大买入金额
     */
    private BigDecimal merchantMaxPurchaseAmount;

    /**
     * 钱包用户最大买入金额
     */
    private BigDecimal memberMaxPurchaseAmount;

    /**
     * 商户会员卖出奖励比例
     */
    private BigDecimal merchantSalesBonus;

    /**
     * 钱包用户卖出奖励比例
     */
    private BigDecimal memberSalesBonus;

    /**
     * 商户会员卖出最多订单数
     */
    private Integer merchantMaxSellOrderNum;

    /**
     * 钱包用户卖出最多订单数
     */
    private Integer memberMaxSellOrderNum;

    /**
     * 钱包用户最多拆单数
     */
    private Integer maxSplitOrderCount;

    /**
     * 商户会员最大卖出金额
     */
    private BigDecimal merchantMaxSellAmount;

    /**
     * 钱包用户最大卖出金额
     */
    private BigDecimal memberMaxSellAmount;

    /**
     * 钱包用户确认超时时间
     */
    private Integer memberConfirmExpirationTime;

    /**
     * 商户会员确认超时时间
     */
    private Integer merchantConfirmExpirationTime;

    /**
     * 钱包用户卖出匹配时长
     */
    private Integer memberSellMatchingDuration;

    /**
     * 商户会员卖出匹配时长
     */
    private Integer merchantSellMatchingDuration;

    /**
     * 是否开启拆单 默认值 0
     */
    private Integer isSplitOrder = 0;

    /**
     * 会员最小卖出数量
     */
    private BigDecimal memberMinimumSellAmount;

    /**
     * 会员单个upi单日最多收款笔数
     */
    private Integer maxDailyUpiTransactions;

    /**
     * 到账语音提醒功能开关 1: 开启 0: 关闭
     */
    private Integer voicePaymentReminderEnabled;

    /**
     * 匹配超时自动取消时长
     */
    private Integer matchOverTimeAutoCancelDuration;

    /**
     * 预警余额
     */
    private BigDecimal warningBalance;

    /**
     * 短信余额报警阈值
     */
    private BigDecimal messageBalanceThreshold;

    /**
     * 确认超时未操作
     */
    private Integer warningConfirmOvertimeNotOperated;

    /**
     * 是否人工审核
     */
    private Integer isManualReview;

    /**
     * 审核时间
     */
    private Integer manualReviewTime;

    /**
     * 交易信用分限制
     */
    private BigDecimal tradeCreditScoreLimit;

    /**
     * 商户订单未产生预警
     */
    private Integer merchantOrderUncreatedTime;
}