package org.ar.wallet.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.utils.StringUtils;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.ApplyDistributedDTO;
import org.ar.common.pay.dto.MatchingOrderDTO;
import org.ar.common.pay.req.ApplyDistributedListPageReq;
import org.ar.common.redis.util.RedisUtils;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.wallet.Enum.AccountChangeEnum;
import org.ar.wallet.Enum.ChangeModeEnum;
import org.ar.wallet.Enum.DistributeddStatusEnum;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.ApplyDistributed;
import org.ar.wallet.entity.MatchingOrder;
import org.ar.wallet.mapper.AccountChangeMapper;
import org.ar.wallet.mapper.ApplyDistributedMapper;
import org.ar.wallet.mapper.MerchantInfoMapper;
import org.ar.wallet.req.ApplyDistributedReq;
import org.ar.wallet.service.IApplyDistributedService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.wallet.service.ICollectionOrderService;
import org.ar.wallet.util.AmountChangeUtil;
import org.ar.wallet.vo.AccountChangeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Service
    @RequiredArgsConstructor
    public class ApplyDistributedServiceImpl extends ServiceImpl<ApplyDistributedMapper, ApplyDistributed> implements IApplyDistributedService {
        private final WalletMapStruct walletMapStruct;
        private final AmountChangeUtil amountChangeUtil;

    @Override
    @SneakyThrows
    public PageReturn<ApplyDistributedDTO> listPage(ApplyDistributedListPageReq req) {
        Page<ApplyDistributed> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<ApplyDistributed> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(ApplyDistributed::getStatus,DistributeddStatusEnum.NOFISHED.getCode());
        // 新增统计金额字段总计字段
        LambdaQueryWrapper<ApplyDistributed> queryWrapper = new QueryWrapper<ApplyDistributed>()
                .select("IFNULL(sum(amount), 0) as amountTotal, IFNULL(sum(balance), 0) as balanceTotal").lambda();
        queryWrapper.eq(ApplyDistributed::getStatus,DistributeddStatusEnum.NOFISHED.getCode());

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUsername())) {
            lambdaQuery.eq(ApplyDistributed::getUsername, req.getUsername());
            queryWrapper.eq(ApplyDistributed::getUsername, req.getUsername());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getOrderNo())) {
            lambdaQuery.eq(ApplyDistributed::getOrderNo, req.getOrderNo());
            queryWrapper.eq(ApplyDistributed::getOrderNo, req.getOrderNo());
        }

        if (!ObjectUtils.isEmpty(req.getStartTime())) {
            lambdaQuery.ge(ApplyDistributed::getCreateTime, req.getStartTime());
            queryWrapper.ge(ApplyDistributed::getCreateTime, req.getStartTime());
        }

        //--动态查询 结束时间
        if (!ObjectUtils.isEmpty(req.getEndTime())) {
            lambdaQuery.le(ApplyDistributed::getCreateTime,  req.getEndTime());
            queryWrapper.le(ApplyDistributed::getCreateTime,  req.getEndTime());
        }
        lambdaQuery.orderByDesc(ApplyDistributed::getId);
        Page<ApplyDistributed> finalPage = page;
        CompletableFuture<ApplyDistributed> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<ApplyDistributed>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));
        CompletableFuture.allOf(totalFuture, resultFuture);
        page = resultFuture.get();
        ApplyDistributed totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("amountTotal", totalInfo.getAmountTotal().toPlainString());
        extent.put("balanceTotal", totalInfo.getBalanceTotal().toPlainString());
        BigDecimal amountPageTotal = BigDecimal.ZERO;
        BigDecimal balancePageTotal = BigDecimal.ZERO;
        List<ApplyDistributedDTO> accountChangeVos = new ArrayList<>();
        List<ApplyDistributed> records = page.getRecords();
        for (ApplyDistributed record : records) {
            ApplyDistributedDTO dto = new ApplyDistributedDTO();
            BeanUtils.copyProperties(record, dto);
            accountChangeVos.add(dto);
            amountPageTotal = amountPageTotal.add(record.getAmount());
            balancePageTotal = balancePageTotal.add(record.getBalance());
        }
        extent.put("amountPageTotal", amountPageTotal.toPlainString());
        extent.put("balancePageTotal", balancePageTotal.toPlainString());

        return PageUtils.flush(page, accountChangeVos, extent);
    }



        @Override
        public ApplyDistributedDTO listRecordTotal(ApplyDistributedListPageReq req) {


            QueryWrapper<ApplyDistributed> queryWrapper = new QueryWrapper<>();

            queryWrapper.select(
                    "sum(balance) as balance",
                    "sum(amount) as amount"

            );

            if(!StringUtils.isEmpty(req.getUsername())){
                queryWrapper.eq("username",req.getUsername());
            }

            if(!StringUtils.isEmpty(req.getOrderNo())){
                queryWrapper.eq("order_no",req.getOrderNo());
            }
            queryWrapper.eq("status",DistributeddStatusEnum.FINISHED.getCode());


            if(!StringUtils.isEmpty(req.getStartTime())){
                queryWrapper.ge("create_time",req.getStartTime());
            }
            if(!StringUtils.isEmpty(req.getEndTime())){
                queryWrapper.le("create_time",req.getEndTime());
            }

            Page<Map<String,Object>> page = new Page<>();
            page.setCurrent(req.getPageNo());
            page.setSize(req.getPageSize());
            baseMapper.selectMapsPage(page,queryWrapper);
            List<Map<String,Object>> records = page.getRecords();
            JSONArray jsonArray = new JSONArray();
            jsonArray.addAll(records);
            List<ApplyDistributedDTO> list = jsonArray.toJavaList(ApplyDistributedDTO.class);
            ApplyDistributedDTO matchingOrderDTO = list.get(0);

            return matchingOrderDTO;
        }




        @Override
        @SneakyThrows
        public PageReturn<ApplyDistributedDTO> listRecordPage(ApplyDistributedListPageReq req) {
            Page<ApplyDistributed> page = new Page<>();
            page.setCurrent(req.getPageNo());
            page.setSize(req.getPageSize());
            LambdaQueryChainWrapper<ApplyDistributed> lambdaQuery = lambdaQuery();
            lambdaQuery.orderByDesc(ApplyDistributed::getCreateTime);
            // 新增统计金额字段总计字段
            LambdaQueryWrapper<ApplyDistributed> queryWrapper = new QueryWrapper<ApplyDistributed>()
                    .select("IFNULL(sum(amount), 0) as amountTotal, IFNULL(sum(balance), 0) as balanceTotal").lambda();

            if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getUsername())) {
                lambdaQuery.eq(ApplyDistributed::getUsername, req.getUsername());
                queryWrapper.eq(ApplyDistributed::getUsername, req.getUsername());
            }
            if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getOrderNo())) {
                lambdaQuery.eq(ApplyDistributed::getOrderNo, req.getOrderNo());
                queryWrapper.eq(ApplyDistributed::getOrderNo, req.getOrderNo());
            }

            if (req.getStartTime() != null) {
                lambdaQuery.ge(ApplyDistributed::getCreateTime, req.getStartTime());
                queryWrapper.ge(ApplyDistributed::getCreateTime, req.getStartTime());
            }

            //--动态查询 结束时间
            if (req.getEndTime() != null) {
                lambdaQuery.le(ApplyDistributed::getCreateTime,  req.getEndTime());
                queryWrapper.le(ApplyDistributed::getCreateTime,  req.getEndTime());
            }

            Page<ApplyDistributed> finalPage = page;
            CompletableFuture<ApplyDistributed> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
            CompletableFuture<Page<ApplyDistributed>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));
            CompletableFuture.allOf(totalFuture, resultFuture);
            page = resultFuture.get();
            ApplyDistributed totalInfo = totalFuture.get();
            JSONObject extent = new JSONObject();
            extent.put("amountTotal", totalInfo.getAmountTotal().toPlainString());
            extent.put("balanceTotal", totalInfo.getBalanceTotal().toPlainString());
            BigDecimal amountPageTotal = BigDecimal.ZERO;
            BigDecimal balancePageTotal = BigDecimal.ZERO;
            List<ApplyDistributedDTO> accountChangeVos = new ArrayList<>();
            List<ApplyDistributed> records = page.getRecords();
            for (ApplyDistributed record : records) {
                ApplyDistributedDTO dto = new ApplyDistributedDTO();
                BeanUtils.copyProperties(record, dto);
                accountChangeVos.add(dto);
                amountPageTotal = amountPageTotal.add(record.getAmount());
                balancePageTotal = balancePageTotal.add(record.getBalance());
            }
            extent.put("amountPageTotal", amountPageTotal.toPlainString());
            extent.put("balancePageTotal", balancePageTotal.toPlainString());
//            List<ApplyDistributedDTO> accountChangeVos = walletMapStruct.ApplyDistributedTransform(records);
            return PageUtils.flush(page, accountChangeVos, extent);
        }




        @Override
        @Transactional(rollbackFor = Exception.class)
        public ApplyDistributedDTO  distributed(ApplyDistributed applyDistributed){
            applyDistributed.setStatus(DistributeddStatusEnum.FINISHED.getCode());
            String time = DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd");
            baseMapper.updateById(applyDistributed);
            ApplyDistributedDTO applyDistributedDTO = new ApplyDistributedDTO();
            amountChangeUtil.insertChangeAmountRecord(applyDistributed.getMerchantCode(), applyDistributed.getAmount(), ChangeModeEnum.SUB, applyDistributed.getCurrence(),
                    applyDistributed.getOrderNo(), AccountChangeEnum.WITHDRAW, time,applyDistributed.getRemark(), "");
            BeanUtils.copyProperties(applyDistributed,applyDistributedDTO);
            return applyDistributedDTO;

        }


    }
