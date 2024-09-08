package org.ar.pay.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author
 */
@Data
@ApiModel(description = "账变记录列表返回参数")
public class AccountChangeListVo implements Serializable {


    /**
     * 订单时间
     */
    @ApiModelProperty(value = "订单时间")
    private Date createTime;

    /**
     * 商户订单号
     */
    @ApiModelProperty(value = "商户订单号")
    private String orderNo;

    /**
     * 账变类型
     */
    @ApiModelProperty(value = "账变类型")
    private String type;

    /**
     * 币种
     */
    @ApiModelProperty(value = "币种")
    private String currentcy;

    /**
     * 账变前
     */
    @ApiModelProperty(value = "账变前额度")
    private BigDecimal beforeChange;

    /**
     * 变化金额
     */
    @ApiModelProperty(value = "账变金额")
    private BigDecimal amountChange;

    /**
     * 账变后金额
     */
    @ApiModelProperty(value = "账变后额度")
    private BigDecimal afterChange;
}