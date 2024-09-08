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
public class RechargeOrderDTO implements Serializable {


    @ApiModelProperty(value = "id")
    private Long id;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "订单时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "商户订单号")
    private String merchantOrder;

    @ApiModelProperty(value = "平台订单号")
    private String platformOrder;

    @ApiModelProperty(value = "支付方式")
    private String payType;

    @ApiModelProperty(value = "订单金额")
    private BigDecimal amount;


    @ApiModelProperty(value = "手续费")
    private BigDecimal cost;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "订单完成时间")
    private LocalDateTime updateTime;

    /**
     * 交易回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "订单回调时间")
    private LocalDateTime tradeCallbackTime;

    @ApiModelProperty(value = "订单状态")
    private String orderStatus;

    /**
     * 交易回调状态 默认状态: 未回调
     */
    @ApiModelProperty(value = "订单回调状态")
    private String tradeCallbackStatus;

    /**
     * 交易回调状态 默认状态: 未回调
     */
    @ApiModelProperty(value = "备注")
    private String remark;

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
     * 完成时间
     */
    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completionTime;


    /**
     * 钱包会员ID
     */
    @ApiModelProperty(value = "钱包会员ID")
    private String memberId;

    /**
     * 商户会员ID
     */
    @ApiModelProperty(value = "商户会员ID")
    private String externalMemberId;


}