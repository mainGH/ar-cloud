package org.ar.common.pay.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author admin
 * @date 2024/3/13 9:37
 */
@Data
@ApiModel(description = "充值列表返回")
public class CollectionOrderExportDTO  implements Serializable {


    @ApiModelProperty("会员Id")
    private String memberId;


    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 平台订单号
     */
    @ApiModelProperty("平台订单号")
    private String platformOrder;

    /**
     * 商户名称
     */
    @ApiModelProperty("商户名称")
    private String merchantName;

    /**
     * UTR
     */
    @ApiModelProperty("UTR")
    private String utr;


    /**
     * 订单金额
     */
    @ApiModelProperty("订单金额")
    private String amount;



    /**
     * 奖励
     */
    @ApiModelProperty("奖励")
    private String bonus;



    /**
     * 实际金额
     */
    @ApiModelProperty("实际金额")
    private String actualAmount;

    /**
     * 完成时间
     */
    @ApiModelProperty("完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;



    /**
     * 订单状态 默认状态: 待支付
     */
    @ApiModelProperty("订单状态")
    private String orderStatus;





    /**
     * 完成时长
     */
    @ApiModelProperty("完成时长")
    private String completeDuration;

}
