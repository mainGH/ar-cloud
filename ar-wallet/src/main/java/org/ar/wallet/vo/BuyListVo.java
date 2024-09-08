package org.ar.wallet.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.wallet.Enum.PayTypeEnum;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author
 */
@Data
@ApiModel(description = "买入列表返回数据")
public class BuyListVo implements Serializable {

    /**
     * 订单号
     */
    @ApiModelProperty(value = "订单号")
    private String platformOrder;

    /**
     * 订单金额
     */
    @ApiModelProperty(value = "订单金额")
    private BigDecimal amount;

    /**
     * 最小限额
     */
    @ApiModelProperty(value = "最小限额")
    private BigDecimal minimumAmount;

    /**
     * 最大限额
     */
    @ApiModelProperty(value = "最大限额")
    private BigDecimal maximumAmount;

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "用户头像")
    private Integer avatar;

    /**
     * 支付方式 默认值: UPI
     */
    @ApiModelProperty(value = "支付方式")
    private String payType = PayTypeEnum.INDIAN_UPI.getCode();

    /**
     * 会员Id
     */
    @ApiModelProperty(value = "会员Id")
    private String memberId;

    /**
     * 信用分
     */
    @ApiModelProperty("信用分")
    private BigDecimal creditScore;

    /**
     * 会员类型
     */
    @ApiModelProperty(value = "会员类型, 取值说明: 1: 内部商户会员, 2: 商户会员, 3: 钱包会员")
    private String memberType;


    public BigDecimal getCreditScore() {
        if (creditScore == null) {
            return BigDecimal.ZERO;
        }
        return creditScore;
    }
}