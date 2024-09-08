package org.ar.pay.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author
 */
@Data
@ApiModel(description = "代收订单号")
public class CollectionOrderVo implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 币种
     */
    @ApiModelProperty(value = "币种")
    private String currency;

    /**
     * 支付方式
     */
    @ApiModelProperty(value = "支付方式")
    private String payType;

    /**
     * 商户订单号
     */
    @ApiModelProperty(value = "商户订单号")
    private String merchantOrder;

    /**
     * 平台订单号
     */
    @ApiModelProperty(value = "平台订单号")
    private String platformOrder;

    /**
     * 三方订单号
     */
    @ApiModelProperty(value = "三方订单号")
    private String thirdOrder;

    /**
     * 转账流水
     */
    @ApiModelProperty(value = "转账流水")
    private String transferStatement;

    /**
     * 订单金额
     */
    @ApiModelProperty(value = "订单金额")
    private BigDecimal amount;

    /**
     * 订单费率
     */
    @ApiModelProperty(value = "订单费率")
    private BigDecimal orderRate;

    /**
     * 汇率
     */
    @ApiModelProperty(value = "汇率")
    private BigDecimal exchangeRate;

    /**
     * 转换金额
     */
    @ApiModelProperty(value = "转换金额")
    private BigDecimal conversionAmount;

    /**
     * 手续费
     */
    @ApiModelProperty(value = "手续费")
    private BigDecimal commission;

    /**
     * 结算金额
     */
    @ApiModelProperty(value = "结算金额")
    private BigDecimal settlementAmount;

    /**
     * 收款金额
     */
    @ApiModelProperty(value = "收款金额")
    private BigDecimal collectedAmount;

    /**
     * 订单状态
     */
//    @ApiModelProperty(value = "订单状态")
//    private String orderStatus;


//            /**
//            * 回调状态
//            */
//            @ApiModelProperty(value="回调状态")
//            private String callbackStatus;


    /**
     * 国家
     */
    @ApiModelProperty(value = "国家")
    private String country;


    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号")
    private String merchantCode;

    /**
     * 商品
     */
    @ApiModelProperty(value = "商品")
    private String goodsName;

    /**
     * 三方编号
     */
    @ApiModelProperty(value = "三方编号")
    private String thirdCode;


    /**
     * 回调地址
     */
    @ApiModelProperty(value = "回调地址")
    private String notifyUrl;


    /**
     * 时间戳
     */
    @ApiModelProperty(value = "时间戳")
    private String timestamp;


    /**
     * 客户端ip
     */
    @ApiModelProperty(value = "客户端ip")
    private String clientIp;

    /**
     * sign
     */
    @ApiModelProperty(value = "md5签名值")
    private String sign;
}