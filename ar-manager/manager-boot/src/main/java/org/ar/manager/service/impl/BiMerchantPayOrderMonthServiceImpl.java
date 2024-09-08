package org.ar.manager.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.BiMerchantPayOrderExportDTO;
import org.ar.common.pay.dto.BiWithdrawOrderDailyExportDTO;
import org.ar.common.web.exception.BizException;
import org.ar.manager.entity.BiMerchantPayOrderDaily;
import org.ar.manager.entity.BiMerchantPayOrderMonth;
import org.ar.manager.mapper.BiMerchantPayOrderMonthMapper;
import org.ar.manager.req.MerchantMonthReportReq;
import org.ar.manager.service.IBiMerchantPayOrderMonthService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author
 */
@Service
public class BiMerchantPayOrderMonthServiceImpl extends ServiceImpl<BiMerchantPayOrderMonthMapper, BiMerchantPayOrderMonth> implements IBiMerchantPayOrderMonthService {

    @Override
    public PageReturn<BiMerchantPayOrderMonth> listPage(MerchantMonthReportReq req) {
        Page<BiMerchantPayOrderMonth> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<BiMerchantPayOrderMonth> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(BiMerchantPayOrderMonth::getDateTime);

        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiMerchantPayOrderMonth::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiMerchantPayOrderMonth::getDateTime, req.getEndTime());
        }

        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.eq(BiMerchantPayOrderMonth::getMerchantCode, req.getMerchantCode());
        }

        page = baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<BiMerchantPayOrderMonth> records = page.getRecords();
        return PageUtils.flush(page, records);
    }

    @Override
    public PageReturn<BiMerchantPayOrderExportDTO> listPageForExport(MerchantMonthReportReq req) {
        List<BiMerchantPayOrderExportDTO> list = new ArrayList<>();
        PageReturn<BiMerchantPayOrderMonth> biMerchantPayOrderDailyPageReturn = listPage(req);
        List<BiMerchantPayOrderMonth> data = biMerchantPayOrderDailyPageReturn.getList();
        for (BiMerchantPayOrderMonth item : data) {
            BiMerchantPayOrderExportDTO dto = new BiMerchantPayOrderExportDTO();
            BeanUtils.copyProperties(item, dto);
            list.add(dto);
        }
        Page<BiMerchantPayOrderExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(biMerchantPayOrderDailyPageReturn.getTotal());
        return PageUtils.flush(page, list);
    }
}
