package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.WithdrawalCancellationDTO;
import org.ar.common.pay.req.WithdrawalCancellationReq;
import org.ar.wallet.entity.WithdrawalCancellation;

import java.util.List;


/**
* @author 
*/
    public interface IWithdrawalCancellationService extends IService<WithdrawalCancellation> {

         PageReturn<WithdrawalCancellationDTO> listPage(WithdrawalCancellationReq req);


    /**
     * 获取卖出取消原因列表
     *
     * @return {@link List}<{@link String}>
     */
    List<String> getSellCancelReasonsList();
    }
