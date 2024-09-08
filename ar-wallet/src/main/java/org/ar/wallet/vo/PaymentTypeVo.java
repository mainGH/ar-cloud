package org.ar.wallet.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.wallet.Enum.PayTypeEnum;

import java.io.Serializable;

/**
 * @author
 */
@Data
@ApiModel(description = "获取支付类型接口返回数据")
public class PaymentTypeVo implements Serializable {

    /**
     * 支付方式 默认值: UPI
     */
    @ApiModelProperty(value = "支付方式 取值说明: 1: 印度银行卡, 3: 印度UPI")
    private String  payType = PayTypeEnum.INDIAN_UPI.getCode();

    /**
     * 支付方式 默认值: UPI
     */
    @ApiModelProperty(value = "支付方式 名称说明")
    private String  name = "UPI";
}