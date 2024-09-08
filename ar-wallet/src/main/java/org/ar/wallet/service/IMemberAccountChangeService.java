package org.ar.wallet.service;


import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MemberAccountChangeDTO;
import org.ar.common.pay.req.MemberAccountChangeReq;
import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.wallet.entity.MemberAccountChange;
import org.ar.wallet.req.ViewTransactionHistoryReq;
import org.ar.wallet.vo.ViewTransactionHistoryVo;

import java.math.BigDecimal;

/**
* @author 
*/
    public interface IMemberAccountChangeService extends IService<MemberAccountChange> {

     PageReturn<MemberAccountChangeDTO> listPage(MemberAccountChangeReq req);


    /**
     * 记录会员账变
     *
     * @param mid             会员ID
     * @param changeAmount    账变金额
     * @param changeType      交易类型
     * @param orderNo         订单号
     * @param previousBalance 账变前余额
     * @param newBalance      账变后余额
     * @param merchantOrder   商户订单号
     * @return {@link Boolean}
     */
    Boolean recordMemberTransaction(String mid, BigDecimal changeAmount, String changeType, String orderNo, BigDecimal previousBalance, BigDecimal newBalance, String merchantOrder);


    /**
     * 交易记录
     *
     * @param viewTransactionHistoryReq
     * @return {@link RestResult}<{@link PageReturn}<{@link ViewTransactionHistoryVo}>>
     */
    RestResult<PageReturn<ViewTransactionHistoryVo>> viewTransactionHistory(ViewTransactionHistoryReq viewTransactionHistoryReq);
}
