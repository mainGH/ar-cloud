package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.CancellationRechargeDTO;
import org.ar.common.pay.req.CancellationRechargePageListReq;
import org.ar.wallet.entity.CancellationRecharge;

import java.util.List;

/**
 * @author
 */
public interface ICancellationRechargeService extends IService<CancellationRecharge> {

    PageReturn<CancellationRechargeDTO> listPage(CancellationRechargePageListReq req);


    /**
     * 获取买入取消原因列表
     *
     * @return {@link List}<{@link String}>
     */
    List<String> getBuyCancelReasonsList();
}
