package org.ar.pay.req;

import cn.hutool.core.date.DateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

@Data
@ApiModel(description = "获取账变列表请求参数")
public class AccountChangeReq extends PageRequest {

    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号")
    private String merchantCode;

    /**
     * 账变类型
     */
    @ApiModelProperty(value = "账变类型")
    private String type;

    /**
     * 商户订单号
     */
    @ApiModelProperty(value = "商户订单号")
    private String orderNo;


    /**
     * 订单时间
     */
    private DateTime createTime;

}
