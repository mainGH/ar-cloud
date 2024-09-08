package org.ar.wallet.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author
 */
@Data
@ApiModel(description = "卖出页面接口")
public class SellListVo implements Serializable {

    /**
     * 余额
     */
    @ApiModelProperty(value = "可用余额")
    private BigDecimal balance;

    /**
     * 卖出余额
     */
    @ApiModelProperty(value = "卖出余额")
    private BigDecimal sellBalance;

    /**
     * 交易中
     */
    @ApiModelProperty(value = "交易中")
    private BigDecimal inTransaction;

    /**
     * 卖出最多订单数
     */
    @ApiModelProperty(value = "卖出最多订单数")
    private Integer maxSellOrderNum;

    /**
     * 正在进行中的订单数量
     */
    @ApiModelProperty(value = "正在进行中的订单数量")
    private Long ongoingOrderCount = 0L;

    /**
     * 正在进行中的订单列表
     */
    @ApiModelProperty(value = "正在进行中的订单列表")
    private List<SellOrderListVo> ongoingOrders;

    /**
     * 卖出奖励比例
     */
    @ApiModelProperty(value = "卖出奖励比例 %")
    private BigDecimal salesBonus;

    /**
     * 最多拆单数
     */
    @ApiModelProperty(value = "最多拆单数")
    private Integer splitOrderCount;

    /**
     * 卖出匹配时长
     */
    @ApiModelProperty("卖出匹配时长 (分钟)")
    private Integer sellMatchingDuration;

    /**
     * 最大卖出金额
     */
    @ApiModelProperty("最大卖出金额")
    private BigDecimal maxSellAmount;

    /**
     * 是否开启拆单 默认值 0
     */
    @ApiModelProperty(value = "是否拆单, 取值说明: 0: 未拆单, 1: 已拆单")
    private Integer isSplitOrder = 0;

    /**
     * 默认收款信息id
     */
    @ApiModelProperty("默认收款信息id")
    private Long defaultPaymentInfoId = null;

    /**
     * 默认upi_id
     */
    @ApiModelProperty("默认upi_id")
    private String defaultUpiId = null;

    /**
     * 会员最小卖出数量
     */
    @ApiModelProperty("会员最小卖出数量")
    private BigDecimal memberMinimumSellAmount;

    /**
     * 会员卖出权限状态
     */
    @ApiModelProperty("会员卖出权限状态：1启用(可以卖) 0禁用(不可以卖)")
    private String memberSellStatus;

}