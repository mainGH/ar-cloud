package org.ar.common.pay.req;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "代收订单列表请求参数")
public class CollectionOrderListPageReq extends PageRequest {

    /**
     * 商户号
     */
    @ApiModelProperty(value = "商户号")
    private String merchantCode;


//    /**
//     * 币种
//     */
//    @ApiModelProperty(value = "币种")
//    private String currency;

    /**
     * 会员id
     */
    @ApiModelProperty(value = "会员Id")
    private String memberId;

    /**
     * UTR
     */
    @ApiModelProperty(value = "utr")
    private String utr;

    /**
     * 商户订单号
     */
    @ApiModelProperty(value = "商户订单号")
    private String merchantOrder;

    /**
     * 平台订单号
     */
    @ApiModelProperty(value = "平台订单号")
    private String platformOrder;






    @ApiModelProperty("订单状态1待匹配2匹配超市3待支付4确认中5确认超时6申诉中7已完成8已取消9订单失效10买入失败11金额错误")
    private String orderStatus;






    /**
     * 交易回调状态 默认状态: 未回调
     */
    @ApiModelProperty("交易回调状态1未回调2回调成功3回调失败4手动回调成功5手动回调失败")
    private String tradeCallbackStatus ;


    /**
     * 开始时间 时间戳(10位)
     */
    @ApiModelProperty(value = "完成时间开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTimeStart;

    /**
     * 结束时间 时间戳(10位)
     */
    @ApiModelProperty(value = "完成时间结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime  completionTimeEnd;


    /**
     * 提现开始时间
     */
    @ApiModelProperty(value = "提现开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime  startTime;


    /**
     * 提现结束时间
     */
    @ApiModelProperty(value = "提现结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime  endTime;

    /**
     * 创建人
     */
//    @ApiModelProperty(value="创建人")
//    private String createBy;

    /**
     * 修改人
     */
//    @ApiModelProperty(value="修改人")
//    private LocalDateTime updateBy;
    @ApiModelProperty(value = "完成时长开始")
    private Integer completeDurationStart;
    @ApiModelProperty(value = "完成时长结束")
    private Integer completeDurationEnd;

    @ApiModelProperty("语言")
    private String lang ;


    @ApiModelProperty(value = "1-操作超时 2-IP黑名单 3-正常")
    private String riskTag;



    /**
     * 是否通过KYC自动完成 1: 是
     */
    @ApiModelProperty(value = "是否通过KYC自动完成 0：否 1: 是")
    private Integer kycAutoCompletionStatus;

}
