package org.ar.wallet.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

/**
 * @author
 */
@Data
@ApiModel(description = "查询会员列表请求参数")
public class MemberInfoQueryWrapperVo extends PageRequest {

    private static final long serialVersionUID = 2763506398943136939L;

    /*
     * 会员ID
     * */
    @ApiModelProperty(value = "会员ID")
    private String memberId;


    /**
     * 会员账号
     */
    @ApiModelProperty(value = "会员账号")
    private String memberAccount;

}