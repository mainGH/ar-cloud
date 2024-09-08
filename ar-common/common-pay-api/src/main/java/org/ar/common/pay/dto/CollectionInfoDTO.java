package org.ar.common.pay.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款信息
 *
 * @author
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(description = "收款信息返回")
public class CollectionInfoDTO  implements Serializable {


    @ApiModelProperty("主键")
    private Long id;

    /**
     * UPI_ID
     */
    @ApiModelProperty("upiId")
    private String upiId;

    /**
     * UPI_Name
     */
    @ApiModelProperty("UPI_Name")
    private String upiName;

    /**
     * 会员id
     */
    @ApiModelProperty("会员id")
    private String memberId;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String mobileNumber;

    /**
     * 日限额
     */
    @ApiModelProperty("日限额")
    private BigDecimal dailyLimitAmount;

    /**
     * 日限笔数
     */
    @ApiModelProperty("日限笔数")
    private Integer dailyLimitNumber;

    /**
     * 最小金额
     */
    @ApiModelProperty("最小金额")
    private BigDecimal minimumAmount;

    /**
     * 最大金额
     */
    @ApiModelProperty("最大金额")
    private BigDecimal maximumAmount;

    /**
     * 已收款金额
     */
    @ApiModelProperty("已收款金额")
    private BigDecimal collectedAmount;

    /**
     * 已收款次数
     */
    @ApiModelProperty("已收款次数")
    private Integer collectedNumber;

    @ApiModelProperty("会员账号")
    private String memberAccount;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 收款状态 默认值: 正常
     */
    @ApiModelProperty("收款状态: 1-正常,0-关闭")
    private String collectedStatus;
}