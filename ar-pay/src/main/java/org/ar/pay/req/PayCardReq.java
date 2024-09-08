package org.ar.pay.req;



import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.ar.common.core.page.PageRequest;

@ApiModel(description = "银行卡")
public class PayCardReq extends PageRequest {

    /**
     * 绑定标识
     */
    @ApiModelProperty(value = "绑定标识")
    private String mark;

    /**
     * 支付渠道
     */
    @ApiModelProperty(value = "支付渠道")
    private String channel;

    /**
     * 银行名称
     */
    @ApiModelProperty(value = "银行名称")
    private String bankName;

    /**
     * 银行别名
     */
    @ApiModelProperty(value = "银行别名")
    private String bankAlias;

    /**
     * 姓名
     */
    @ApiModelProperty(value = "姓名")
    private String name;

    /**
     * 特殊编码
     */
    @ApiModelProperty(value = "特殊编码")
    private String code;

    /**
     * 银行卡号
     */
    @ApiModelProperty(value = "银行卡号")
    private String cardNo;

    /**
     * 白名单
     */
    @ApiModelProperty(value = "白名单")
    private String whiteList;

    /**
     * 代收单笔最小金额
     */
    @ApiModelProperty(value = "代收单笔最小金额")
    private BigDecimal minCollection;

    /**
     * 代收单笔最大金额
     */
    @ApiModelProperty(value = "代收单笔最大金额")
    private BigDecimal maxCollection;

    /**
     * 代收日限额
     */
    @ApiModelProperty(value = "代收日限额")
    private BigDecimal dayCollection;

    /**
     * 日限制笔数
     */
    @ApiModelProperty(value = "日限制笔数")
    private Integer quantityCllection;

    /**
     * 代收累计限额
     */
    @ApiModelProperty(value = "代收累计限额")
    private BigDecimal allCllection;

    /**
     * 代付类型
     */
    @ApiModelProperty(value = "代付类型")
    private String paymentType;

    /**
     * 代付最小金额
     */
    @ApiModelProperty(value = "代付最小金额")
    private BigDecimal minPayment;

    /**
     * 代付最大金额
     */
    @ApiModelProperty(value = "代付最大金额")
    private BigDecimal maxPayment;

    /**
     * 代付日限额
     */
    @ApiModelProperty(value = "代付日限额")
    private BigDecimal dayPayment;

    /**
     * 代付日限制笔数
     */
    @ApiModelProperty(value = "代付日限制笔数")
    private String quantityPayment;

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
    private LocalDateTime updateBy;

}
