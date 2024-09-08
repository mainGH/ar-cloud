package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

@Data
@ApiModel(description = "USDT买入下单请求参数")
public class UsdtBuyReq {

    /**
     * 主网络
     */
    @ApiModelProperty(value = "主网络")
    @NotBlank(message = "USDT main network cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9-]{1,20}$", message = "USDT main network format is incorrect")
    private String networkProtocol;

    /**
     * USDT数量
     */
    @ApiModelProperty(value = "USDT数量")
    @NotNull(message = "USDT quantity cannot be empty")
    @DecimalMin(value = "0.00", message = "USDT quantity format is incorrect")
    private BigDecimal usdtAmount;

    /**
     * ARB数量
     */
    @ApiModelProperty(value = "ARB数量")
    @NotNull(message = "ARB quantity cannot be empty")
    @DecimalMin(value = "0.00", message = "ARB quantity format is incorrect")
    private BigDecimal arbAmount;
}
