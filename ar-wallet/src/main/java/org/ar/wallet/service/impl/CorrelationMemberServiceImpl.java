package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.CorrelationMemberDTO;
import org.ar.common.pay.dto.MemberBlackDTO;
import org.ar.common.pay.req.MemberBlackReq;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.CorrelationMember;
import org.ar.wallet.entity.MemberAccountChange;
import org.ar.wallet.entity.MemberBlack;
import org.ar.wallet.mapper.CorrelationMemberMapper;
import org.ar.wallet.service.ICorrelationMemberService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 关联会员信息 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-03-30
 */
@Service
@RequiredArgsConstructor
public class CorrelationMemberServiceImpl extends ServiceImpl<CorrelationMemberMapper, CorrelationMember> implements ICorrelationMemberService {


    private final WalletMapStruct walletMapStruct;

    @Override
    public PageReturn<CorrelationMemberDTO> listPage(MemberBlackReq req) {
        Page<CorrelationMember> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<CorrelationMember> lambdaQuery = lambdaQuery();
        if (StringUtils.isNotBlank(req.getMemberId())) {
            lambdaQuery.eq(CorrelationMember::getMemberId, req.getMemberId());
        }
        if (StringUtils.isNotBlank(req.getMerchantMemberId())) {
            lambdaQuery.eq(CorrelationMember::getMerchantMemberId, req.getMerchantMemberId());
        }
        if (StringUtils.isNotBlank(req.getMemberAccount())) {
            lambdaQuery.eq(CorrelationMember::getMemberAccount, req.getMemberAccount());
        }
        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.ge(CorrelationMember::getMerchantCode, req.getMerchantCode());
        }
        if (StringUtils.isNotBlank(req.getRelationsIp())) {
            lambdaQuery.ge(CorrelationMember::getRelationsIp, req.getRelationsIp());
        }
        lambdaQuery.orderByDesc(CorrelationMember::getCreateTime);
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<CorrelationMember> records = page.getRecords();
        List<CorrelationMemberDTO> list = walletMapStruct.correlationMemberToDto(records);
        return PageUtils.flush(page, list);
    }
}
