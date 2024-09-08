package org.ar.manager.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.DurationCalculatorUtil;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.BiPaymentOrderExportDTO;
import org.ar.common.web.exception.BizException;
import org.ar.manager.entity.BiPaymentOrder;
import org.ar.manager.entity.BiPaymentOrderMonth;
import org.ar.manager.mapper.BiPaymentOrderMonthMapper;
import org.ar.manager.req.PaymentMonthOrderReportReq;
import org.ar.manager.service.IBiPaymentOrderMonthService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author
 */
@Service
public class BiPaymentOrderMonthServiceImpl extends ServiceImpl<BiPaymentOrderMonthMapper, BiPaymentOrderMonth> implements IBiPaymentOrderMonthService {

    @Override
    @SneakyThrows
    public PageReturn<BiPaymentOrderMonth> listPage(PaymentMonthOrderReportReq req) {

        Page<BiPaymentOrderMonth> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<BiPaymentOrderMonth> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(BiPaymentOrderMonth::getDateTime);

        // 新增统计金额字段总计字段
        // 新增统计金额字段总计字段
        LambdaQueryWrapper<BiPaymentOrderMonth> queryWrapper = new QueryWrapper<BiPaymentOrderMonth>()
                .select("IFNULL(sum(actual_money), 0) as actualMoneyTotal, IFNULL(sum(total_fee), 0) as feeTotal").lambda();


        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiPaymentOrderMonth::getDateTime, req.getStartTime());
            queryWrapper.ge(BiPaymentOrderMonth::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiPaymentOrderMonth::getDateTime, req.getEndTime());
            queryWrapper.le(BiPaymentOrderMonth::getDateTime, req.getEndTime());
        }

        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.eq(BiPaymentOrderMonth::getMerchantCode, req.getMerchantCode());
            queryWrapper.eq(BiPaymentOrderMonth::getMerchantCode, req.getMerchantCode());
        }

        Page<BiPaymentOrderMonth> finalPage = page;
        CompletableFuture<BiPaymentOrderMonth> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<BiPaymentOrderMonth>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));
        CompletableFuture.allOf(totalFuture, resultFuture);
        page = resultFuture.get();
        BiPaymentOrderMonth totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("actualMoneyTotal", totalInfo.getActualMoneyTotal().toPlainString());
        extent.put("feeTotal", totalInfo.getFeeTotal().toPlainString());
        BigDecimal actualMoneyPageTotal = BigDecimal.ZERO;
        BigDecimal feePageTotal = BigDecimal.ZERO;

        List<BiPaymentOrderMonth> records = page.getRecords();
        for (BiPaymentOrderMonth item : records) {
            if(item.getOrderNum() <= 0L){
                item.setSuccessRate(0d);
                item.setAverageFinishDuration(0L);
            }else {
                double result = new BigDecimal(item.getSuccessOrderNum().toString())
                        .divide(new BigDecimal(item.getOrderNum().toString()), 2, RoundingMode.DOWN).doubleValue();
                item.setSuccessRate(result);
                Long  averageFinishDuration = new BigDecimal(item.getFinishDuration()).
                        divide(new BigDecimal(item.getOrderNum().toString()), 2, RoundingMode.DOWN).longValue();
                item.setAverageFinishDuration(averageFinishDuration);
            }
            actualMoneyPageTotal = actualMoneyPageTotal.add(item.getActualMoney());
            feePageTotal = feePageTotal.add(item.getTotalFee());
        }
        extent.put("actualMoneyPageTotal",actualMoneyPageTotal.toPlainString());
        extent.put("feePageTotal", feePageTotal.toPlainString());
        return PageUtils.flush(page, records, extent);
    }

    @Override
    public PageReturn<BiPaymentOrderExportDTO> listPageForExport(PaymentMonthOrderReportReq req) {
        List<BiPaymentOrderExportDTO> list = new ArrayList<>();
        PageReturn<BiPaymentOrderMonth> biPaymentOrderPageReturn = listPage(req);
        List<BiPaymentOrderMonth> data = biPaymentOrderPageReturn.getList();
        for (BiPaymentOrderMonth item : data) {
            BiPaymentOrderExportDTO biPaymentOrderDTO = new BiPaymentOrderExportDTO();
            BeanUtils.copyProperties(item, biPaymentOrderDTO);
            if(item.getSuccessRate() != null){
                double successRate = (item.getSuccessRate() * 100);
                String successRateStr = (int) successRate + "%";
                biPaymentOrderDTO.setSuccessRate(successRateStr);
            }

            String averageFinishDurationStr = item.getAverageFinishDuration().toString();
            String orderCompleteDuration = DurationCalculatorUtil.getOrderCompleteDuration(averageFinishDurationStr);
            biPaymentOrderDTO.setAverageFinishDuration(orderCompleteDuration);
            biPaymentOrderDTO.setActualMoney(item.getActualMoney().toString());
            biPaymentOrderDTO.setTotalFee(item.getTotalFee().toString());
            list.add(biPaymentOrderDTO);
        }
        Page<BiPaymentOrderExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(biPaymentOrderPageReturn.getTotal());
        return PageUtils.flush(page, list);
    }
}
