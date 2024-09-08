package org.ar.wallet.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author
 */
@Data
@ApiModel(description = "买入下单接口返回数据")
public class BuyVo implements Serializable {


    /**
     * 转账金额
     */
    @ApiModelProperty(value = "转账金额")
    private BigDecimal amount;

    /**
     * UPI_ID
     */
    @ApiModelProperty(value = "UPI_ID")
    private String upiId;

    /**
     * UPI_Name
     */
    @ApiModelProperty(value = "UPI_Name")
    private String upiName;

    /**
     * 订单号
     */
    @ApiModelProperty(value = "订单号")
    private String platformOrder;

    /**
     * 支付剩余时间
     */
    @ApiModelProperty(value = "支付剩余时间 单位: 秒  如果值为null或负数 表示该笔订单已过期")
    private Long paymentExpireTime;

    /**
     * 随机码
     */
    @ApiModelProperty(value = "随机码")
    private String randomCode;
}