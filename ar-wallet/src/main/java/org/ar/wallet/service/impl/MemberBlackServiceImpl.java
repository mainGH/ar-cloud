package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.MemberBlackDTO;
import org.ar.common.pay.req.MemberBlackReq;
import org.ar.common.redis.constants.RedisKeys;
import org.ar.common.redis.util.RedisUtils;
import org.ar.wallet.Enum.BuyStatusEnum;
import org.ar.wallet.Enum.MemberStatusEnum;
import org.ar.wallet.Enum.SellStatusEnum;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.MemberBlack;
import org.ar.wallet.mapper.MemberBlackMapper;
import org.ar.wallet.mapper.MemberInfoMapper;
import org.ar.wallet.service.IMemberBlackService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.wallet.service.IMemberInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 会员黑名单 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-03-29
 */
@Service
@RequiredArgsConstructor
public class MemberBlackServiceImpl extends ServiceImpl<MemberBlackMapper, MemberBlack> implements IMemberBlackService {

    private final WalletMapStruct walletMapStruct;
    private final MemberBlackMapper memberBlackMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final RedisUtils redisUtil;


    @Override
    public PageReturn<MemberBlackDTO> listPage(MemberBlackReq req) {
        Page<MemberBlack> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MemberBlack> lambdaQuery = lambdaQuery();
        if (StringUtils.isNotBlank(req.getMemberId())) {
            lambdaQuery.eq(MemberBlack::getMemberId, req.getMemberId());
        }
        if (StringUtils.isNotBlank(req.getMerchantMemberId())) {
            lambdaQuery.eq(MemberBlack::getMerchantMemberId, req.getMerchantMemberId());
        }
        if (StringUtils.isNotBlank(req.getMemberAccount())) {
            lambdaQuery.eq(MemberBlack::getMemberAccount, req.getMemberAccount());
        }
        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.eq(MemberBlack::getMerchantCode, req.getMerchantCode());
        }
        lambdaQuery.orderByDesc(MemberBlack::getOpTime);
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<MemberBlack> records = page.getRecords();
        List<MemberBlackDTO> list = walletMapStruct.memberBlackToDto(records);
        return PageUtils.flush(page, list);
    }

    @Override
    public RestResult removeBlack(MemberBlackReq req) {
        // 更新会员信息
        memberInfoMapper.updateMemberInfoStatus(req.getMemberId(), MemberStatusEnum.ENABLE.getCode(), BuyStatusEnum.ENABLE.getCode(), SellStatusEnum.ENABLE.getCode());
        this.baseMapper.delete(lambdaQuery().eq(MemberBlack::getMemberId, req.getMemberId()).getWrapper());
        redisUtil.lRemove(RedisKeys.MEMBER_BALCK_LIST, 0, req.getMemberId());
        return RestResult.ok();
    }


    @Override
    public Boolean addBlack(MemberBlack req) {

        LambdaQueryChainWrapper<MemberBlack> lambdaQuery = new LambdaQueryChainWrapper<>(memberBlackMapper);
        lambdaQuery.eq(MemberBlack::getMemberId, req.getMemberId());
        Integer count = this.baseMapper.selectCount(lambdaQuery.getWrapper());
        if(count > 0){
            return false;
        }
        // 更新会员信息
        //memberInfoMapper.updateMemberInfoStatus(req.getMemberId(),MemberStatusEnum.DISABLE.getCode(), BuyStatusEnum.DISABLE.getCode(), SellStatusEnum.DISABLE.getCode());
        redisUtil.lSet(RedisKeys.MEMBER_BALCK_LIST, req.getMemberId());
        this.baseMapper.insert(req);

        //todo 踢人
        return true;
    }
}
