package org.ar.common.pay.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author admin
 * @date 2024/3/9 15:37
 */
@Data
@ApiModel(description = "订单概览")
public class OrderOverviewDTO {

    @ApiModelProperty(value = "待处理订单")
    private Long pendingOrdersNum;

//    @ApiModelProperty(value = "金额错误订单")
//    private Long amountErrorNum;

    @ApiModelProperty(value = "买入申诉订单")
    private Long payAppealNum;

    @ApiModelProperty(value = "卖出申诉订单")
    private Long withdrawAppealNum;

    @ApiModelProperty(value = "进行中的订单")
    private Long beProcessedOrderNum;

    @ApiModelProperty(value = "匹配中")
    private Long matchingOrderNum;

    @ApiModelProperty(value = "待支付")
    private Long waitForPaymentOrderNum;

    @ApiModelProperty(value = "确认中")
    private Long waitForConfirmOrderNum;
}
