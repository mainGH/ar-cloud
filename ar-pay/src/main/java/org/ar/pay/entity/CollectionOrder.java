package org.ar.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.ar.pay.Enum.CollectionOrderStatusEnum;
import org.ar.pay.Enum.NotifyStatusEnum;
import org.ar.pay.Enum.SendStatusEnum;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("collection_order")
public class CollectionOrder extends BaseEntityOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 币种
     */
    private String currency;

    /**
     * 支付方式
     */
    private String payType;

    /**
     * 商户订单号
     */
    private String merchantOrder;

    /**
     * 平台订单号
     */
    private String platformOrder;

    /**
     * 三方订单号
     */
    private String thirdOrder;

    /**
     * 转账流水
     */
    private String transferStatement;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 订单费率
     */
    private BigDecimal orderRate;

    /**
     * 汇率
     */
    private BigDecimal exchangeRate;

    /**
     * 转换金额
     */
    private BigDecimal conversionAmount;

    /**
     * 手续费
     */
    private BigDecimal commission;

    /**
     * 结算金额
     */
    private BigDecimal settlementAmount;

    /**
     * 收款金额
     */
    private BigDecimal collectedAmount;

    /**
     * 订单状态 默认值为: 待支付
     */
    private String orderStatus = CollectionOrderStatusEnum.BE_PAID.getCode();

    /**
     * 回调状态 默认值为: 未回调
     */
    private String callbackStatus = NotifyStatusEnum.NOTCALLBACK.getCode();

    /**
     * 创建时间
     */
//    @TableField(value = "create_time", fill = FieldFill.INSERT)
//    private LocalDateTime createTime;

    /**
     * 修改时间
     */
//    @TableField(fill = FieldFill.INSERT_UPDATE)
//    private LocalDateTime updateTime;

    /**
     * 创建人
     */
//    private String createBy;

    /**
     * 修改人
     */
//    private LocalDateTime updateBy;


    private String country;

    private String merchantCode;

    private String goodsName;

    private String notifyUrl;

    private String thirdCode;
    @TableField(exist = false)
    private String sign;

    /**
     * 时间戳
     */
    private String timestamp;


    /**
     * 客户端ip
     */
    private String clientIp;


    /**
     * 订单详情日志
     */
    private String logInfo;

    /**
     * 签名key
     */
    @TableField(exist = false)
    private String key;

    /**
     * 回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime callbackTime;

    /**
     * 订单费用
     */
    private BigDecimal cost;

    /**
     * 是否发送 默认值为: 未发送
     */
    private String send = SendStatusEnum.UNSENT.getCode();


    /**
     * 总代收金额统计
     */
    @TableField(exist = false)
    @ApiModelProperty(value = "总代收金额统计")
    private String allCollectionAmount;

}