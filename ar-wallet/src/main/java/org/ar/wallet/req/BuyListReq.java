package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import java.math.BigDecimal;

@Data
@ApiModel(description = "买入列表请求参数")
public class BuyListReq {

    /**
     * 最小金额
     */
    @ApiModelProperty(value = "最小金额")
    @DecimalMin(value = "0.00", message = "Minimum amount format is incorrect")
    private BigDecimal minimumAmount;

    /**
     * 最大金额
     */
    @ApiModelProperty(value = "最大金额")
    @DecimalMin(value = "0.00", message = "Maximum amount format is incorrect")
    private BigDecimal maximumAmount;

    //查询页码
    @ApiModelProperty(value = "查询页码, 默认查询第一页")
    @Min(value = 0, message = "Page number format is incorrect")
    private Long pageNo = Long.valueOf(1);

    /**
     * 支付方式
     */
    @ApiModelProperty(value = "支付方式 取值说明: 1: 印度银行卡, 3: 印度UPI")
    private String  paymentType;

    /**
     * 会员id
     */
    @ApiModelProperty("会员id")
    private String memberId;

    /**
     * 会员最大限制金额
     */
    private BigDecimal memberMaxLimitAmount;

    /**
     * 会员最小限制金额
     */
    private BigDecimal memberMinLimitAmount;

    /**
     * 会员类型
     */
    @ApiModelProperty(value = "会员类型, 取值说明: 1: 内部商户会员, 2: 商户会员, 3: 钱包会员")
    private String memberType;
}
