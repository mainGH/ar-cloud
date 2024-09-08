package org.ar.pay.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* 
*
* @author 
*/
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @TableName("collection_order_test")
    public class CollectionOrderTest implements Serializable {
    private static final long serialVersionUID = 1L;


            /**
            * 币种
            */
    private String currency;

            /**
            * 支付方式
            */
    private String payType;

            /**
            * 商户订单号
            */
    private String merchantOrder;

            /**
            * 平台订单号
            */
    private String platformOrder;

            /**
            * 三方订单号
            */
    private String thirdOrder;

            /**
            * 转账流水
            */
    private String transferStatement;

            /**
            * 订单金额
            */
    private BigDecimal amount;

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
            * 收款金额
            */
    private String collectedAmount;

            /**
            * 订单状态
            */
    private String orderStatus;

            /**
            * 回调状态
            */
    private String callbackStatus;

            /**
            * 创建时间
            */
    private LocalDateTime createTime;

            /**
            * 修改时间
            */
    private LocalDateTime updateTime;

            /**
            * 创建人
            */
    private String createBy;

            /**
            * 修改人
            */
    private LocalDateTime updateBy;


    private String country;

    private String merchantCode;

    private String goodsName;

    private String thirdCode;




}