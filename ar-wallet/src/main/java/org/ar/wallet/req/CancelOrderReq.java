package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "取消订单接口请求参数")
public class CancelOrderReq {

    /**
     * 订单号
     */
    @ApiModelProperty(value = "订单号")
    @NotBlank(message = "Order number cannot be empty")
    @Pattern(regexp = "^[A-Za-z0-9]{5}\\d{1,30}$", message = "Order number format is incorrect")
    private String platformOrder;

    /**
     * 取消原因
     */
    @ApiModelProperty(value = "取消原因")
    @Pattern(regexp = "^.{0,60}$", message = "Please fill in no more than 60 characters")
    private String reason;

    /**
     * 取消来源类型：不需要前端传, 1.C端 2.后台
     */
    private Integer sourceType;


    public Integer getSourceType() {
        if (sourceType == null) {
            // 默认为后台取消
            return 2;
        }
        return sourceType;
    }
}
