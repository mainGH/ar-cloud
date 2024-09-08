package org.ar.manager.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 会员对账报表
 * </p>
 *
 * @author 
 * @since 2024-03-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("bi_member_reconciliation")
public class BiMemberReconciliation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    @ApiModelProperty("日期")
    private String dateTime;

    /**
     * 商户编码
     */
    @ApiModelProperty("商户编码")
    private String merchantCode;

    /**
     * 商户名称
     */
    @ApiModelProperty("商户名称")
    private String merchantName;

    /**
     * 商户类型: 1.内部商户 2.外部商户
     */
    @ApiModelProperty("商户类型: 1.内部商户 2.外部商户")
    private String merchantType;

    /**
     * 会员金额
     */
    @ApiModelProperty("会员金额")
    private BigDecimal memberBalance = BigDecimal.ZERO;

    /**
     * 代收金额
     */
    @ApiModelProperty("代收金额")
    private BigDecimal payMoney = BigDecimal.ZERO;

    /**
     * 商户代付金额
     */
    @ApiModelProperty("商户代付金额")
    private BigDecimal withdrawMoney = BigDecimal.ZERO;

    /**
     * 商户代收费用
     */
    @ApiModelProperty("商户代收费用")
    private BigDecimal payFee = BigDecimal.ZERO;

    /**
     * 商户代付费用
     */
    @ApiModelProperty("商户代付费用")
    private BigDecimal withdrawFee = BigDecimal.ZERO;

    @ApiModelProperty("usdt买入")
    private BigDecimal usdtBuyMoney = BigDecimal.ZERO;

    /**
     * 卖出奖励
     */
    @ApiModelProperty("卖出奖励")
    private BigDecimal sellReward = BigDecimal.ZERO;

    /**
     * 买入奖励
     */
    @ApiModelProperty("买入奖励")
    private BigDecimal buyReward = BigDecimal.ZERO;

    /**
     * 会员上分
     */
    @ApiModelProperty("会员上分")
    private BigDecimal memberUp = BigDecimal.ZERO;

    /**
     * 会员下分
     */
    @ApiModelProperty("会员下分")
    private BigDecimal memberDown = BigDecimal.ZERO;

    /**
     * 任务奖励
     */
    @ApiModelProperty("任务奖励")
    private BigDecimal taskReward = BigDecimal.ZERO;

    /**
     * 会员账目偏差
     */
    @ApiModelProperty("会员账目偏差")
    private BigDecimal memberDiff = BigDecimal.ZERO;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time",fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time",fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;




    @TableField(exist = false)
    private BigDecimal balanceTotal;

    @TableField(exist = false)
    private BigDecimal payMoneyTotal;

    @TableField(exist = false)
    private BigDecimal withdrawMoneyTotal;

    @TableField(exist = false)
    private BigDecimal payFeeTotal;

    @TableField(exist = false)
    private BigDecimal withdrawFeeTotal;

    @TableField(exist = false)
    private BigDecimal sellRewardTotal;

    @TableField(exist = false)
    private BigDecimal buyRewardTotal;

    @TableField(exist = false)
    private BigDecimal memberUpTotal;

    @TableField(exist = false)
    private BigDecimal memberDownTotal;

    @TableField(exist = false)
    private BigDecimal memberDiffTotal;

    @TableField(exist = false)
    private BigDecimal usdtBuyMoneyTotal;

    @TableField(exist = false)
    private BigDecimal taskRewardTotal;


}
