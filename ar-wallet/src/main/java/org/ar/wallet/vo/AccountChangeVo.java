package org.ar.wallet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@ApiModel(description = "账变列表返回")
public class AccountChangeVo implements Serializable {



    /**
     * 商户号
     */
    @ApiModelProperty("商户号")
    private String merchantCode;


    /**
     * 商户名
     */
    @ApiModelProperty("商户名")
    private String merchantName;


    /**
     * 商户订单
     */
    @ApiModelProperty("商户订单")
    private String merchantOrder;






    /**
     * 手续费
     */
    @ApiModelProperty("手续费")
    private BigDecimal commission;




    /**
     * 商户订单号
     */
    @ApiModelProperty("商户订单号")
    private String orderNo;

    /**
     * 账变前
     */
    @ApiModelProperty("账变前")
    private BigDecimal beforeChange;

    /**
     * 变化金额
     */
    @ApiModelProperty("变化金额")
    private BigDecimal amountChange;


    /**
     * 账变后金额
     */
    @ApiModelProperty("账变后金额")
    private BigDecimal afterChange;

    /**
     * 创建时间
     */
    @ApiModelProperty("订单时间")
    private LocalDateTime createTime;


    /**
     * 账变类型: 1-代收, 2-代付, 3-下发,4-上分
     */
    @ApiModelProperty("账变类型")
    private Integer changeType;


    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;




}