package org.ar.wallet.entity;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 退回订单表
 * </p>
 *
 * @author 
 * @since 2024-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("cash_back_order")
public class CashBackOrder implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private BigDecimal amount;

    /**
     * 订单状态 1-退回中 2-退回成功 3-退回失败
     */
    private String orderStatus;

    /**
     * 商户号
     */
    private String merchantCode;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 商户会员id
     */
    private String merchantMemberId;

    /**
     * 钱包会员id
     */
    private String memberId;

    /**
     * 请求IP
     */
    private String clientIp;

    /**
     * 请求时间戳
     */
    private String requestTimestamp;

    /**
     * 完成时间戳
     */
    private String responseTimestamp;

    /**
     * 完成时长
     */
    private String completeDuration;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 完成时间
     */
    private LocalDateTime completionTime;

    /**
     * 失败时间
     */
    private LocalDateTime failedTime;

    /**
     * 失败原因
     */
    private String failedReason;


}
