package org.ar.wallet.req;


import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 提现接口 请求参数
 *
 * @author Simon
 * @date 2023/12/26
 */
@Data
public class WithdrawalApplyReq implements Serializable {

    /**
     * 商户号
     */
    @NotBlank(message = "merchantCode cannot be empty")
    private String merchantCode;


    /**
     * 会员id
     */
    @NotBlank(message = "memberId cannot be empty")
    private String memberId;


    /**
     * 商户订单号
     */
    @NotBlank(message = "merchantTradeNo cannot be empty")
    private String merchantTradeNo;


    /**
     * 提现金额
     */
    @NotNull(message = "amount cannot be empty")
    @DecimalMin(value = "0.00", message = "amount format is incorrect")
    private String amount;


    /**
     * 渠道编码
     */
    @NotBlank(message = "channel cannot be empty")
    private String channel;


    /**
     * 时间戳
     */
    @NotBlank(message = "timestamp cannot be empty")
    @Pattern(regexp = "^[0-9]{10}$", message = "timestamp format is incorrect")
    private String timestamp;


    /**
     * 异步回调地址
     */
    @NotBlank(message = "notifyUrl cannot be empty")
    private String notifyUrl;


    /**
     * 签名
     */
    @NotBlank(message = "sign cannot be empty")
    private String sign;
}
