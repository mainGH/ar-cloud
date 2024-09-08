package org.ar.common.pay.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@ApiModel(description = "代付订单请求对象")
@Validated
public class WithdrawOrderReq extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "商户订单号(三方订单号)")
    private String merchantOrder;

    @ApiModelProperty(value = "平台订单号")
    private String platformOrder;

    @ApiModelProperty(value = "订单状态 1-支付中 2-已完成 3-代付失败")
    private Integer orderStatus;

    @ApiModelProperty(value = "订单回调状态")
    private Integer callbackStatus;

    @ApiModelProperty(value = "时间类型, 1-下单时间 2-完成时间", required = true)
    private Integer timeType;

    @ApiModelProperty(value = "开始时间" , required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "商户code", required = false)
    private String merchantCode;

    @ApiModelProperty(value = "商户名称", required = false)
    private String merchantName;

    @ApiModelProperty(value = "完成开始时间" , required = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "完成结束时间",  required = false)
    private LocalDateTime completeEndTime;

    @ApiModelProperty(value = "语言", required = false)
    private String lang;


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