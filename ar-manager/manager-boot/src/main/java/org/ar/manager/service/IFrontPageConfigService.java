package org.ar.manager.service;

import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.AppVersionDTO;
import org.ar.common.pay.dto.FrontPageConfigDTO;
import org.ar.manager.entity.FrontPageConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 首页弹窗内容 服务类
 * </p>
 *
 * @author 
 * @since 2024-04-27
 */
public interface IFrontPageConfigService extends IService<FrontPageConfigDTO> {

    List<FrontPageConfigDTO> listPage();

    RestResult updateInfo(FrontPageConfigDTO req);
}
