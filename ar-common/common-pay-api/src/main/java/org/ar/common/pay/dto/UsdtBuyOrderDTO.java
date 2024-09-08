package org.ar.common.pay.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@ApiModel(description = "ustd买入订单返回")
public class UsdtBuyOrderDTO implements Serializable {

    private Long id;

    /**
     * 会员id
     */
    @ApiModelProperty("会员id")
    private String memberId;

    /**
     * 会员账号
     */
    @ApiModelProperty("会员账号")
    private String memberAccount;

    /**
     * 创建人
     */
    @ApiModelProperty("创建人")
    private String createBy;

    /**
     * 修改时间
     */
    @ApiModelProperty("修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @ApiModelProperty("修改人")
    private String updateBy;

    /**
     * 订单号
     */
    @ApiModelProperty("订单号")
    private String platformOrder;

    /**
     * USDT地址
     */
    @ApiModelProperty("USDT地址")
    private String usdtAddr;

    /**
     * USDT数量
     */
    @ApiModelProperty("USDT数量")
    private BigDecimal usdtNum;

    /**
     * ARB数量
     */
    @ApiModelProperty("ARB数量")
    private BigDecimal arbNum;

    /**
     * 订单状态 默认值: 待支付
     */
    @ApiModelProperty("3等待转账4确认中7已完成13订单过期10买入失败")
    private String status;

    /**
     * USDT支付凭证
     */
    @ApiModelProperty("USDT支付凭证")
    private String usdtProof;

    @ApiModelProperty("备注")
    private String remark;
}