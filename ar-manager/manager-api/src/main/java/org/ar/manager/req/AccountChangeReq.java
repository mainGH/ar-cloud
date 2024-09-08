package org.ar.manager.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;


/**
 * @author Admin
 */
@Data
@ApiModel(description = "商户账变实体类")
public class AccountChangeReq extends PageRequest {
    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号")
    private String merchantCode;
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
    @ApiModelProperty(value = "开始时间", example = "2023-10-20 15:04:56")
    private String startTime;

    /**
     * 结束时间
     */
    @ApiModelProperty(value = "结束时间", example = "2023-10-20 15:04:56")
    private String endTime;

}
