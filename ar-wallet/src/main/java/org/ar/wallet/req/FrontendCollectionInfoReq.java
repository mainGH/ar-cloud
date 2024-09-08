package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "添加收款信息请求参数")
public class FrontendCollectionInfoReq {

    /**
     * UPI_ID
     */
    @ApiModelProperty(value = "UPI_ID 格式为: 本地用户名@银行handle")
    @NotBlank(message = "upiId cannot be empty")
    @Pattern(regexp = "^[A-Za-z0-9 !@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$", message = "upiId format is incorrect")
    private String upiId;

    /**
     * UPI_Name
     */
    @ApiModelProperty(value = "UPI_Name 格式为: 纯用户名")
    @NotBlank(message = "upiName cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9]+(?:[\\s._][a-zA-Z0-9]+)*$", message = "upiName format is incorrect")
    private String upiName;


    /**
     * 手机号码
     */
//    @NotNull(message = "手机号码不能为空")
//    @Pattern(regexp = "^\\d{8,13}$", message = "手机号码格式不正确")
//    @ApiModelProperty(value = "手机号码 格式为印度手机号码格式 示例: 7528988319")
//    private String mobileNumber;


    /**
     * 验证码
     */
    @NotNull(message = "verification code must be filled")
    @ApiModelProperty(value = "验证码 (格式为6位随机数 示例: 123456)")
    @Pattern(regexp = "\\d{6}", message = "Verification code error")
    private String verificationCode;


//    /**
//     * 最小收款限额
//     */
//    @DecimalMin(value = "0.00", message = "最小收款限额格式不正确")
//    @ApiModelProperty(value = "最小收款限额")
//    private BigDecimal minimumAmount;
//
//    /**
//     * 最大收款限额
//     */
//    @DecimalMin(value = "0.00", message = "最大收款限额格式不正确")
//    @ApiModelProperty(value = "最大收款限额")
//    private BigDecimal maximumAmount;
//
//    /**
//     * 每日收款限额
//     */
//    @ApiModelProperty(value = "每日收款限额")
//    @DecimalMin(value = "0.00", message = "每日收款限额格式不正确")
//    private BigDecimal dailyLimitAmount;
//
//    /**
//     * 每日收款次数
//     */
//    @ApiModelProperty(value = "每日收款笔数")
//    @Min(value = 0, message = "每日收款次数格式不正确")
//    private Integer dailyLimitCount;
}
