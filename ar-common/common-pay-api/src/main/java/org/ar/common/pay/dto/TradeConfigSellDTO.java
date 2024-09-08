package org.ar.common.pay.dto;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 交易配置表
 *
 *
 */
@Data
@ApiModel(description = "配置信息")
public class TradeConfigSellDTO implements Serializable {

     private Long id;


    /**
     * 商户会员卖出奖励比例
     */
    @ApiModelProperty("商户会员卖出奖励比例")
    private BigDecimal merchantSalesBonus;

    /**
     * 钱包用户卖出奖励比例
     */
    @ApiModelProperty("钱包用户卖出奖励比例")
    private BigDecimal memberSalesBonus;

    /**
     * 商户会员卖出最多订单数
     */
    @ApiModelProperty("商户会员卖出最多订单数")
    private Integer merchantMaxSellOrderNum;

    /**
     * 钱包用户卖出最多订单数
     */
    @ApiModelProperty("钱包用户卖出最多订单数")
    private Integer memberMaxSellOrderNum;

    /**
     * 钱包用户最多拆单数
     */
    @ApiModelProperty("钱包用户最多拆单数")
    private Integer maxSplitOrderCount;

    /**
     * 商户会员最大卖出金额
     */
    @ApiModelProperty("商户会员最大卖出金额")
    private BigDecimal merchantMaxSellAmount;

    /**
     * 钱包用户最大卖出金额
     */
    @ApiModelProperty("钱包用户最大卖出金额")
    private BigDecimal memberMaxSellAmount;

    /**
     * 钱包用户确认超时时间
     */
    @ApiModelProperty("钱包用户确认超时时间")
    private Integer memberConfirmExpirationTime;

    /**
     * 商户会员确认超时时间
     */
    @ApiModelProperty("商户会员确认超时时间")
    private Integer merchantConfirmExpirationTime;

    /**
     * 商户会员卖出匹配时长
     */
    @ApiModelProperty("商户会员卖出匹配时长")
    private Integer merchantSellMatchingDuration;

    /**
     * 钱包用户卖出匹配时长
     */
    @ApiModelProperty("钱包用户卖出匹配时长")
    private Integer memberSellMatchingDuration;

    /**
     * 是否开启拆单 默认值 0
     */
    @ApiModelProperty(value = "是否开启拆单, 取值说明: 0: 关闭拆单, 1: 开启拆单")
    private Integer isSplitOrder;

    /**
     * 确认超时时间
     */
    @ApiModelProperty("确认超时时长")
    private Integer confirmExpirationTime;


    /**
     * 是否人工审核
     */
    @ApiModelProperty("是否人工审核")
    private Integer isManualReview;

    /**
     * 审核时间
     */
    @ApiModelProperty("审核时间")
    private Integer manualReviewTime;

}