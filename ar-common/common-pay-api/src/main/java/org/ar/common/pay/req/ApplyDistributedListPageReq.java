package org.ar.common.pay.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.ar.common.core.page.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* 
*
* @author 
*/
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Accessors(chain = true)
    @ApiModel(description ="下发申请参数")
    public class ApplyDistributedListPageReq extends PageRequest {






            @ApiModelProperty("订单号")
            private String orderNo;

//            /**
//            * 商户
//            */
//            @ApiModelProperty("商户")
//            private String merchantCode;


    /**
     * 商户
     */
    @ApiModelProperty("商户名")
    private String username;



//
//           @ApiModelProperty("类型")
//           private String type;




            @ApiModelProperty("开始时间")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime startTime;
            @ApiModelProperty("结束时间")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime endTime;


}