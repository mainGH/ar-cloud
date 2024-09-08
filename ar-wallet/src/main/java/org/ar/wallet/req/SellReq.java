package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@ApiModel(description = "卖出下单请求参数")
public class SellReq {


    /**
     * 收款信息ID
     */
    @ApiModelProperty(value = "收款信息ID")
    @NotNull(message = "Payment information ID cannot be empty")
    @Min(value = 1, message = "The payment information ID format is incorrect.")
    private Long collectionInfoId;

    /**
     * 卖出数量
     */
    @ApiModelProperty(value = "卖出数量")
    @NotNull(message = "The selling quantity cannot be empty")
    @DecimalMin(value = "0.00", message = "Sell quantity format is incorrect")
    private BigDecimal amount;

    /**
     * 最小限额
     */
    @DecimalMin(value = "0.00", message = "Minimum limit format is incorrect")
    @ApiModelProperty(value = "最小限额")
    private BigDecimal minimumAmount;
}
