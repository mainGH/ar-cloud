package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(description = "校验UPI是否重复-请求参数")
public class CheckUpiIdDuplicateReq {

    /**
     * UPI_ID
     */
    @ApiModelProperty(value = "UPI_ID 格式为: 本地用户名@银行handle")
    @NotBlank(message = "upiId cannot be empty")
    @Pattern(regexp = "^[A-Za-z0-9 !@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$", message = "upiId format is incorrect")
    private String upiId;
}
