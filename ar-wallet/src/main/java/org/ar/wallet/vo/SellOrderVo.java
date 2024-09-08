package org.ar.wallet.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author
 * 订单统计信息
 */
@Data
public class SellOrderVo implements Serializable {

    /**
     * 订单号
     */
    @ApiModelProperty(value = "平台订单号")
    private String platformOrder;
}