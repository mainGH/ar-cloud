package org.ar.wallet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@ApiModel(description = "交易记录-列表")
public class ViewTransactionHistoryVo implements Serializable {


    /**
     * 交易类型
     */
    @ApiModelProperty(value = "交易类型, 取值说明: 1: 买入, 2: 卖出, 3: USDT买入, 9: 卖出奖励, 10: 退回, 11: 支付, 12: 到账, 13: 金额错误退回")
    private String transactionType;

    /**
     * 时间
     */
    @ApiModelProperty("时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 金额
     */
    @ApiModelProperty("金额")
    private BigDecimal amount;

}