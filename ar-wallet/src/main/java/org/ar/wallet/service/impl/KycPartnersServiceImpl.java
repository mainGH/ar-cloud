package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.KycPartnersDTO;
import org.ar.common.pay.req.KycPartnerIdReq;
import org.ar.common.pay.req.KycPartnerListPageReq;
import org.ar.wallet.entity.KycPartners;
import org.ar.wallet.mapper.KycPartnersMapper;
import org.ar.wallet.service.IKycPartnersService;
import org.ar.wallet.util.RequestUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * kyc信息表 服务实现类
 * </p>
 *
 * @author
 * @since 2024-04-20
 */
@Service
public class KycPartnersServiceImpl extends ServiceImpl<KycPartnersMapper, KycPartners> implements IKycPartnersService {

    /**
     * 获取KYC列表
     *
     * @param memberId
     * @return {@link List}<{@link KycPartners}>
     */
    @Override
    public List<KycPartners> getKycPartners(Long memberId) {
        return lambdaQuery()
                .eq(KycPartners::getMemberId, memberId)
                .list();
    }

    /**
     *
     * @param req KycPartnerReq {@link KycPartnerListPageReq}
     * @return {@link PageReturn}<{@link KycPartnersDTO}>
     */
    @Override
    public PageReturn<KycPartnersDTO> listPage(KycPartnerListPageReq req) {
        Page<KycPartners> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<KycPartners> lambdaQuery = lambdaQuery();
        // 查询未删除
        lambdaQuery.eq(KycPartners::getDeleted, 0);
        // 排序
        page.addOrder(RequestUtil.getOrderItem(req.getColumn(), req.isAsc()));
        // 筛选
        if (ObjectUtils.isNotEmpty(req.getMemberId())) {
            lambdaQuery.eq(KycPartners::getMemberId, req.getMemberId());
        }

        if(ObjectUtils.isNotEmpty(req.getMobileNumber())){
            lambdaQuery.eq(KycPartners::getMobileNumber, req.getMobileNumber());
        }

        if(ObjectUtils.isNotEmpty(req.getBankCode())){
            lambdaQuery.eq(KycPartners::getBankCode, req.getBankCode());
        }

        if(ObjectUtils.isNotEmpty(req.getAccount())){
            lambdaQuery.eq(KycPartners::getAccount, req.getAccount());
        }

        if(ObjectUtils.isNotEmpty(req.getUpiId())){
            lambdaQuery.eq(KycPartners::getUpiId, req.getUpiId());
        }

        if(ObjectUtils.isNotEmpty(req.getLinkStatus())){
            lambdaQuery.eq(KycPartners::getLinkStatus, req.getLinkStatus());
        }

        if(ObjectUtils.isNotEmpty(req.getSellStatus())){
            lambdaQuery.eq(KycPartners::getSellStatus, req.getSellStatus());
        }

        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<KycPartners> records = page.getRecords();

        ArrayList<KycPartnersDTO> kycPartnersDTOArrayList = new ArrayList<>();
        for (KycPartners record : records) {
            KycPartnersDTO kycPartnersDTO = new KycPartnersDTO();
            BeanUtil.copyProperties(record, kycPartnersDTO);
            kycPartnersDTOArrayList.add(kycPartnersDTO);
        }
        return PageUtils.flush(page, kycPartnersDTOArrayList);
    }

    /**
     * 删除
     *
     * @param req {@link KycPartnerIdReq} req
     * @return boolean
     */
    @Override
    public boolean delete(KycPartnerIdReq req) {
        return lambdaUpdate().eq(KycPartners::getId, req.getId()).set(KycPartners::getDeleted, 1).update();
    }


    /**
     * 根据upi_id 获取 KYC
     *
     * @param upiId
     * @return {@link KycPartners}
     */
    @Override
    public KycPartners getKYCPartnersByUpiId(String upiId) {
        return lambdaQuery()
                .eq(KycPartners::getUpiId, upiId)//upi_id
                .eq(KycPartners::getDeleted, 0)// 未删除
                .eq(KycPartners::getSellStatus, 1)//卖出状态: 开启
                .eq(KycPartners::getLinkStatus, 1)//连接状态: 开启
                .one();
    }
}
