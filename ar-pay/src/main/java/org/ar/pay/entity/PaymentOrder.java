package org.ar.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("payment_order")
public class PaymentOrder extends BaseEntityOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 币种
     */
    private String currentcy;

    /**
     * 支付方式
     */
    private String payType;

    /**
     * 商户订单
     */
    private String merchantOrder;

    /**
     * 平台订单
     */
    private String platformOrder;

    /**
     * 三方订单
     */
    private String thirdOrder;

    /**
     * 账号
     */
    private String accountNumber;

    /**
     * 账号名称
     */
    private String accountName;

    /**
     * 账户金额
     */
    private BigDecimal accountAmount;

    /**
     * 订单费率
     */
    private BigDecimal orderRate;

    /**
     * 汇率
     */
    private BigDecimal exchangeRate;

    /**
     * 转换金额
     */
    private BigDecimal conversionAmount;

    /**
     * 手续费
     */
    private BigDecimal commission;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 回调订单状态
     */
    private String callbackStatus;

    /**
     * 三方代码
     */
    private String thirdCode;

    /**
     * 回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime callbackTime;

    /**
     * 费用
     */
    private String cost;

    /**
     * 创建时间
     */
//    private LocalDateTime createTime;

    /**
     * 修改时间
     */
//    private LocalDateTime updateTime;

    /**
     * 创建人
     */
//    private String createBy;

    /**
     * 修改人
     */
//    private String updateBy;


    private String country;

    private String merchantCode;

    private String goodsName;


    /**
     * 总代付金额统计
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "总付统计")
    private String allTransferAmount;


}