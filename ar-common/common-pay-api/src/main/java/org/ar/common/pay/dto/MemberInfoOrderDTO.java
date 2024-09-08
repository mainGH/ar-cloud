package org.ar.common.pay.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员信息表
 *
 * @author
 */
@Data
@ApiModel(description = "会员列表返回")
public class MemberInfoOrderDTO implements Serializable {

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("买入订单或者卖出订单时间")
    private LocalDateTime createTime;

    @ApiModelProperty("买入订单或者卖出订单时间")
    private BigDecimal amount;

    @ApiModelProperty("买入或者卖出订单状态")
    private String  status;

    @ApiModelProperty("交易对象ID")
    private String matchMemberId;

    /**
     * 平台订单号
     */
    @ApiModelProperty("平台订单号")
    private String platformOrder;


}