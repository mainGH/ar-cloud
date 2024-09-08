package org.ar.wallet.service;

import org.ar.common.core.result.ApiResponse;
import org.ar.common.core.result.KycRestResult;
import org.ar.wallet.entity.KycTransactionMessage;
import org.ar.wallet.req.KycPartnerReq;
import org.ar.wallet.req.KycSellReq;
import org.ar.wallet.req.LinkKycPartnerReq;
import org.ar.wallet.vo.KycBanksVo;
import org.ar.wallet.vo.KycPartnersVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface IKycCenterService {


    /**
     * 获取KYC列表
     *
     * @return {@link KycRestResult}<{@link List}<{@link KycPartnersVo}>>
     */
    KycRestResult<List<KycPartnersVo>> getKycPartners();


    /**
     * 添加 KYC Partner
     *
     * @param kycPartnerReq
     * @param request
     * @return {@link ApiResponse}
     */
    KycRestResult addKycPartner(KycPartnerReq kycPartnerReq, HttpServletRequest request);


    /**
     * 连接KYC
     *
     * @param linkKycPartnerReq
     * @param request
     * @return {@link KycRestResult}
     */
    KycRestResult linkKycPartner(LinkKycPartnerReq linkKycPartnerReq, HttpServletRequest request);


    /**
     * 获取银行列表
     *
     * @return {@link ApiResponse}
     */
    KycRestResult<List<KycBanksVo>> getBanks();


    /**
     * 判断KYC是否在线
     *
     * @param req
     * @return {@link Boolean}
     */
//    Boolean effective(AppToken req);

    /**
     * 开始卖出
     *
     * @param kycSellReq
     * @param request
     * @return {@link KycRestResult}
     */
    KycRestResult startSell(KycSellReq kycSellReq, HttpServletRequest request);


    /**
     * 停止卖出
     *
     * @param kycSellReq
     * @param request
     * @return {@link KycRestResult}
     */
    KycRestResult stopSell(KycSellReq kycSellReq, HttpServletRequest request);


    /**
     * 通过 KYC 验证完成订单
     *
     * @param kycTransactionMessage
     * @return {@link Boolean}
     */
    Boolean finalizeOrderWithKYCVerification(KycTransactionMessage kycTransactionMessage);


}
