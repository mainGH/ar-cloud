package org.ar.common.pay.req;


import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.ar.common.core.page.PageRequest;

/**
 * 交易配置表
 *
 *
 */
@Data
@ApiModel(description = "配置信息")
public class TradeConfigSchemeListPageReq extends PageRequest {

  //   private Long id;


}