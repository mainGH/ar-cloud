package org.ar.pay.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
* 
*
* @author 
*/
    @Data
    @ApiModel(description = "账变参数")
    public class AccountChangeVo implements Serializable {





            /**
            * 商户号
            */
            @ApiModelProperty(value = "商户号")
           private String merchantCode;

            /**
            * 币种
            */
            @ApiModelProperty(value = "币种")
           private String currentcy;

            /**
            * 账变类型
            */
    private String type;

            /**
            * 商户订单号
            */
            @ApiModelProperty(value = "账变类型")
    private String orderNo;

            /**
            * 账变前
            */
            @ApiModelProperty(value = "账变前")
    private BigDecimal beforeChange;

            /**
            * 变化金额
            */
            @ApiModelProperty(value = "账变金额")
    private BigDecimal amountChange;

            /**
            * 账变后金额
            */
            @ApiModelProperty(value = "账变后金额")
    private BigDecimal afterChange;



}