package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.TradeConfigDTO;
import org.ar.common.pay.dto.TradeConfigVoiceEnableDTO;
import org.ar.common.pay.dto.TradeManualConfigDTO;
import org.ar.common.pay.dto.TradeWarningConfigDTO;
import org.ar.common.pay.req.TradeConfigIdReq;
import org.ar.common.pay.req.TradeConfigListPageReq;
import org.ar.common.pay.req.TradeConfigVoiceEnableReq;
import org.ar.common.pay.req.TradeConfigWarningConfigUpdateReq;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.TradeConfig;
import org.ar.wallet.mapper.TradeConfigMapper;
import org.ar.wallet.service.ITradeConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author
 */
@Service
@RequiredArgsConstructor
public class TradeConfigServiceImpl extends ServiceImpl<TradeConfigMapper, TradeConfig> implements ITradeConfigService {

    private  final WalletMapStruct walletMapStruct;
    @Override
    public PageReturn<TradeConfigDTO> listPage(TradeConfigListPageReq req) {
        Page<TradeConfig> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<TradeConfig> lambdaQuery = lambdaQuery();
//        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getWithdrawalRewardRatio())) {
//            lambdaQuery.eq(CancellationRecharge::getReason, req.getWithdrawalRewardRatio());
//        }
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<TradeConfig> records = page.getRecords();
        List<TradeConfigDTO> listDTO = walletMapStruct.TradeConfigTransform(records);
        return PageUtils.flush(page, listDTO);
    }

    @Override
    public TradeConfigVoiceEnableDTO updateVoiceEnable(TradeConfigVoiceEnableReq req) {
        TradeConfig tradeConfig = baseMapper.selectById(req.getId());
        BeanUtils.copyProperties(req, tradeConfig);
        TradeConfigVoiceEnableDTO tradeConfigVoiceEnableDTO = new TradeConfigVoiceEnableDTO();
        BeanUtils.copyProperties(tradeConfig, tradeConfigVoiceEnableDTO);
        int update = baseMapper.updateById(tradeConfig);
        return update == 0 ? null : tradeConfigVoiceEnableDTO;
    }

    @Override
    public TradeWarningConfigDTO updateWarningConfig(TradeConfigWarningConfigUpdateReq req) {
        TradeConfig tradeConfig = baseMapper.selectById(req.getId());
        BeanUtils.copyProperties(req, tradeConfig);
        TradeWarningConfigDTO tradeConfigVoiceEnableDTO = new TradeWarningConfigDTO();
        BeanUtils.copyProperties(tradeConfig, tradeConfigVoiceEnableDTO);
        int update = baseMapper.updateById(tradeConfig);
        return update == 0 ? null : tradeConfigVoiceEnableDTO;
    }

    @Override
    public TradeWarningConfigDTO warningConfigDetail(TradeConfigIdReq req) {
        TradeConfig tradeConfig  = baseMapper.selectById(req.getId());
        TradeWarningConfigDTO tradeConfigDTO = new TradeWarningConfigDTO();
        BeanUtils.copyProperties(tradeConfig,tradeConfigDTO);
        return tradeConfigDTO;
    }

    @Override
    public TradeManualConfigDTO manualReview() {
        TradeConfig tradeConfig  = baseMapper.selectById(1);
        TradeManualConfigDTO dto = new TradeManualConfigDTO();
        BeanUtils.copyProperties(tradeConfig, dto);
        return dto;
    }

}
