package org.ar.wallet.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 查询充值订单接口 返回数据
 *
 * @author
 */
@Data
public class DepositQueryVo implements Serializable {


    /**
     * 商户号
     */
    private String merchantCode;


    /**
     * 商户订单号
     */
    private String merchantTradeNo;


    /**
     * 平台订单号
     */
    private String tradeNo;


    /**
     * 充值金额
     */
    private BigDecimal amount;


    /**
     * 订单时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDateTime;


    /**
     * 交易状态
     */
    private String tradeStatus;


    /**
     * 签名
     */
    private String sign;
}