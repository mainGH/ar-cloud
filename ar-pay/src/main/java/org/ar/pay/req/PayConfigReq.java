package org.ar.pay.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "三方支付")
public class PayConfigReq extends PageRequest {


    /**
     * 三方支付名称
     */
    @ApiModelProperty(value = "三方支付名称")
    private String thirdName;

    /**
     * 三方支付编号
     */
    @ApiModelProperty(value = "三方支付编号")
    private String thirdCode;

    /**
     * 通道
     */
    @ApiModelProperty(value = "通道")
    private String channel;

    /**
     * 币种
     */
    @ApiModelProperty(value = "币种")
    private String currency;

    /**
     * 费率
     */
    @ApiModelProperty(value = "费率")
    private BigDecimal rate;

    /**
     * 汇率
     */
    @ApiModelProperty(value = "汇率")
    private BigDecimal exchangeRate;

    /**
     * 最大充值金额
     */
    @ApiModelProperty(value = "最大充值金额")
    private BigDecimal maximumAmount;

    /**
     * 最小充值金额
     */
    @ApiModelProperty(value = "最小充值金额")
    private BigDecimal minimumAmount;

    /**
     * 充值成功率
     */
    @ApiModelProperty(value = "充值成功率")
    private BigDecimal successRate;

    /**
     * 加密算法
     */
    @ApiModelProperty(value = "加密算法")
    private String encryptionAlgorithm;

    /**
     * 私钥
     */
    @ApiModelProperty(value = "私钥")
    private String privateKey;

    /**
     * 公钥
     */
    @ApiModelProperty(value = "公钥")
    private String publicKey;

    /**
     * 适用国家地区
     */
    @ApiModelProperty(value = "适用国家地区")
    private String country;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @ApiModelProperty(value = "修改时间")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /**
     * 修改人
     */
    @ApiModelProperty(value = "修改人")
    private String updateBy;

    /**
     * 状态
     */
    @ApiModelProperty(value = "创建时间")
    private String status;

    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号")
    private String merchantCode;

    /**
     * md5签名值
     */
    @ApiModelProperty(value = "md5签名值")
    private String sign;

    /**
     * md5Key
     */
    @ApiModelProperty(value = "md5Key")
    private String md5Key;

    /**
     * md5Key
     */
    @ApiModelProperty(value = "md5Key")
    private String payType;

}
