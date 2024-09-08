package org.ar.common.pay.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员信息表
 *
 * @author
 */
@Data
@ApiModel(description = "会员列表返回")
public class MemberInfolistPageDTO  implements Serializable {

    @ApiModelProperty("会员id")
    private Long id;

    /**
     * 商户会员ID
     */
    @ApiModelProperty("商户会员ID")
    private String memberId;

    /**
     * 会员账号
     */
    @ApiModelProperty("会员账号")
    private String memberAccount;



    /**
     * 真实姓名
     */
    @ApiModelProperty("真实姓名")
    private String realName;

    /**
     * 手机号
     */
    @ApiModelProperty("手机号")
    private String mobileNumber;



    /**
     * 会员类型
     */

    @ApiModelProperty("会员类型 1内部会员2商户会员3钱包会员")
    private String memberType;

    /**
     * 余额
     */
    @ApiModelProperty("余额")
    private BigDecimal balance;

//    /**
//     * 冻结金额
//     */
//    private BigDecimal frozenAmount;

    /**
     * 累计买入次数
     */
    @ApiModelProperty("累计买入次数")
    private Integer totalBuyCount;

    /**
     * 累计卖出次数
     */
    @ApiModelProperty("累计卖出次数")
    private Integer totalSellCount;

    /**
     * 累计买入金额
     */

    @ApiModelProperty("累计买入金额")
    private BigDecimal totalBuyAmount;

    /**
     * 累计卖出金额
     */
    @ApiModelProperty("累计卖出金额")
    private BigDecimal totalSellAmount;

    /**
     * 累计买入成功次数
     */
    @ApiModelProperty("累计买入成功次数")
    private Integer totalBuySuccessCount;

    /**
     * 累计卖出成功次数
     */
    @ApiModelProperty("累计卖出成功次数")
    private Integer totalSellSuccessCount;

    /**
     * 累计买入奖励
     */
    private BigDecimal totalBuyBonus;

    /**
     * 累计卖出奖励
     */
    private BigDecimal totalSellBonus;

    /**
     * 状态 默认值 启用
     */
    @ApiModelProperty("状态 0禁止 1开启")
    private String status;

    /**
     * 在线状态 默认值 离线
     */
    @ApiModelProperty("在线 0离线 1 在线")
    private String onlineStatus;

    /**
     * 买入状态 默认值 开启
     */
    @ApiModelProperty("买入状态 0禁用 1启用")
    private String buyStatus;

    /**
     * 卖出状态 默认值 开启
     */
    @ApiModelProperty("卖出状态 0禁用 1启用")
    private String sellStatus;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;



    /**
     * 钱包地址
     */
    @ApiModelProperty("钱包地址")
    private String walletAddress;

    /**
     * 冻结金额
     */
    @ApiModelProperty("冻结金额/交易中的金额")
    private BigDecimal frozenAmount;

    /**
     * 分组
     */
    @ApiModelProperty("会员分组")
    private Long memberGroup;

    /**
     * 首次登录时间
     */
    @ApiModelProperty("首次登录时间")
    private String firstLoginTime;

    /**
     * 登录ip
     */
    @ApiModelProperty("登录ip")
    private String loginIp;

    /**
     * 商户号
     */
    @ApiModelProperty("商户号")
    private String merchantCode;

    /**
     * 商户名称
     */
    @ApiModelProperty("商户名称")
    private String merchantName;


    /**
     * 商户会员ID
     */
    @ApiModelProperty("商户会员ID")
    private String externalMemberId;

    /**
     * 冻结金额
     */
    @ApiModelProperty("后台冻结金额/冻结金额")
    private BigDecimal biFrozenAmount;


    /**
     * 充值次数
     */
    @ApiModelProperty("充值次数")
    private Long rechargeNum;


    /**
     * 累计充值金额
     */
    @ApiModelProperty("累计充值金额")
    private BigDecimal rechargeTotalAmount;


    /**
     * 提现次数
     */
    @ApiModelProperty("提现次数")
    private Long withdrawNum;


    /**
     * 累计提现金额
     */
    @ApiModelProperty("累计提现金额")
    private BigDecimal withdrawTotalAmount;


    /**
     * 信用分
     */
    @ApiModelProperty("信用分")
    private BigDecimal creditScore;

    /**
     * 信用分
     */
    @ApiModelProperty("信用分")
    private Integer level;

    /**
     * 邀请码
     */
    @ApiModelProperty("邀请码")
    private String invitationCode;
}