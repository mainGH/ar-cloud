package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.ControlSwitchDTO;
import org.ar.common.pay.req.*;
import org.ar.wallet.entity.ControlSwitch;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 后台控制开关表 服务类
 * </p>
 *
 * @author 
 * @since 2024-03-21
 */
public interface IControlSwitchService extends IService<ControlSwitch> {
    RestResult<ControlSwitchDTO>  createControlSwitch(ControlSwitchReq req);
    RestResult<ControlSwitchDTO>  updateControlSwitchInfo(ControlSwitchUpdateReq req);
    RestResult<ControlSwitchDTO>  updateControlSwitchStatus(ControlSwitchStatusReq req);
    RestResult<ControlSwitchDTO>  detail(ControlSwitchIdReq req);
    PageReturn<ControlSwitchDTO> listPage(ControlSwitchPageReq req);


    /**
     * 检查指定开关是否开启
     *
     * @param switchId
     * @return boolean
     */
    boolean isSwitchEnabled(Long switchId);
}
