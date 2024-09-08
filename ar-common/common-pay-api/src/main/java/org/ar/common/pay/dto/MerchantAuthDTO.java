package org.ar.common.pay.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "商户权限")
public class MerchantAuthDTO {

    /**
     * 用户ID
     */
    @ApiModelProperty("用户id")
    private Long userId;

    /**
     * 用户名
     */
    @ApiModelProperty("用户名")
    private String username;

    /**
     * 用户密码
     */
    @ApiModelProperty("用户密码")
    private String password;

    /**
     * 用户状态：1-有效；0-禁用
     */
    @ApiModelProperty("用户状态")
    private Integer status;

    /**
     * 用户角色编码集合 ["ROOT","ADMIN"]
     */
    @ApiModelProperty("角色")
    private List<String> roles;



}
