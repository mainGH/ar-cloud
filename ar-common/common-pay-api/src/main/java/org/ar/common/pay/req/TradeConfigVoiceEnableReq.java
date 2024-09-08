package org.ar.common.pay.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 交易配置表
 */
@Data
@ApiModel(description = "配置信息")
public class TradeConfigVoiceEnableReq implements Serializable {

    private Long id;


    /**
     * 语音到账提醒功能开关 1：开启 0：关闭
     */
    @ApiModelProperty("语音到账提醒功能开关 1：开启 0：关闭")
    private Integer voicePaymentReminderEnabled;

    /**
     * 短信余额报警阈值
     */
    @ApiModelProperty("短信余额报警阈值")
    private BigDecimal messageBalanceThreshold;

    /**
     * 交易信用分限制
     */
    @ApiModelProperty("交易信用分限制")
    private BigDecimal tradeCreditScoreLimit;

    /**
     * 商户订单未产生预警
     */
    @ApiModelProperty("商户订单未产生预警")
    private Integer merchantOrderUncreatedTime;
}