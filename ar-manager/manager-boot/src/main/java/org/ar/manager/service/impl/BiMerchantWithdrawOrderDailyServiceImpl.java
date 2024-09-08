package org.ar.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.utils.DurationCalculatorUtil;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.BiMerchantWithdrawOrderDailyDTO;
import org.ar.common.pay.dto.BiWithdrawOrderDailyExportDTO;
import org.ar.common.pay.dto.MerchantOrderOverviewDTO;
import org.ar.manager.entity.BiMerchantPayOrderDaily;
import org.ar.manager.entity.BiMerchantWithdrawOrderDaily;
import org.ar.manager.entity.BiWithdrawOrderDaily;
import org.ar.manager.mapper.BiMerchantWithdrawOrderDailyMapper;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.req.MerchantMonthReportReq;
import org.ar.manager.req.WithdrawDailyOrderReportReq;
import org.ar.manager.service.IBiMerchantWithdrawOrderDailyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author
 */
@Service
public class BiMerchantWithdrawOrderDailyServiceImpl extends ServiceImpl<BiMerchantWithdrawOrderDailyMapper, BiMerchantWithdrawOrderDaily> implements IBiMerchantWithdrawOrderDailyService {

    @Override
    public PageReturn<BiMerchantWithdrawOrderDaily> listPage(MerchantMonthReportReq req) {
        Page<BiMerchantWithdrawOrderDaily> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<BiMerchantWithdrawOrderDaily> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(BiMerchantWithdrawOrderDaily::getDateTime);

        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiMerchantWithdrawOrderDaily::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiMerchantWithdrawOrderDaily::getDateTime, req.getEndTime());
        }

        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.eq(BiMerchantWithdrawOrderDaily::getMerchantCode, req.getMerchantCode());
        }

        page = baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<BiMerchantWithdrawOrderDaily> records = page.getRecords();
        return PageUtils.flush(page, records);
    }

    @Override
    public MerchantOrderOverviewDTO getMerchantOrderOverview(MerchantDailyReportReq req) {
        LambdaQueryWrapper<BiMerchantWithdrawOrderDaily> lambdaQuery = new QueryWrapper<BiMerchantWithdrawOrderDaily>()
                .select("IFNULL(sum(actual_money),0) as merchantWithdrawAmount," +
                        "        IFNULL(sum(success_order_num),0) as merchantWithdrawTransNum," +
                        "        IFNULL(sum(total_fee),0) as withdrawFee"
                )
                .lambda();
        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiMerchantWithdrawOrderDaily::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiMerchantWithdrawOrderDaily::getDateTime, req.getEndTime());
        }
        BiMerchantWithdrawOrderDaily biMerchantWithdrawOrderDaily = baseMapper.selectOne(lambdaQuery);
        MerchantOrderOverviewDTO result = new MerchantOrderOverviewDTO();
        result.setMerchantWithdrawAmount(biMerchantWithdrawOrderDaily.getMerchantWithdrawAmount());
        result.setMerchantWithdrawTransNum(biMerchantWithdrawOrderDaily.getMerchantWithdrawTransNum());
        result.setWithdrawFee(biMerchantWithdrawOrderDaily.getWithdrawFee());
        return result;
    }

    @Override
    public PageReturn<BiMerchantWithdrawOrderDailyDTO> listPageForExport(MerchantMonthReportReq req) {
        List<BiMerchantWithdrawOrderDailyDTO> list = new ArrayList<>();
        PageReturn<BiMerchantWithdrawOrderDaily> biWithdrawOrderDailyPageReturn = listPage(req);
        List<BiMerchantWithdrawOrderDaily> data = biWithdrawOrderDailyPageReturn.getList();
        for (BiMerchantWithdrawOrderDaily item : data) {
            BiMerchantWithdrawOrderDailyDTO biWithdrawOrderDailyExportDTO = new BiMerchantWithdrawOrderDailyDTO();
            BeanUtils.copyProperties(item, biWithdrawOrderDailyExportDTO);
            list.add(biWithdrawOrderDailyExportDTO);
        }
        Page<BiMerchantWithdrawOrderDailyDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(biWithdrawOrderDailyPageReturn.getTotal());
        return PageUtils.flush(page, list);
    }
}
