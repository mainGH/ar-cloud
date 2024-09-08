package org.ar.common.pay.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author
 */
@Data
@ApiModel(description = "获取kyc列表返回数据")
public class KycPartnersDTO implements Serializable {

    @ApiModelProperty("id")
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
     * 银行登录令牌
     */
    @ApiModelProperty("银行登录令牌")
    private String token;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String mobileNumber;

    /**
     * 银行编码
     */
    @ApiModelProperty("银行编码")
    private String bankCode;

    /**
     * 银行名称
     */
    @ApiModelProperty("银行名称")
    private String bankName;

    /**
     * upi_id
     */
    @ApiModelProperty("upiId")
    private String upiId;

    /**
     * 账户姓名
     */
    @ApiModelProperty("账户姓名")
    private String name;

    /**
     * 账户
     */
    @ApiModelProperty("账户")
    private String account;

    /**
     * 连接状态: 0: 未连接, 1: 已连接
     */
    @ApiModelProperty("连接状态: 0: 未连接, 1: 已连接")
    private Integer linkStatus;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;



    /**
     * 卖出状态: 0: 关闭, 1: 开启
     */
    @ApiModelProperty("卖出状态: 0: 关闭, 1: 开启")
    private Integer sellStatus;


    /**
     * 图标地址
     */
    @ApiModelProperty("图标地址")
    private String iconUrl;
}