package org.ar.common.pay.req;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 后台控制开关表
 * </p>
 *
 * @author 
 * @since 2024-03-21
 */
@Data
@ApiModel(description = "后台开关")
public class ControlSwitchStatusReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 开关id
     */
    private Long switchId;

    /**
     * 状态（1为启用，0为禁用）
     */
    private Integer status;


}
