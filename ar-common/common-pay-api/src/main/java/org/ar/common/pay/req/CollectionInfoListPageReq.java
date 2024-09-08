package org.ar.common.pay.req;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@ApiModel(description = "添加收款信息请求参数")
public class CollectionInfoListPageReq extends PageRequest {

    @ApiModelProperty(value = "UPI_ID")
    private String upiId;

    /**
     * UPI_Name
     */
    @ApiModelProperty(value = "UPI_Name")
    private String upiName;







    /**
     * 会员账号
     */
    @ApiModelProperty(value = "会员账号")
    private String memberAccount;

    @ApiModelProperty(value = "会员ID")
    private String memberId;
}
