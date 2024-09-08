package org.ar.wallet.service.impl;

import org.ar.wallet.vo.BankKycTransactionVo;
import org.ar.wallet.vo.KycBankResponseVo;

import java.util.List;

public interface IAppBankTransaction {


    /**
     * 连接KYC银行
     *
     * @param kycToken
     * @return {@link Boolean}
     */
    KycBankResponseVo linkKycPartner(String kycToken);


    /**
     * 获取 KYC银行交易记录
     *
     * @param kycToken
     * @return {@link List}<{@link BankKycTransactionVo}>
     */
    List<BankKycTransactionVo> getKYCBankTransactions(String kycToken);
}
