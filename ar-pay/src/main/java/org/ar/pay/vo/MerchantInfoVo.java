package org.ar.pay.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author
 */
@Data
@ApiModel(description = "商户信息")
public class MerchantInfoVo implements Serializable {


    /**
     * 商户名
     */
    @ApiModelProperty(value = "商户名")
    private String username;

    /**
     * 账号
     */
    @ApiModelProperty(value = "商户账号")
    private String account;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private String status;

    /**
     * 商户编码
     */
    @ApiModelProperty(value = "商户编码")
    private String code;

    /**
     * 商户公钥
     */
    @ApiModelProperty(value = "商户公钥")
    private String publicKey;

    /**
     * 商户私钥
     */
    @ApiModelProperty(value = "商户私钥")
    private String privateKey;

    /**
     * 余额
     */
    @ApiModelProperty(value = "余额")
    private BigDecimal balance;

    /**
     * 国家
     */
    @ApiModelProperty(value = "国家")
    private String country;

    /**
     * 冻结金额
     */
    @ApiModelProperty(value = "冻结金额")
    private BigDecimal frozenAmount;

    /**
     * 回调地址
     */
    @ApiModelProperty(value = "回调地址")
    private String notifyUrl;

//    @Size(min = 1, message = "roleIds 不能为空")
//    @ApiModelProperty(value = "商户角色Id")
//    private List<Long> roleIds;

//    @TableField(exist = false)
//    @ApiModelProperty(value = "商户菜单Id")
//    private List<Long> menuIds;

//    @TableField(exist = false)
//    @ApiModelProperty(value = "商户权限")
//    private List<String> permissions;

    @TableField(exist = false)
//    @ApiModelProperty(value = "商户角色")
//    private List<String> roles;

    @ApiModelProperty(value = "删除状态")
    private String deleted;

    @ApiModelProperty(value = "移动电话")
    private String mobile;

    @ApiModelProperty(value = "电子邮件")
    private String email;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "白名单")
    private String whiteList;

    /**
     * 谷歌身份验证密钥
     */
    private String googlesecret;

    /**
     * 商家网址
     */
    @ApiModelProperty(value = "商家网址")
    private String website;

    /**
     * 登录次数
     */
    @ApiModelProperty(value = "登录次数")
    private String logins;

    /**
     * 支付费率
     */
    @ApiModelProperty(value = "代收费率")
    private String payRate;

    /**
     * 代付费率
     */
    @ApiModelProperty(value = "代付费率")
    private String transferRate;

    /**
     * 下发usdt地址
     */
    @ApiModelProperty(value = "下发usdt地址")
    private String usdtAddress;

    /**
     * 总代收金额统计
     */
    @ApiModelProperty(value = "总代收金额统计")
    private String allCollectionAmount = String.valueOf(0);

    /**
     * 总代付金额统计
     */
    @ApiModelProperty(value = "总代付金额统计")
    private String allTransferAmount = String.valueOf(0);

    /**
     * 总下发次数
     */
    @ApiModelProperty(value = "总下发次数")
    private String transferCount = String.valueOf(0);

    /**
     * 总下发金额
     */
    @ApiModelProperty(value = "总下发金额")
    private String transferAmount = String.valueOf(0);

    /**
     * 开通时间
     */
    @ApiModelProperty(value = "开通时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * md5Key
     */
    @ApiModelProperty(value = "md5Key")
    private String md5Key;

    /**
     * 币种
     */
    @ApiModelProperty(value = "币种")
    private String currency;

}