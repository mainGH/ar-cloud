package org.ar.common.pay.req;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

import java.time.LocalDateTime;


/**
 * @author Admin
 */
@Data
@ApiModel(description = "商户账变实体类")
public class AccountChangeReq extends PageRequest {
    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号，商户后台必传", required = false)
    private String merchantCode;

    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户名称", required = false)
    private String merchantName;

    /**
     * 订单号
     */
    @ApiModelProperty(value = "订单号")
    private String orderNo;

    /**
     * 账变类型
     */
    @ApiModelProperty(value = "账变类型")
    private Integer changeType;

    /**
     * 开始时间
     */
    @ApiModelProperty(value = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @ApiModelProperty(value = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

}
