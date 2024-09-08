package org.ar.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("merchant_info")
public class MerchantInfo extends BaseEntityOrder {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 账号
     */
    private String account;

    /**
     * 状态
     */
    private String status;

    /**
     * 商户编码
     */
    private String code;

    /**
     * 商户公钥
     */
    private String publicKey;

    /**
     * 商户私钥
     */
    private String privateKey;

    /**
     * 余额
     */
    private BigDecimal balance;

    /**
     * 国家
     */
    private String country;

    /**
     * 冻结金额
     */
    private BigDecimal frozenAmount;

    /**
     * 回调地址
     */
    private String notifyUrl;

    @TableField(exist = false)
    @Size(min = 1, message = "roleIds 不能为空")
    private List<Long> roleIds;

    @TableField(exist = false)
    private List<Long> menuIds;

    @TableField(exist = false)
    private List<String> permissions;

    @TableField(exist = false)
    private List<String> roles;

    private String deleted;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 登录账号
     */
    private String nickname;

    /**
     * 白名单
     */
    private String whiteList;

    /**
     * md5Key
     */
    private String md5Key;

    /**
     * 谷歌身份验证密钥
     */
    private String googlesecret;

    /**
     * 商家网址
     */
    private String website;

    /**
     * 登录次数
     */
    private Integer logins;

    /**
     * 支付费率
     */
    private String payRate;

    /**
     * 代付费率
     */
    private String transferRate;

    /**
     * 下发usdt地址
     */
    private String usdtAddress;

    /**
     * 币种
     */
    private String currency;

    /**
     * 登录IP
     */
    private String loginIp;


    /**
     * 总下发次数
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "总下发次数")
    private Integer transferCount;

    /**
     * 总下发金额
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "总下发金额")
    private BigDecimal transferAmount;

    /**
     * 总提现金额
     */
    @ApiModelProperty(value = "总提现金额(总代付金额统计)")
    private BigDecimal totalWithdrawAmount;

    /**
     * 总充值金额
     */
    @ApiModelProperty(value = "总充值金额(总代收金额统计)")
    private BigDecimal totalPayAmount;
}