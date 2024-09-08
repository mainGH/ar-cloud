package org.ar.pay.entity;

    import java.math.BigDecimal;
    import com.baomidou.mybatisplus.annotation.TableName;
    import java.time.LocalDateTime;
    import java.io.Serializable;
    import lombok.Data;
    import lombok.EqualsAndHashCode;
    import lombok.experimental.Accessors;

/**
* 
*
* @author 
*/
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @TableName("pay_card")
    public class PayCard implements Serializable {



            /**
            * 绑定标识
            */
    private String mark;

            /**
            * 支付渠道
            */
    private String channel;

            /**
            * 银行名称
            */
    private String bankName;

            /**
            * 银行别名
            */
    private String bankAlias;

            /**
            * 姓名
            */
    private String name;

            /**
            * 特殊编码
            */
    private String code;

            /**
            * 银行卡号
            */
    private String cardNo;

            /**
            * 白名单
            */
    private String whiteList;

            /**
            * 代收单笔最小金额
            */
    private BigDecimal minCollection;

            /**
            * 代收单笔最大金额
            */
    private BigDecimal maxCollection;

            /**
            * 代收日限额
            */
    private BigDecimal dayCollection;

            /**
            * 日限制笔数
            */
    private Integer quantityCllection;

            /**
            * 代收累计限额
            */
    private BigDecimal allCllection;

            /**
            * 代付类型
            */
    private String paymentType;

            /**
            * 代付最小金额
            */
    private BigDecimal minPayment;

            /**
            * 代付最大金额
            */
    private BigDecimal maxPayment;

            /**
            * 代付日限额
            */
    private BigDecimal dayPayment;

            /**
            * 代付日限制笔数
            */
    private String quantityPayment;

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


}