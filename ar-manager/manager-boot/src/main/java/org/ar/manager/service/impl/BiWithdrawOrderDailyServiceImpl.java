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
import org.ar.common.pay.dto.BiWithdrawOrderDailyExportDTO;
import org.ar.common.pay.dto.MemberOrderOverviewDTO;
import org.ar.common.pay.dto.MerchantOrderOverviewDTO;
import org.ar.common.pay.dto.OrderStatusOverviewDTO;
import org.ar.common.pay.req.CommonDateLimitReq;
import org.ar.common.pay.req.MemberInfoIdReq;
import org.ar.manager.entity.BiMerchantPayOrderDaily;
import org.ar.manager.entity.BiPaymentOrder;
import org.ar.manager.entity.BiWithdrawOrderDaily;
import org.ar.manager.mapper.BiWithdrawOrderDailyMapper;
import org.ar.manager.req.WithdrawDailyOrderReportReq;
import org.ar.manager.service.IBiWithdrawOrderDailyService;
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
public class BiWithdrawOrderDailyServiceImpl extends ServiceImpl<BiWithdrawOrderDailyMapper, BiWithdrawOrderDaily> implements IBiWithdrawOrderDailyService {

    @Override
    @SneakyThrows
    public PageReturn<BiWithdrawOrderDaily> listPage(WithdrawDailyOrderReportReq req) {
        Page<BiWithdrawOrderDaily> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<BiWithdrawOrderDaily> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(BiWithdrawOrderDaily::getDateTime);
        // 新增统计金额字段总计字段
        LambdaQueryWrapper<BiWithdrawOrderDaily> queryWrapper = new QueryWrapper<BiWithdrawOrderDaily>()
                .select("IFNULL(sum(actual_money), 0) as actualMoneyTotal, IFNULL(sum(total_fee), 0) as feeTotal").lambda();


        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiWithdrawOrderDaily::getDateTime, req.getStartTime());
            queryWrapper.ge(BiWithdrawOrderDaily::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiWithdrawOrderDaily::getDateTime, req.getEndTime());
            queryWrapper.le(BiWithdrawOrderDaily::getDateTime, req.getEndTime());
        }

        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.eq(BiWithdrawOrderDaily::getMerchantCode, req.getMerchantCode());
            queryWrapper.eq(BiWithdrawOrderDaily::getMerchantCode, req.getMerchantCode());
        }
        Page<BiWithdrawOrderDaily> finalPage = page;
        CompletableFuture<BiWithdrawOrderDaily> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<BiWithdrawOrderDaily>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));
        CompletableFuture.allOf(totalFuture, resultFuture);
        page = resultFuture.get();
        BiWithdrawOrderDaily totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("actualMoneyTotal", totalInfo.getActualMoneyTotal().toPlainString());
        extent.put("feeTotal", totalInfo.getFeeTotal().toPlainString());
        BigDecimal actualMoneyPageTotal = BigDecimal.ZERO;
        BigDecimal feePageTotal = BigDecimal.ZERO;
        List<BiWithdrawOrderDaily> records = page.getRecords();
        for (BiWithdrawOrderDaily item : records) {

            if(item.getOrderNum() <= 0L){
                item.setSuccessRate(0d);
                item.setAverageFinishDuration(0L);
                item.setAverageMatchDuration(0L);
            }else {
                double result = new BigDecimal(item.getSuccessOrderNum().toString())
                        .divide(new BigDecimal(item.getOrderNum().toString()), 2, RoundingMode.DOWN).doubleValue();
                item.setSuccessRate(result);
                Long  averageFinishDuration = new BigDecimal(item.getFinishDuration()).
                        divide(new BigDecimal(item.getOrderNum().toString()), 2, RoundingMode.DOWN).longValue();
                item.setAverageFinishDuration(averageFinishDuration);
                Long  averageMatchDuration = new BigDecimal(item.getMatchDuration()).
                        divide(new BigDecimal(item.getOrderNum().toString()), 2, RoundingMode.DOWN).longValue();
                item.setAverageMatchDuration(averageMatchDuration);
            }
            actualMoneyPageTotal = actualMoneyPageTotal.add(item.getActualMoney());
            feePageTotal = feePageTotal.add(item.getTotalFee());
        }
        extent.put("actualMoneyPageTotal",actualMoneyPageTotal.toPlainString());
        extent.put("feePageTotal", feePageTotal.toPlainString());
        return PageUtils.flush(page, records, extent);
    }

    @Override
    public PageReturn<BiWithdrawOrderDailyExportDTO> listPageForExport(WithdrawDailyOrderReportReq req) {
        List<BiWithdrawOrderDailyExportDTO> list = new ArrayList<>();
        PageReturn<BiWithdrawOrderDaily> biWithdrawOrderDailyPageReturn = listPage(req);
        List<BiWithdrawOrderDaily> data = biWithdrawOrderDailyPageReturn.getList();
        for (BiWithdrawOrderDaily item : data) {
            BiWithdrawOrderDailyExportDTO biWithdrawOrderDailyExportDTO = new BiWithdrawOrderDailyExportDTO();
            BeanUtils.copyProperties(item, biWithdrawOrderDailyExportDTO);
            if(item.getSuccessRate() != null){
                double successRateD = (item.getSuccessRate() * 100);
                String successRateStr = (int) successRateD + "%";
                biWithdrawOrderDailyExportDTO.setSuccessRate(successRateStr);
            }
            String orderCompleteDuration = "";
            if(item.getAverageFinishDuration() != null && item.getAverageFinishDuration() != 0){
                orderCompleteDuration = DurationCalculatorUtil.getOrderCompleteDuration(item.getAverageFinishDuration().toString());
            }
            biWithdrawOrderDailyExportDTO.setAverageFinishDuration(orderCompleteDuration);
            biWithdrawOrderDailyExportDTO.setActualMoney(item.getActualMoney().toString());
            biWithdrawOrderDailyExportDTO.setTotalFee(item.getTotalFee().toString());
            list.add(biWithdrawOrderDailyExportDTO);
        }
        Page<BiWithdrawOrderDailyExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(biWithdrawOrderDailyPageReturn.getTotal());
        return PageUtils.flush(page, list);
    }

    @Override
    public BiWithdrawOrderDaily getWithdrawOrderStatusOverview(CommonDateLimitReq req) {
        LambdaQueryWrapper<BiWithdrawOrderDaily> lambdaQuery = new QueryWrapper<BiWithdrawOrderDaily>()
                .select("IFNULL(sum(over_time_num),0) as overTimeNumTotal," +
                        "IFNULL(sum(confirm_over_time),0) as confirmOverTimeTotal," +
                        "IFNULL(sum(appeal_success),0) as appealSuccessTotal," +
                        "IFNULL(sum(appeal_fail),0) as appealFailTotal," +
                        "IFNULL(sum(amount_error),0) as amountErrorTotal," +
                        "IFNULL(sum(cancel),0) as cancelOrderTotal," +
                        "IFNULL(sum(success_order_num),0) as successOrderNumTotal,IFNULL(sum(order_num),0) as orderNumTotal")
                .lambda();
        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiWithdrawOrderDaily::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiWithdrawOrderDaily::getDateTime, req.getEndTime());
        }
        return baseMapper.selectOne(lambdaQuery);
    }

    @Override
    public MemberOrderOverviewDTO getMemberOrderOverview(CommonDateLimitReq req) {
        LambdaQueryWrapper<BiWithdrawOrderDaily> lambdaQuery = new QueryWrapper<BiWithdrawOrderDaily>()
                .select("IFNULL(sum(actual_money), 0) as memberWithdrawAmount," +
                        "       IFNULL(sum(success_order_num), 0) as memberWithdrawTransNum")
                .lambda();
        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiWithdrawOrderDaily::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiWithdrawOrderDaily::getDateTime, req.getEndTime());
        }
        BiWithdrawOrderDaily biWithdrawOrderDaily = baseMapper.selectOne(lambdaQuery);
        MemberOrderOverviewDTO result = new MemberOrderOverviewDTO();
        result.setMemberWithdrawAmount(biWithdrawOrderDaily.getMemberWithdrawAmount());
        result.setMemberWithdrawTransNum(biWithdrawOrderDaily.getMemberWithdrawTransNum());
        return result;
    }
}
