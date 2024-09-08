package org.ar.common.pay.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


import java.io.Serializable;
import java.math.BigDecimal;


/**
 * @author
 */
@Data
@ApiModel(description = "下发申请参数")
public class ApplyDistributedReq implements Serializable {


    /**
     * 商户
     */
    @ApiModelProperty("商户")
    private String merchantCode;


    /**
     * 下发usdt地址
     */
    @ApiModelProperty("下发usdt地址")
    private String usdtAddr;

    /**
     * 币种
     */
    @ApiModelProperty("币种")
    private String currence;

    /**
     * 总额度
     */
    @ApiModelProperty("总额度")
    private BigDecimal balance;
    @ApiModelProperty("下发金额")
    private BigDecimal amount;



    @ApiModelProperty("remark")
    private String remark;


}