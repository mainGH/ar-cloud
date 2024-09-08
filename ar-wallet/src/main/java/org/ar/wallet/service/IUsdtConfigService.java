package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.UsdtConfigDTO;
import org.ar.common.pay.req.UsdtConfigPageReq;
import org.ar.wallet.entity.UsdtConfig;

import java.util.List;

/**
 * @author
 */
public interface IUsdtConfigService extends IService<UsdtConfig> {

    PageReturn<UsdtConfigDTO> listPage(UsdtConfigPageReq req);

    /**
     * 匹配USDT收款信息
     *
     * @param networkProtocol
     * @return {@link UsdtConfig}
     */
    UsdtConfig matchUsdtReceiptInfo(String networkProtocol);


    /**
     * 获取主网络下拉列表
     *
     * @return {@link List}
     */
    List getNetworkProtocol();
}