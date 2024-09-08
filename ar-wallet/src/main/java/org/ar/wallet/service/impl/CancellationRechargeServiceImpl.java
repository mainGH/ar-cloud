package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.CancellationRechargeDTO;
import org.ar.common.pay.req.CancellationRechargePageListReq;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.CancellationRecharge;
import org.ar.wallet.mapper.CancellationRechargeMapper;
import org.ar.wallet.service.ICancellationRechargeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class CancellationRechargeServiceImpl extends ServiceImpl<CancellationRechargeMapper, CancellationRecharge> implements ICancellationRechargeService {
    private final WalletMapStruct walletMapStruct;

    @Override
    public PageReturn<CancellationRechargeDTO> listPage(CancellationRechargePageListReq req) {
        Page<CancellationRecharge> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<CancellationRecharge> lambdaQuery = lambdaQuery();
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getReason())) {
            lambdaQuery.eq(CancellationRecharge::getReason, req.getReason());
        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<CancellationRecharge> records = page.getRecords();
        List<CancellationRechargeDTO> list = walletMapStruct.CancellationRechargeTransform(records);
        return PageUtils.flush(page, list);
    }

    /**
     * 获取买入取消原因列表
     *
     * @return {@link List}<{@link String}>
     */
    @Override
    public List<String> getBuyCancelReasonsList() {
        List<CancellationRecharge> CancellationRechargeList = lambdaQuery().list();

        ArrayList<String> reasonList = new ArrayList<>();

        for (CancellationRecharge cancellationRecharge : CancellationRechargeList) {
            reasonList.add(cancellationRecharge.getReason());
        }

        return reasonList;
    }

}
