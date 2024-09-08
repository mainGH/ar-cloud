package org.ar.pay.entity;

    import java.math.BigDecimal;

    import com.baomidou.mybatisplus.annotation.IdType;
    import com.baomidou.mybatisplus.annotation.TableId;
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
    @TableName("pay_config")
    public class PayConfig extends BaseEntityOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;


    /**
            * 三方支付名称
            */
    private String thirdName;

            /**
            * 三方支付编号
            */
    private String thirdCode;

            /**
            * 通道
            */
    private String channel;

            /**
            * 币种
            */
    private String currency;

            /**
            * 费率
            */
    private BigDecimal rate;

            /**
            * 汇率
            */
    private BigDecimal exchangeRate;

            /**
            * 最大充值金额
            */
    private BigDecimal maximumAmount;

            /**
            * 最小充值金额
            */
    private BigDecimal minimumAmount;

            /**
            * 充值成功率
            */
    private BigDecimal successRate;

            /**
            * 加密算法
            */
    private String encryptionAlgorithm;

            /**
            * 私钥
            */
    private String privateKey;

            /**
            * 公钥
            */
    private String publicKey;

            /**
            * 适用国家地区
            */
    private String country;

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

    private String httpMethod;


    private String contentType;

    private String payUrl;

    private String serviceName;

    private String status;

    /**
     * md5Key
     */
    private String md5Key;

    private String channelName;

    private String payType;

    private String channelType;

    private String bankProvid;


}