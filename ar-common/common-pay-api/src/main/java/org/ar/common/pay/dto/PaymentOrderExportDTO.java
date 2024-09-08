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
@ApiModel(description = "卖出订单列表返回")
public class PaymentOrderExportDTO implements Serializable {


    /**
     * 会员ID
     */
    @ApiModelProperty("会员ID")
    private String memberId;

    /**
     * 匹配订单号
     */
    @ApiModelProperty("匹配订单号")
    private String matchOrder;

    /**
     * 平台订单号
     */
    @ApiModelProperty("平台订单号")
    private String platformOrder;

    /**
     * UTR
     */
    @ApiModelProperty("UTR")
    private String utr;

    /**
     * 商户名称
     */
    @ApiModelProperty("商户名称")
    private String merchantName;

    /**
     * 订单金额
     */
    @ApiModelProperty("订单金额")
    private String amount;
    /**
     * 实际金额
     */
    @ApiModelProperty("实际金额")
    private String actualAmount;

    /**
     * 奖励
     */
    @ApiModelProperty("奖励")
    private String bonus;


    @ApiModelProperty("完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;


    /**
     * 匹配时长
     */
    @ApiModelProperty("匹配时长")
    private String matchDuration;

    /**
     * 完成时长
     */
    @ApiModelProperty("完成时长")
    private String completeDuration;

    /**
     * 订单状态 默认状态: 待匹配
     */
    @ApiModelProperty("订单状态")
    private String orderStatus;



    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;






}