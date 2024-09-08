package org.ar.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.wallet.Enum.NotifyStatusEnum;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.Enum.PayTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@TableName("payment_order")
public class PaymentOrder extends BaseEntityOrder {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 支付方式 默认值: UPI
     */
    private String payType = PayTypeEnum.INDIAN_UPI.getCode();

    /**
     * 商户号
     */
    private String merchantCode;

    /**
     * 商户订单号
     */
    private String merchantOrder;

    /**
     * 平台订单号
     */
    private String platformOrder;

    /**
     * 匹配订单号
     */
    private String matchOrder;

    /**
     * UPI_ID
     */
    private String upiId;

    /**
     * UPI_Name
     */
    private String upiName;

    /**
     * 会员ID
     */
    private String memberId;

    /**
     * 会员账号
     */
    private String memberAccount;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String mobileNumber;

    /**
     * 订单费率
     */
    private BigDecimal orderRate;

    /**
     * 订单金额
     */
    private BigDecimal amount = BigDecimal.ZERO;;

    /**
     * 实际金额
     */
    private BigDecimal actualAmount = BigDecimal.ZERO;;

    /**
     * 订单状态 默认状态: 待匹配
     */
    private String orderStatus = OrderStatusEnum.BE_MATCHED.getCode();

    /**
     * 匹配回调地址
     */
    private String matchNotifyUrl;

    /**
     * 匹配回调状态 默认状态: 未回调
     */
    private String matchCallbackStatus = NotifyStatusEnum.NOTCALLBACK.getCode();

    /**
     * 匹配回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime matchCallbackTime;

    /**
     * 交易回调地址
     */
    private String tradeNotifyUrl;

    /**
     * 交易回调状态 默认状态: 未回调
     */
    private String tradeCallbackStatus = NotifyStatusEnum.NOTCALLBACK.getCode();

    /**
     * 交易回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeCallbackTime;

    /**
     * 费用
     */
    private BigDecimal cost;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 客户端ip
     */
    private String clientIp;

    /**
     * 奖励
     */
    private BigDecimal bonus = BigDecimal.ZERO;;

    /**
     * 匹配时长
     */
    private String matchDuration;

    /**
     * 完成时长
     */
    private String completeDuration;

    /**
     * UTR
     */
    private String utr;

    /**
     * 凭证
     */
    private String voucher;

    /**
     * 备注
     */
    private String remark;

    /**
     * 收款信息id
     */
    private Long collectionInfoId;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;

    /**
     * 匹配时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime matchTime;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;

    /**
     * 申诉审核人
     */
    private String appealReviewBy;

    /**
     * 申诉审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appealReviewTime;

    /**
     * 取消人
     */
    private String cancelBy;

    /**
     * 取消时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;

    /**
     * 撮合列表订单号
     */
    private String matchingPlatformOrder;

    /**
     * 金额错误图片
     */
    private String amountErrorImage;

    /**
     * 金额错误视频
     */
    private String amountErrorVideo;

    /**
     * 金额错误提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime amountErrorSubmitTime;

    /**
     * 申诉时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appealTime;

    /**
     * 匹配时间戳 (每次匹配时 都要更新这个值) 避免MQ延时消息问题 还有定时任务兜底方案扫描 也是扫这个值
     */
    private Long lastUpdateTimestamp;


    /**
     * 匹配超时
     */
    private Integer matchTimeout;


    /**
     * 继续匹配
     */
    private Integer continueMatching;


    /**
     * 取消匹配
     */
    private Integer cancelMatching;


    /**
     * 用户头像
     */
    private Integer avatar;

    /**
     * 取消原因
     */
    private String cancellationReason;

    /**
     * 预计匹配时间
     */
    private Integer estimatedMatchTime;

    /**
     * 是否通过KYC自动完成 1: 是
     */
    private Integer kycAutoCompletionStatus;

    /**
     * 商户类型
     */
    private String merchantType;


    @ApiModelProperty(value = "风控标识-超时 0-正常 1-操作超时")
    private Integer riskTagTimeout;

    @ApiModelProperty(value = "风控标识-黑名单 0-正常 1-黑名单")
    private Integer riskTagBlack;

    @TableField(exist = false)
    @ApiModelProperty(value = "订单金额总计")
    private BigDecimal amountTotal;

    @TableField(exist = false)
    @ApiModelProperty(value = "实际金额总计")
    private BigDecimal actualAmountTotal;

    @TableField(exist = false)
    @ApiModelProperty(value = "奖励总计")
    private BigDecimal bonusTotal;

    /**
     * 人工审核截至时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditDelayTime;

    public String getAmountStr() {
        return this.getAmount().toString();
    }

}