package org.ar.wallet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.MemberAccountChangeDTO;
import org.ar.common.pay.dto.MemberInfoDTO;
import org.ar.common.pay.req.MemberAccountChangeReq;
import org.ar.common.pay.req.MemberInfoIdGetInfoReq;
import org.ar.wallet.Enum.MemberAccountChangeEnum;
import org.ar.wallet.Enum.MemberTypeEnum;
import org.ar.wallet.config.WalletMapStruct;
import org.ar.wallet.entity.MemberAccountChange;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.mapper.MemberAccountChangeMapper;
import org.ar.wallet.req.ViewTransactionHistoryReq;
import org.ar.wallet.service.IMemberAccountChangeService;
import org.ar.wallet.service.IMemberInfoService;
import org.ar.wallet.vo.ViewTransactionHistoryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
* @author
*/  @RequiredArgsConstructor
    @Service
    @Slf4j
    public class MemberAccountChangeServiceImpl extends ServiceImpl<MemberAccountChangeMapper, MemberAccountChange> implements IMemberAccountChangeService {
    private final WalletMapStruct walletMapStruct;
    private final IMemberInfoService memberInfoService;

    @Override
    @SneakyThrows
    public PageReturn<MemberAccountChangeDTO> listPage(MemberAccountChangeReq req) {
        Page<MemberAccountChange> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MemberAccountChange> lambdaQuery = lambdaQuery();
        // 新增统计金额字段总计字段
        LambdaQueryWrapper<MemberAccountChange> queryWrapper = new QueryWrapper<MemberAccountChange>()
                .select("IFNULL(sum(before_change), 0) as beforeChangeTotal,IFNULL(sum(after_change), 0) as afterChangeTotal," +
                        "IFNULL(sum(amount_change), 0) as amountChangeTotal").lambda();

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getOrderNo())) {
            lambdaQuery.eq(MemberAccountChange::getOrderNo, req.getOrderNo());
            queryWrapper.eq(MemberAccountChange::getOrderNo, req.getOrderNo());
        }
        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getChangeType())) {
            lambdaQuery.eq(MemberAccountChange::getChangeType, req.getChangeType());
            queryWrapper.eq(MemberAccountChange::getChangeType, req.getChangeType());
        }

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getId())) {
            lambdaQuery.eq(MemberAccountChange::getMid, req.getId());
            queryWrapper.eq(MemberAccountChange::getMid, req.getId());
        }

        if (!com.alibaba.nacos.api.utils.StringUtils.isBlank(req.getMerchantOrder())) {
            lambdaQuery.eq(MemberAccountChange::getMerchantOrder, req.getMerchantOrder());
            queryWrapper.eq(MemberAccountChange::getMerchantOrder, req.getMerchantOrder());
        }


        if (!ObjectUtils.isEmpty(req.getAmountChangeStart())) {
            lambdaQuery.ge(MemberAccountChange::getAmountChange, req.getAmountChangeStart());
            queryWrapper.ge(MemberAccountChange::getAmountChange, req.getAmountChangeStart());
        }
        if (!ObjectUtils.isEmpty(req.getAmountChangeEnd())) {
            lambdaQuery.le(MemberAccountChange::getAmountChange, req.getAmountChangeEnd());
            queryWrapper.le(MemberAccountChange::getAmountChange, req.getAmountChangeEnd());
        }

        if (!ObjectUtils.isEmpty(req.getCreateTimeStart())) {
            lambdaQuery.ge(MemberAccountChange::getCreateTime, req.getCreateTimeStart());
            queryWrapper.ge(MemberAccountChange::getCreateTime, req.getCreateTimeStart());
        }
        if (!ObjectUtils.isEmpty(req.getCreateTimeEnd())) {
            lambdaQuery.le(MemberAccountChange::getCreateTime, req.getCreateTimeEnd());
            queryWrapper.le(MemberAccountChange::getCreateTime, req.getCreateTimeEnd());
        }

        // 根据会员id/商户会员id/会员账号筛选
        if (!ObjectUtils.isEmpty(req.getObscureId())) {
            lambdaQuery.and(wq -> wq.eq(MemberAccountChange::getMid, req.getObscureId())
                    .or()
                    .eq(MemberAccountChange::getMemberId, req.getObscureId()))
                    .or()
                    .eq(MemberAccountChange::getMemberAccount, req.getObscureId());
            queryWrapper.and(wq -> wq.eq(MemberAccountChange::getMid, req.getObscureId())
                    .or()
                    .eq(MemberAccountChange::getMemberId, req.getObscureId()))
                    .or()
                    .eq(MemberAccountChange::getMemberAccount, req.getObscureId());
        }

        // 根据平台订单号/商户订单号筛选
        if (!ObjectUtils.isEmpty(req.getObscureOrderNo())) {
            lambdaQuery.eq(MemberAccountChange::getOrderNo, req.getObscureOrderNo())
                    .or().eq(MemberAccountChange::getMerchantOrder, req.getObscureOrderNo());
            queryWrapper.eq(MemberAccountChange::getOrderNo, req.getObscureOrderNo())
                    .or().eq(MemberAccountChange::getMerchantOrder, req.getObscureOrderNo());
        }

        // 根据所属商户筛选
        if (!ObjectUtils.isEmpty(req.getMerchantName())) {
            lambdaQuery.eq(MemberAccountChange::getMerchantName, req.getMerchantName());
            queryWrapper.eq(MemberAccountChange::getMerchantName, req.getMerchantName());
        }
        // 加入排序
        OrderItem orderItem = new OrderItem();
        if(org.apache.commons.lang3.StringUtils.isBlank(req.getColumn())){
            lambdaQuery.orderByDesc(MemberAccountChange::getCreateTime);
        }else {
            orderItem.setColumn(StrUtil.toUnderlineCase(req.getColumn()));
            orderItem.setAsc(req.isAsc());
            page.addOrder(orderItem);
        }

        Page<MemberAccountChange> finalPage = page;
        CompletableFuture<MemberAccountChange> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<MemberAccountChange>> resultFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));
        CompletableFuture.allOf(totalFuture, resultFuture);

        page = resultFuture.get();
        MemberAccountChange totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("afterChangeTotal", totalInfo.getAfterChangeTotal().toPlainString());
        extent.put("beforeChangeTotal", totalInfo.getBeforeChangeTotal().toPlainString());
        extent.put("amountChangeTotal", totalInfo.getAmountChangeTotal().toPlainString());

        BigDecimal afterChangePageTotal = BigDecimal.ZERO;
        BigDecimal beforeChangePageTotal = BigDecimal.ZERO;
        BigDecimal amountChangePageTotal = BigDecimal.ZERO;

        List<MemberAccountChange> records = page.getRecords();
        List<MemberAccountChangeDTO> list = new ArrayList<>();
        for (MemberAccountChange record : records) {
            MemberAccountChangeDTO dto = new MemberAccountChangeDTO();
            BeanUtils.copyProperties(record, dto);
            list.add(dto);
            afterChangePageTotal = afterChangePageTotal.add(record.getAfterChange());
            beforeChangePageTotal = beforeChangePageTotal.add(record.getBeforeChange());
            amountChangePageTotal = amountChangePageTotal.add(record.getAmountChange());
        }
        extent.put("afterChangePageTotal", afterChangePageTotal.toPlainString());
        extent.put("beforeChangePageTotal", beforeChangePageTotal.toPlainString());
        extent.put("amountChangePageTotal", amountChangePageTotal.toPlainString());
        return PageUtils.flush(page, list, extent);
    }


    /**
     * 记录会员账变
     *
     * @param mid             会员ID
     * @param changeAmount    账变金额
     * @param changeType      交易类型
     * @param orderNo         订单号
     * @param previousBalance 账变前余额
     * @param newBalance      账变后余额
     * @param merchantOrder   商户订单号
     * @return {@link Boolean}
     */
    @Override
    public Boolean recordMemberTransaction(String mid, BigDecimal changeAmount, String changeType, String orderNo, BigDecimal previousBalance, BigDecimal newBalance, String merchantOrder) {

        MemberAccountChange memberAccountChange = new MemberAccountChange();

        //设置平台订单号
        memberAccountChange.setOrderNo(orderNo);

        //设置商户订单号
        memberAccountChange.setMerchantOrder(merchantOrder);

        //设置会员ID
        memberAccountChange.setMid(mid);

        //设置交易类型
        memberAccountChange.setChangeType(changeType);

        //设置账变前金额
        memberAccountChange.setBeforeChange(previousBalance);

        //设置账变后金额
        memberAccountChange.setAfterChange(newBalance);

        //设置账变金额
        memberAccountChange.setAmountChange(changeAmount);

        //获取商户会员id/商户名称/会员账号
        MemberInfo memberInfo = getMemberInfo(mid);

        // 设置商户会员id
        memberAccountChange.setMemberId(mid);
        // 非纯钱包用户需要截取商户会员id
        if(!memberInfo.getMemberType().equals(MemberTypeEnum.WALLET_MEMBER.getCode())
                && org.apache.commons.lang3.StringUtils.isNotBlank(memberInfo.getMemberId())
                && org.apache.commons.lang3.StringUtils.isNotBlank(memberInfo.getMerchantCode())){
            String externalMemberId = memberInfo.getMemberId().substring(memberInfo.getMerchantCode().length());
            memberAccountChange.setMemberId(externalMemberId);
        }
        // 设置商户名称
        if(!ObjectUtils.isEmpty(memberInfo.getMerchantName())){
            memberAccountChange.setMerchantName(memberInfo.getMerchantName());
        }

        // 设置会员账号
        if(!ObjectUtils.isEmpty(memberInfo.getMemberAccount())){
            memberAccountChange.setMemberAccount(memberInfo.getMemberAccount());
        }

        boolean save = save(memberAccountChange);

        log.info("记录会员账变, 会员id: {}, 账变金额: {}, 交易类型: {}, 订单号: {}, 账变前余额: {}, 账变后余额: {}, sql执行结果: {}",
                mid, changeAmount, MemberAccountChangeEnum.getNameByCode(changeType), orderNo, previousBalance, newBalance, save);

        return save;
    }

    /**
     * 交易记录
     *
     * @param req
     * @return {@link RestResult}<{@link PageReturn}<{@link ViewTransactionHistoryVo}>>
     */
    @Override
    public RestResult<PageReturn<ViewTransactionHistoryVo>> viewTransactionHistory(ViewTransactionHistoryReq req) {

        if (req == null) {
            req = new ViewTransactionHistoryReq();
        }

        Page<MemberAccountChange> pageCollectionOrder = new Page<>();
        pageCollectionOrder.setCurrent(req.getPageNo());
        pageCollectionOrder.setSize(req.getPageSize());

        LambdaQueryChainWrapper<MemberAccountChange> lambdaQuery = lambdaQuery();

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null){
            log.error("查询交易记录失败: 获取会员信息失败: {}", memberInfo);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //查询当前会员的交易记录
        lambdaQuery.eq(MemberAccountChange::getMid, memberInfo.getId());

        //--动态查询 交易类型
        if (StringUtils.isNotEmpty(req.getTransactionType())) {
            lambdaQuery.eq(MemberAccountChange::getChangeType, req.getTransactionType());
        }else{
            //如果没有传交易类型  那么查询买入 卖出 USDT买入 卖出奖励 支付 到账
            lambdaQuery.in(MemberAccountChange::getChangeType, new String[]{"1", "2", "3", "9", "10", "11", "12", "13"});
        }

        //--动态查询 时间 某天
        if (StringUtils.isNotEmpty(req.getDate())){
            LocalDate localDate = LocalDate.parse(req.getDate());
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.MAX);

            lambdaQuery.ge(MemberAccountChange::getCreateTime, startOfDay);
            lambdaQuery.le(MemberAccountChange::getCreateTime, endOfDay);
        }

        // 倒序排序
        lambdaQuery.orderByDesc(MemberAccountChange::getId);

        baseMapper.selectPage(pageCollectionOrder, lambdaQuery.getWrapper());

        List<MemberAccountChange> records = pageCollectionOrder.getRecords();

        ArrayList<ViewTransactionHistoryVo> viewTransactionHistoryVoList = new ArrayList<>();

        //IPage＜实体＞转 IPage＜Vo＞
        for (MemberAccountChange memberAccountChange : records) {

            ViewTransactionHistoryVo viewTransactionHistoryVo = new ViewTransactionHistoryVo();

            //交易类型
            viewTransactionHistoryVo.setTransactionType(memberAccountChange.getChangeType());

            //时间
            viewTransactionHistoryVo.setCreateTime(memberAccountChange.getCreateTime());

            //金额
            viewTransactionHistoryVo.setAmount(memberAccountChange.getAmountChange());

            //如果交易类型是13(金额错误退回 那么改为 10退回)
            if (MemberAccountChangeEnum.AMOUNT_ERROR.getCode().equals(viewTransactionHistoryVo.getTransactionType())){
                viewTransactionHistoryVo.setTransactionType(MemberAccountChangeEnum.CANCEL_RETURN.getCode());
            }

            viewTransactionHistoryVoList.add(viewTransactionHistoryVo);
        }

        PageReturn<ViewTransactionHistoryVo> flush = PageUtils.flush(pageCollectionOrder, viewTransactionHistoryVoList);

        log.info("查询交易记录成功: 会员账号: {}, req: {}, 返回数据: {}", memberInfo.getMemberAccount(), req, flush);

        return RestResult.ok(flush);
    }

    /**
     * 获取会员信息
     * @param mid 会员id
     * @return {@link MemberInfo}
     */
    public MemberInfo getMemberInfo(String mid){
        return memberInfoService.getMemberInfoById(mid);
    }
}
