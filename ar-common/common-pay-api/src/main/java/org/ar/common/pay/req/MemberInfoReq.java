package org.ar.common.pay.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @author
 */
@Data
@ApiModel(description = "创建会员请求参数")
public class MemberInfoReq implements Serializable {

    /**
     * 会员账号
     */
    @NotBlank(message = "账号不能为空")
    @ApiModelProperty(value = "会员账号")
    private String memberAccount;

    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    @ApiModelProperty(value = "登录密码")
    private String password;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

}