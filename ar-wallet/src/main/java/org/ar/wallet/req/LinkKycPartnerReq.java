package org.ar.wallet.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;

/**
 * 连接 KYC Partner 请求参数
 *
 * @author Simon
 * @date 2023/12/26
 */
@Data
@ApiModel(description = "连接 KYC Partner 请求参数")
public class LinkKycPartnerReq implements Serializable {

    private static final long serialVersionUID = 1L; // 显式序列化版本ID


    /**
     * id
     */
    @NotNull(message = "KYC id cannot be null")
    @Positive(message = "KYC id must be greater than 0")
    @ApiModelProperty(value = "KYC id")
    private Long id;


    /**
     * token
     */
    @NotBlank(message = "token cannot be empty")
    @ApiModelProperty("token")
    private String token;

}
