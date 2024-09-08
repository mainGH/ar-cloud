package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.KycPartnersDTO;
import org.ar.common.pay.req.KycPartnerIdReq;
import org.ar.common.pay.req.KycPartnerListPageReq;
import org.ar.wallet.entity.KycPartners;

import java.util.List;

/**
 * <p>
 * kyc信息表 服务类
 * </p>
 *
 * @author
 * @since 2024-04-20
 */
public interface IKycPartnersService extends IService<KycPartners> {

    /**
     * 获取KYC列表
     *
     * @param memberId
     * @return {@link List}<{@link KycPartners}>
     */
    List<KycPartners> getKycPartners(Long memberId);

    /**
     * 获取
     *
     * @param kycPartnerListPageReq KycPartnerReq {@link KycPartnerListPageReq}
     * @return {@link PageReturn} <{@link KycPartnersDTO}>
     */
    PageReturn<KycPartnersDTO> listPage(KycPartnerListPageReq kycPartnerListPageReq);


    /**
     * 删除
     *
     * @param req {@link KycPartnerIdReq} req
     * @return boolean
     */
    boolean delete(KycPartnerIdReq req);


    /**
     * 根据upi_id 获取 KYC
     *
     * @param upiId
     * @return {@link KycPartners}
     */
    KycPartners getKYCPartnersByUpiId(String upiId);

}
