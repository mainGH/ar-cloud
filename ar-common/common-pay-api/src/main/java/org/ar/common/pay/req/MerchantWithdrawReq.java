package org.ar.common.pay.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

import java.math.BigDecimal;


/**
 * @author Admin
 */
@Data
@ApiModel(description = "商户手动下发实体类")
public class MerchantWithdrawReq extends PageRequest {
    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号", required = true)
    private String merchantCode;

    /**
     * 订单号
     */
    @ApiModelProperty(value = "金额", required = true)
    private BigDecimal amount;

    /**
     * 账变类型
     */
    @ApiModelProperty(value = "币种", required = true)
    private String currency;

    /**
     * 开始时间
     */
    @ApiModelProperty(value = "备注", required = false)
    private String remark;


}
