package org.ar.wallet.service;

import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.wallet.entity.C2cConfig;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.wallet.entity.CancellationRecharge;
import org.ar.wallet.req.C2cConfigReq;
import org.ar.wallet.req.CancellationRechargeReq;

/**
* @author 
*/
    public interface IC2cConfigService extends IService<C2cConfig> {

    PageReturn<C2cConfigDTO> listPage(C2cConfigReq req);


}
