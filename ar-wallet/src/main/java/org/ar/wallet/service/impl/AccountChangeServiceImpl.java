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
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.req.AccountChangeReq;
import org.ar.common.web.exception.BizException;
import org.ar.wallet.Enum.AccountChangeEnum;
import org.ar.wallet.Enum.NotifyStatusEnum;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.AccountChange;
import org.ar.wallet.mapper.AccountChangeMapper;

import org.ar.wallet.service.IAccountChangeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.ar.wallet.vo.AccountChangeVo;
import org.h2.util.DateTimeUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author
 */
@Service
@RequiredArgsConstructor
public class AccountChangeServiceImpl extends ServiceImpl<AccountChangeMapper, AccountChange> implements IAccountChangeService {

    private final WalletMapStruct walletMapStruct;

    @Override
    @SneakyThrows
    public PageReturn<AccountChangeVo> queryAccountChangeList(AccountChangeReq req) {

        Page<AccountChange> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        // 新增统计金额字段总计字段
        LambdaQueryWrapper<AccountChange> queryWrapper = new QueryWrapper<AccountChange>()
                .select("IFNULL(sum(before_change),0) as beforeChangeTotal,IFNULL(sum(after_change), 0) as afterChangeTotal,IFNULL(sum(amount_change), 0) as amountChangeTotal,IFNULL(sum(commission), 0) as commissionTotal").lambda();


        LambdaQueryChainWrapper<AccountChange> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(AccountChange::getCreateTime);

        if (!ObjectUtils.isEmpty(req.getChangeType())) {
            lambdaQuery.eq(AccountChange::getChangeType, req.getChangeType());
            queryWrapper.eq(AccountChange::getChangeType, req.getChangeType());
        }

        if (StringUtils.isNotBlank(req.getOrderNo())) {
            lambdaQuery.eq(AccountChange::getOrderNo, req.getOrderNo());
            queryWrapper.eq(AccountChange::getOrderNo, req.getOrderNo());
        }

        if (StringUtils.isNotBlank(req.getMerchantName())) {
            lambdaQuery.eq(AccountChange::getMerchantName, req.getMerchantName());
            queryWrapper.eq(AccountChange::getMerchantName, req.getMerchantName());
        }

        if (ObjectUtils.isNotEmpty(req.getStartTime())) {
            lambdaQuery.ge(AccountChange::getCreateTime, req.getStartTime());
            queryWrapper.ge(AccountChange::getCreateTime, req.getStartTime());
        }

        if (ObjectUtils.isNotEmpty(req.getEndTime())) {
            lambdaQuery.le(AccountChange::getCreateTime, req.getEndTime());
            queryWrapper.le(AccountChange::getCreateTime, req.getEndTime());
        }

        if (StringUtils.isNotBlank(req.getMerchantCode())) {
            lambdaQuery.eq(AccountChange::getMerchantCode, req.getMerchantCode());
            queryWrapper.eq(AccountChange::getMerchantCode, req.getMerchantCode());
        }
        lambdaQuery.orderByDesc(AccountChange::getId);

        Page<AccountChange> finalPage = page;
        CompletableFuture<AccountChange> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<AccountChange>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));

        page = resultFuture.get();
        AccountChange totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("beforeChangeTotal", totalInfo.getBeforeChangeTotal());
        extent.put("afterChangeTotal", totalInfo.getAfterChangeTotal());
        extent.put("amountChangeTotal", totalInfo.getAmountChangeTotal());
        extent.put("commissionTotal", totalInfo.getCommissionTotal());
        BigDecimal beforeChangePageTotal = BigDecimal.ZERO;
        BigDecimal afterChangePageTotal = BigDecimal.ZERO;
        BigDecimal amountChangePageTotal = BigDecimal.ZERO;
        BigDecimal commissionPageTotal = BigDecimal.ZERO;
        List<AccountChange> records = page.getRecords();
        for (AccountChange record : records) {
            beforeChangePageTotal = beforeChangePageTotal.add(record.getBeforeChange());
            afterChangePageTotal = afterChangePageTotal.add(record.getAfterChange());
            amountChangePageTotal = amountChangePageTotal.add(record.getAmountChange());
            commissionPageTotal = commissionPageTotal.add(record.getCommission());
        }
        List<AccountChangeVo> accountChangeVos = walletMapStruct.AccountChangeTransform(records);
        extent.put("beforeChangePageTotal", beforeChangePageTotal);
        extent.put("afterChangePageTotal", afterChangePageTotal);
        extent.put("amountChangePageTotal", amountChangePageTotal);
        extent.put("commissionPageTotal", commissionPageTotal);
        return PageUtils.flush(page, accountChangeVos, extent);
    }

    @Override
    public Map<Integer, String> fetchAccountType() {

        Map<Integer, String> map = new HashMap<>(10);
        map.put(Integer.parseInt(AccountChangeEnum.COLLECTION.getCode()), AccountChangeEnum.COLLECTION.getName());
        map.put(Integer.parseInt(AccountChangeEnum.PAYMENT.getCode()), AccountChangeEnum.PAYMENT.getName());
        map.put(Integer.parseInt(AccountChangeEnum.RECHARGE.getCode()), AccountChangeEnum.RECHARGE.getName());
        map.put(Integer.parseInt(AccountChangeEnum.WITHDRAW.getCode()), AccountChangeEnum.WITHDRAW.getName());
        map.put(Integer.parseInt(AccountChangeEnum.COLLECTION_FEE.getCode()), AccountChangeEnum.COLLECTION_FEE.getName());
        map.put(Integer.parseInt(AccountChangeEnum.PAYMENT_FEE.getCode()), AccountChangeEnum.PAYMENT_FEE.getName());
        map.put(Integer.parseInt(AccountChangeEnum.WITHDRAW_BACK.getCode()), AccountChangeEnum.WITHDRAW_BACK.getName());
        return map;
    }


    @Override
    public AccountChangeVo queryTotal(AccountChangeReq req){
        QueryWrapper<AccountChange> queryWrapper = new QueryWrapper<>();

        queryWrapper.select(
                "sum(before_change) as beforeChange",
                "sum(after_change) as afterChange",
                "sum(amount_change) as amountChange"

        );

        if (!ObjectUtils.isEmpty(req.getChangeType())) {
            queryWrapper.eq("change_type", req.getChangeType());
        }

        if (StringUtils.isNotBlank(req.getOrderNo())) {
            queryWrapper.eq("order_no", req.getOrderNo());
        }

        if (req.getStartTime()!=null) {
            queryWrapper.ge("create_time", req.getStartTime());
        }

        if (req.getEndTime()!=null) {
            queryWrapper.le("create_time", req.getEndTime());
        }
        Page<Map<String,Object>> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        baseMapper.selectMapsPage(page,queryWrapper);
        List<Map<String,Object>> records = page.getRecords();
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(records);
        List<AccountChangeVo> list = jsonArray.toJavaList(AccountChangeVo.class);
        AccountChangeVo accountChangeVo = list.get(0);

        return accountChangeVo;
    }
}
