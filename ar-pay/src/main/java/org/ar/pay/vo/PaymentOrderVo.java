package org.ar.pay.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@ApiModel(description = "代付订单")
public class PaymentOrderVo implements Serializable {


    /**
     * 币种
     */
    @ApiModelProperty(value="币种")
    private String currentcy;

    /**
     * 支付方式
     */
    @ApiModelProperty(value="支付方式")
    private String payType;

    /**
     * 商户订单
     */
    @ApiModelProperty(value="商户订单")
    private String merchantOrder;

    /**
     * 平台订单
     */
    @ApiModelProperty(value="平台订单")
    private String platformOrder;

    /**
     * 三方订单
     */
    @ApiModelProperty(value="三方订单")
    private String thirdOrder;

    /**
     * 账号
     */
    @ApiModelProperty(value="账号")
    private String accountNumber;

    /**
     * 账号名称
     */
    @ApiModelProperty(value="账号名称")
    private String accountName;

    /**
     * 账户金额
     */
    @ApiModelProperty(value="账户金额")
    private BigDecimal accountAmount;

    /**
     * 订单费率
     */
    @ApiModelProperty(value="订单费率")
    private BigDecimal orderRate;

    /**
     * 汇率
     */
    @ApiModelProperty(value="汇率")
    private BigDecimal exchangeRate;

    /**
     * 转换金额
     */
    @ApiModelProperty(value="转换金额")
    private BigDecimal conversionAmount;

    /**
     * 手续费
     */
    @ApiModelProperty(value="手续费")
    private BigDecimal commission;

    /**
     * 结算金额
     */
    @ApiModelProperty(value="结算金额")
    private BigDecimal settlementAmount;

    /**
     * 订单状态
     */
    @ApiModelProperty(value="订单状态")
    private String orderStatus;

    /**
     * 回调订单状态
     */
    @ApiModelProperty(value="回调订单状态")
    private String callbackStatus;



    @ApiModelProperty(value="国家")
    private String country;
    @ApiModelProperty(value="商户号")
    private String merchantCode;
    @ApiModelProperty(value="商品名称")
    private String goodsName;

    private String currency;

    private String thirdCode;

    @TableField(exist = false)
    private String sign;


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
     * 订单金额
     */
    @ApiModelProperty(value = "订单金额")
    private BigDecimal amount;


    @ApiModelProperty(value = "bankCode")
    private String bankCode;




}