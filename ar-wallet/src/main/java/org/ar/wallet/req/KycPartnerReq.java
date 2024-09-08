package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 添加 KYC Partner 请求参数
 *
 * @author Simon
 * @date 2023/12/26
 */
@Data
@ApiModel(description = "添加 KYC Partner 请求参数")
public class KycPartnerReq implements Serializable {

    private static final long serialVersionUID = 1L; // 显式序列化版本ID


    /**
     * 银行编码
     */
    @NotBlank(message = "bankCode cannot be empty")
    @ApiModelProperty(value = "银行编码")
    private String bankCode;


    /**
     * 账户姓名
     */
    @NotBlank(message = "name cannot be empty")
    @ApiModelProperty(value = "姓名")
    private String name;


    /**
     * 账户
     */
    @NotBlank(message = "account cannot be empty")
    @ApiModelProperty(value = "账户")
    private String account;


    /**
     * upi_id
     */
    @NotBlank(message = "upiId cannot be empty")
    @ApiModelProperty(value = "upiId")
    private String upiId;
}
