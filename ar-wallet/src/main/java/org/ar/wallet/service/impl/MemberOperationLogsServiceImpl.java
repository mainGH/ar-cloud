package org.ar.wallet.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.MemberOperationLogsDTO;
import org.ar.common.pay.dto.UserVerificationCodeslistPageDTO;
import org.ar.common.pay.req.MemberOperationLogsReq;
import org.ar.common.pay.req.UserTextMessageReq;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.MemberOperationLogs;
import org.ar.wallet.entity.UserVerificationCodes;
import org.ar.wallet.mapper.MemberOperationLogsMapper;
import org.ar.wallet.service.IMemberOperationLogsService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 会员操作日志表 服务实现类
 * </p>
 *
 * @author
 * @since 2024-01-13
 */
@Service
@RequiredArgsConstructor
public class MemberOperationLogsServiceImpl extends ServiceImpl<MemberOperationLogsMapper, MemberOperationLogs> implements IMemberOperationLogsService {

    private final WalletMapStruct walletMapStruct;

    @Override
    public PageReturn<MemberOperationLogsDTO> listPage(MemberOperationLogsReq req) {
        Page<MemberOperationLogs> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MemberOperationLogs> lambdaQuery = lambdaQuery();
        if (StringUtils.isNotBlank(req.getUserId())) {
            lambdaQuery.eq(MemberOperationLogs::getMemberId, req.getUserId());
        }
        if (StringUtils.isNotBlank(req.getOpModule())) {
            lambdaQuery.eq(MemberOperationLogs::getModuleCode, req.getOpModule());
        }
        if (StringUtils.isNotBlank(req.getLoginIp())) {
            lambdaQuery.eq(MemberOperationLogs::getIpAddress, req.getLoginIp());
        }
        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(MemberOperationLogs::getOperationTime, req.getStartTime());
        }
        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(MemberOperationLogs::getOperationTime, req.getEndTime());
        }
        lambdaQuery.orderByDesc(MemberOperationLogs::getOperationTime);
        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<MemberOperationLogs> records = page.getRecords();
        List<MemberOperationLogsDTO> list = walletMapStruct.memberOperationLogsDto(records);
        return PageUtils.flush(page, list);

    }
}
