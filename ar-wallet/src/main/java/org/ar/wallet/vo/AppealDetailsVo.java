package org.ar.wallet.vo;

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
@ApiModel(description = "申诉详情")
public class AppealDetailsVo implements Serializable {


    /**
     * 订单金额
     */
    @ApiModelProperty(value = "订单金额")
    private BigDecimal amount;

    /**
     * 订单时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "订单时间")
    private LocalDateTime createTime;

    /**
     * 申诉时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "申诉时间")
    private LocalDateTime appealTime;

    /**
     * UTR
     */
    @ApiModelProperty("UTR")
    private String utr;


    /**
     * 订单号
     */
    @ApiModelProperty(value = "订单号")
    private String platformOrder;

    /**
     * 支付时间
     */
    @ApiModelProperty(value = "支付时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;

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
     * 申诉原因
     */
    @ApiModelProperty(value = "申诉原因")
    private String reason;

    /**
     * 申诉图片
     */
    @ApiModelProperty(value = "申诉图片 多张图片以逗号, 分割")
    private String picInfo;

    /**
     * 申诉视频
     */
    @ApiModelProperty(value = "申诉视频")
    private String videoUrl;

}