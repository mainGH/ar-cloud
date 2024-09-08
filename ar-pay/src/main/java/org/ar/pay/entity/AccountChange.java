package org.ar.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("account_change")
public class AccountChange extends BaseEntityOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商户号
     */
    private String merchantCode;

    /**
     * 币种
     */
    private String currentcy;

    /**
     * 账变类型
     */
    private String type;

    /**
     * 商户订单号
     */
    private String orderNo;

    /**
     * 账变前
     */
    private BigDecimal beforeChange;

    /**
     * 变化金额
     */
    private BigDecimal amountChange;

    /**
     * 账变后金额
     */
    private BigDecimal afterChange;


}