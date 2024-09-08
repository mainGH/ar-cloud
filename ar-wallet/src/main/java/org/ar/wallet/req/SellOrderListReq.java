package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequestHome;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "卖出订单列表请求参数")
public class SellOrderListReq extends PageRequestHome {

    /**
     * 订单状态
     */
    @ApiModelProperty("订单状态，取值说明： 1:匹配中, 2: 匹配超时, 3: 待支付, 4: 确认中, 5: 确认超时, 6: 申诉中, 7: 已完成, 8: 已取消, 9: 订单失效, 11: 金额错误, 13: 支付超时, 14: 进行中, 15: 已完成")
    @Pattern(regexp = "^\\d+$", message = "Order status format is incorrect")
    private String orderStatus;

    /**
     * 查询时间 (格式: YYYY-MM-DD)
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Query time format is incorrect")
    @ApiModelProperty(value = "查询时间 (格式: YYYY-MM-DD)")
    private String date;
}
