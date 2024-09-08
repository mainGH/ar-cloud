package org.ar.wallet.service;

public interface AsyncNotifyService {

    /**
     * 发送 充值成功 异步回调通知
     *
     * @param type    1 自动回调  2 手动回调
     * @param orderNo
     * @return {@link Boolean}
     */
    Boolean sendRechargeSuccessCallback(String orderNo, String type);


    /**
     * 发送 提现成功 异步回调通知
     *
     * @param type    1 自动回调  2 手动回调
     * @param orderNo
     * @return {@link Boolean}
     */
    Boolean sendWithdrawalSuccessCallback(String orderNo, String type);
}
