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
@ApiModel(description = "充值列表返回")
public class CollectionOrderDTO implements Serializable {


    @ApiModelProperty("主键")
    private Long id;


    @ApiModelProperty("会员Id")
    private String memberId;

    @ApiModelProperty("会员账号")
    private String memberAccount;

    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

//    /**
//     * 支付方式 默认值: UPI
//     */
//    private String payType;

    /**
     * 商户订单号
     */
    @ApiModelProperty("商户订单号")
    private String merchantOrder;

    /**
     * 平台订单号
     */
    @ApiModelProperty("平台订单号")
    private String platformOrder;

    /**
     * 订单金额
     */
    @ApiModelProperty("订单金额")
    private BigDecimal amount;



    /**
     * 订单状态 默认状态: 待支付
     */
    @ApiModelProperty("订单状态")
    private String orderStatus;

    /**
     * 交易回调状态 默认状态: 未回调
     */
    @ApiModelProperty("交易回调状态")
    private String tradeCallbackStatus;

    /**
     * 商户号
     */
    @ApiModelProperty("商户号")
    private String merchantCode;




    /**
     * 交易回调时间
     */
    @ApiModelProperty("回调时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeCallbackTime;





    /**
     * UTR
     */
    @ApiModelProperty("utr")
    private String utr;

    /**
     * 奖励
     */
    @ApiModelProperty("奖励")
    private String bonus;

    /**
     * 实际金额
     */
    @ApiModelProperty("实际金额")
    private BigDecimal actualAmount;




    /**
     * 完成时长
     */
    @ApiModelProperty("完成时长")
    private BigDecimal completeDuration;


    /**
     * 完成时间
     */
    @ApiModelProperty("完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;

    /**
     * 商户名称
     */
    @ApiModelProperty("商户名称")
    private String merchantName;

    /**
     * 凭证
     */
    @ApiModelProperty("凭证")
    private String voucher;


    @ApiModelProperty(value = "风控标识-黑名单 0-正常 1-ip黑名单")
    private Integer riskTagBlack;

    @ApiModelProperty(value = "风控标识 0-正常 1-操作超时 2-ip黑名单 3-余额过低")
    private String riskTag;



    /**
     * 是否通过KYC自动完成 1: 是
     */
    @ApiModelProperty(value = "是否通过KYC自动完成 0：否 1: 是")
    private Integer kycAutoCompletionStatus;

}