package org.ar.wallet.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author
 */
@Data
@ApiModel(description = "创建会员请求参数")
public class MemberInfoReq {

    /**
     * 会员账号
     */
    @NotBlank(message = "Account cannot be empty")
    @ApiModelProperty(value = "会员账号")
    private String memberAccount;

    /**
     * 登录密码
     */
    @NotBlank(message = "password can not be blank")
    @ApiModelProperty(value = "登录密码")
    private String password;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

}