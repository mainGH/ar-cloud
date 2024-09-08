package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequestHome;

import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "查看交易类型请求参数")
public class ViewTransactionHistoryReq extends PageRequestHome {

    /**
     * 交易类型
     */
    @ApiModelProperty(value = "交易类型, 取值说明: 1: 买入, 2: 卖出, 3: USDT买入, 9: 卖出奖励, 10: 退回, 11: 支付, 12: 到账, 13: 金额错误退回")
    @Pattern(regexp = "^\\d{1,2}$", message = "Transaction type format is incorrect")
    private String transactionType;

    /**
     * 查询时间 (格式: YYYY-MM-DD)
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Query time format is incorrect")
    @ApiModelProperty(value = "查询时间 (格式: YYYY-MM-DD)")
    private String date;
}
