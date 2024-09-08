package org.ar.wallet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.ar.wallet.Enum.OrderStatusEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("usdt_buy_order")
public class UsdtBuyOrder extends BaseEntityOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会员id
     */
    private String memberId;

    /**
     * 会员账号
     */
    private String memberAccount;

    /**
     * 订单号
     */
    private String platformOrder;

    /**
     * USDT地址
     */
    private String usdtAddr;

    /**
     * USDT数量
     */
    private BigDecimal usdtNum;

    /**
     * USDT实际数量
     */
    private BigDecimal usdtActualNum;

    /**
     * ARB数量
     */
    private BigDecimal arbNum;

    /**
     * ARB实际数量
     */
    private BigDecimal arbActualNum;

    /**
     * 订单状态 默认值: 待支付
     */
    private String status = OrderStatusEnum.BE_PAID.getCode();

    /**
     * USDT支付凭证
     */
    private String usdtProof;

    private String remark;

    /**
     * 商户号
     */
    private String merchantCode;

    /**
     * 商户名称
     */
    private String merchantName;

    /**
     * 支付时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paymentTime;


    @TableField(exist = false)
    private BigDecimal usdtNumTotal;
    @TableField(exist = false)
    private BigDecimal arbNumTotal;

}