package org.ar.common.pay.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@ApiModel(description = "代收订单对象")
public class RechargeOrderExportDTO implements Serializable {


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "订单时间")
    private LocalDateTime createTime;

    /**
     * 商户名称
     */
    @ApiModelProperty(value = "商户名称")
    private String merchantName;

    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号")
    private String merchantCode;

    /**
     * 钱包会员ID
     */
    @ApiModelProperty(value = "会员ID")
    private String memberId;

    @ApiModelProperty(value = "订单号")
    private String platformOrder;

    /**
     * 商户会员ID
     */
    @ApiModelProperty(value = "商户会员ID")
    private String externalMemberId;

    @ApiModelProperty(value = "商户订单号")
    private String merchantOrder;

    @ApiModelProperty(value = "订单金额")
    private String amount;

    @ApiModelProperty(value = "奖励金额")
    private String cost;

    /**
     * 完成时间
     */
    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completionTime;

    /**
     * 交易回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "回调时间")
    private LocalDateTime tradeCallbackTime;


    @ApiModelProperty(value = "支付状态")
    private String orderStatus;

    /**
     * 交易回调状态 默认状态: 未回调
     */
    @ApiModelProperty(value = "回调状态")
    private String tradeCallbackStatus;


}