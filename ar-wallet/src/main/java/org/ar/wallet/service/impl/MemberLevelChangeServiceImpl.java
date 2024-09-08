package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MemberLevelChangeDTO;
import org.ar.common.pay.dto.MemberLevelConfigDTO;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.MemberLevelChange;
import org.ar.wallet.entity.MemberLevelConfig;
import org.ar.wallet.mapper.MemberLevelChangeMapper;
import org.ar.wallet.service.IMemberLevelChangeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 会员等级变化记录 服务实现类
 * </p>
 *
 * @author 
 * @since 2024-04-10
 */
@Service
@RequiredArgsConstructor
public class MemberLevelChangeServiceImpl extends ServiceImpl<MemberLevelChangeMapper, MemberLevelChange> implements IMemberLevelChangeService {
    private final WalletMapStruct walletMapStruct;
    @Override
    public List<MemberLevelChangeDTO> listPage(String memberId) {
        LambdaQueryChainWrapper<MemberLevelChange> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(MemberLevelChange::getCreateTime).last(" limit 3");
        lambdaQuery.eq(MemberLevelChange::getMid, memberId);
        List<MemberLevelChange> result = baseMapper.selectList(lambdaQuery.getWrapper());
        List<MemberLevelChangeDTO> list = walletMapStruct.memberLevelChangeToDto(result);
        return list;
    }
}
