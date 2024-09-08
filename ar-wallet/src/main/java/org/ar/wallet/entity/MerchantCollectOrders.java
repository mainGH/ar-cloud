package org.ar.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.ar.wallet.Enum.CollectionOrderStatusEnum;
import org.ar.wallet.Enum.NotifyStatusEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 商户代收订单表
 * </p>
 *
 * @author
 * @since 2024-01-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("merchant_collect_orders")
public class MerchantCollectOrders extends BaseEntityOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商户订单号
     */
    private String merchantOrder;

    /**
     * 平台订单号
     */
    private String platformOrder;

    /**
     * 订单金额
     */
    private BigDecimal amount = BigDecimal.ZERO;;

    /**
     * 订单费率
     */
    private BigDecimal orderRate;

    /**
     * 订单状态 默认 待支付
     */
    private String orderStatus = CollectionOrderStatusEnum.BE_PAID.getCode();

    /**
     * 交易回调状态 默认状态: 未回调
     */
    private String tradeCallbackStatus = NotifyStatusEnum.NOTCALLBACK.getCode();


    /**
     * 商户号
     */
    private String merchantCode;

    /**
     * 交易回调地址
     */
    private String tradeNotifyUrl;

    /**
     * 交易回调是否发送
     */
    private String tradeNotifySend;

    /**
     * 时间戳
     */
    private String timestamp;

    /**
     * 客户端ip
     */
    private String clientIp;

    /**
     * 交易回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeCallbackTime;

    /**
     * 费用
     */
    private BigDecimal cost = BigDecimal.ZERO;;

    /**
     * 会员id
     */
    private String memberId;

    /**
     * 商户会员id
     */
    private String externalMemberId;

    /**
     * 奖励
     */
    private Integer bonus;

    /**
     * 支付方式
     */
    private String payType;

    /**
     * 完成时长
     */
    private String completeDuration;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 商户类型
     */
    private String merchantType;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;

    /**
     * 手动完成人
     */
    private String completedBy;

    /**
     * 取消人
     */
    private String cancelBy;

    /**
     * 申诉时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appealTime;

    @TableField(exist = false)
    @ApiModelProperty(value = "订单金额总计")
    private BigDecimal amountTotal;

    @TableField(exist = false)
    @ApiModelProperty(value = "奖励金额总计")
    private BigDecimal costTotal;

}
