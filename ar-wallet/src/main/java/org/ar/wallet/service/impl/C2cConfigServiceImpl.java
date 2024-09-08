package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.C2cConfigDTO;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.C2cConfig;
import org.ar.wallet.entity.CancellationRecharge;
import org.ar.wallet.mapper.C2cConfigMapper;
import org.ar.wallet.req.C2cConfigReq;
import org.ar.wallet.req.CancellationRechargeReq;
import org.ar.wallet.service.IC2cConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author 
*/  @RequiredArgsConstructor
    @Service
    public class C2cConfigServiceImpl extends ServiceImpl<C2cConfigMapper, C2cConfig> implements IC2cConfigService {

    private final WalletMapStruct walletMapStruct;

    @Override
    public PageReturn<C2cConfigDTO> listPage(C2cConfigReq req) {
        Page<C2cConfig> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<C2cConfig> lambdaQuery = lambdaQuery();
//        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getWithdrawalRewardRatio())) {
//            lambdaQuery.eq(CancellationRecharge::getReason, req.getWithdrawalRewardRatio());
//        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<C2cConfig> records = page.getRecords();
        List<C2cConfigDTO> listDTO = walletMapStruct.C2cConfigTransform(records);
        return PageUtils.flush(page, listDTO);
    }

    }
