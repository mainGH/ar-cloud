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
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.BiMerchantDailyDTO;
import org.ar.common.pay.dto.BiPaymentOrderDTO;
import org.ar.common.web.exception.BizException;
import org.ar.manager.entity.BiMerchantDaily;
import org.ar.manager.entity.BiPaymentOrder;
import org.ar.manager.entity.BiPaymentOrderMonth;
import org.ar.manager.entity.BiWithdrawOrderDaily;
import org.ar.manager.mapper.BiMerchantDailyMapper;
import org.ar.manager.req.MerchantDailyReportReq;
import org.ar.manager.service.IBiMerchantDailyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author
 */
@Service
public class BiMerchantDailyServiceImpl extends ServiceImpl<BiMerchantDailyMapper, BiMerchantDaily> implements IBiMerchantDailyService {

    @Override
    @SneakyThrows
    public PageReturn<BiMerchantDaily> listPage(MerchantDailyReportReq req) {
        Page<BiMerchantDaily> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<BiMerchantDaily> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(BiMerchantDaily::getDateTime);
        // 新增统计金额字段总计字段
        LambdaQueryWrapper<BiMerchantDaily> queryWrapper = new QueryWrapper<BiMerchantDaily>()
                .select("IFNULL(sum(pay_money), 0) as payMoneyTotal, IFNULL(sum(withdraw_money), 0) as withdrawMoneyTotal, " +
                        "IFNULL(sum(total_fee), 0) as feeTotal, IFNULL(sum(difference), 0) as differenceTotal").lambda();

        if (StringUtils.isNotBlank(req.getStartTime())) {
            lambdaQuery.ge(BiMerchantDaily::getDateTime, req.getStartTime());
            queryWrapper.ge(BiMerchantDaily::getDateTime, req.getStartTime());
        }

        if (StringUtils.isNotBlank(req.getEndTime())) {
            lambdaQuery.le(BiMerchantDaily::getDateTime, req.getEndTime());
            queryWrapper.le(BiMerchantDaily::getDateTime, req.getEndTime());
        }

        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.eq(BiMerchantDaily::getMerchantCode, req.getMerchantCode());
            queryWrapper.eq(BiMerchantDaily::getMerchantCode, req.getMerchantCode());
        }
        Page<BiMerchantDaily> finalPage = page;
        CompletableFuture<BiMerchantDaily> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<BiMerchantDaily>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));
        CompletableFuture.allOf(totalFuture, resultFuture);
        page = resultFuture.get();
        BiMerchantDaily totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("payMoneyTotal", totalInfo.getPayMoneyTotal().toPlainString());
        extent.put("withdrawMoneyTotal", totalInfo.getWithdrawMoneyTotal().toPlainString());
        extent.put("feeTotal", totalInfo.getFeeTotal().toPlainString());
        extent.put("differenceTotal", totalInfo.getDifferenceTotal().toPlainString());
        BigDecimal payMoneyPageTotal = BigDecimal.ZERO;
        BigDecimal withdrawMoneyPageTotal = BigDecimal.ZERO;
        BigDecimal feePageTotal = BigDecimal.ZERO;
        BigDecimal differencePageTotal = BigDecimal.ZERO;
        List<BiMerchantDaily> records = page.getRecords();
        for (BiMerchantDaily item : records) {
            item.setDifference(item.getWithdrawMoney().subtract(item.getPayMoney()));
            if(item.getPayOrderNum() <= 0L){
                item.setPaySuccessRate(0d);
            }else {
                double payRate = new BigDecimal(item.getPaySuccessOrderNum().toString())
                        .divide(new BigDecimal(item.getPayOrderNum().toString()), 2, RoundingMode.DOWN).doubleValue();
                item.setPaySuccessRate(payRate);

            }

            if(item.getWithdrawOrderNum() <= 0L){
                item.setWithdrawSuccessRate(0d);
            }else {
                double withdrawRate = new BigDecimal(item.getWithdrawSuccessOrderNum().toString())
                        .divide(new BigDecimal(item.getWithdrawOrderNum().toString()), 2, RoundingMode.DOWN).doubleValue();
                item.setWithdrawSuccessRate(withdrawRate);
            }
            payMoneyPageTotal = payMoneyPageTotal.add(item.getPayMoney());
            withdrawMoneyPageTotal = withdrawMoneyPageTotal.add(item.getWithdrawMoney());
            feePageTotal = feePageTotal.add(item.getTotalFee());
            differencePageTotal = differencePageTotal.add(item.getDifference());
        }
        extent.put("payMoneyPageTotal", payMoneyPageTotal.toPlainString());
        extent.put("withdrawMoneyPageTotal", withdrawMoneyPageTotal.toPlainString());
        extent.put("feePageTotal", feePageTotal.toPlainString());
        extent.put("differencePageTotal", differencePageTotal.toPlainString());
        return PageUtils.flush(page, records, extent);
    }

    @Override
    public PageReturn<BiMerchantDailyDTO> listPageForExport(MerchantDailyReportReq req) {
        List<BiMerchantDailyDTO> list = new ArrayList<>();
        PageReturn<BiMerchantDaily> biPaymentOrderPageReturn = listPage(req);
        List<BiMerchantDaily> data = biPaymentOrderPageReturn.getList();
        for (BiMerchantDaily item : data) {
            BiMerchantDailyDTO biMerchantDailyDTO = new BiMerchantDailyDTO();
            BeanUtils.copyProperties(item, biMerchantDailyDTO);
            String type = "内部商户";
            if(biMerchantDailyDTO.getMerchantType().equals("2")){
                type = "外部商户";
            }
            biMerchantDailyDTO.setMerchantType(type);

            biMerchantDailyDTO.setPayMoney(item.getPayMoney().toString());
            biMerchantDailyDTO.setWithdrawMoney(item.getWithdrawMoney().toString());
            if(item.getPaySuccessRate() != null){
                double successRateD = (item.getPaySuccessRate() * 100);
                String paySuccessRateStr = (int) successRateD + "%";
                biMerchantDailyDTO.setPaySuccessRate(paySuccessRateStr);
            }
            if(item.getWithdrawSuccessRate() != null){
                double successRateW = (item.getWithdrawSuccessRate() * 100);
                String withdrawSuccessRateStr = (int)successRateW + "%";
                biMerchantDailyDTO.setWithdrawSuccessRate(withdrawSuccessRateStr);
            }
            biMerchantDailyDTO.setDifference(item.getDifference().toString());
            biMerchantDailyDTO.setTotalFee(item.getTotalFee().toString());

            list.add(biMerchantDailyDTO);
        }
        Page<BiMerchantDailyDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(biPaymentOrderPageReturn.getTotal());
        return PageUtils.flush(page, list);
    }
}
