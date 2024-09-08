package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
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
import org.ar.common.core.utils.DurationCalculatorUtil;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.mybatis.util.SpringContextUtil;
import org.ar.common.pay.dto.CollectionOrderDTO;
import org.ar.common.pay.dto.CollectionOrderExportDTO;
import org.ar.common.pay.req.CollectionOrderIdReq;
import org.ar.common.pay.req.CollectionOrderListPageReq;
import org.ar.common.web.exception.BizException;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.CollectionOrderMapper;
import org.ar.wallet.mapper.MatchingOrderMapper;
import org.ar.wallet.req.BuyOrderListReq;
import org.ar.wallet.req.ProcessingOrderListReq;
import org.ar.wallet.req.PlatformOrderReq;
import org.ar.wallet.service.*;
import org.ar.wallet.util.*;
import org.ar.wallet.vo.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Admin
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CollectionOrderServiceImpl extends ServiceImpl<CollectionOrderMapper, CollectionOrder> implements ICollectionOrderService {

    @Autowired
    private IMerchantInfoService merchantInfoService;

    @Autowired
    private IPaymentOrderService paymentOrderService;

    @Autowired
    private IMatchingOrderService matchingOrderService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private MatchingOrderMapper matchingOrderMapper;


    @Autowired
    private AmountChangeUtil amountChangeUtil;
    //private final ISellService sellService;

    @Autowired
    private IMemberInfoService memberInfoService;
    @Autowired
    private ITradeConfigService tradeConfigService;


    /**
     * 查询买入订单列表
     *
     * @param req
     * @return {@link RestResult}<{@link List}<{@link BuyOrderListVo}>>
     */
    @Override
    public RestResult<PageReturn<BuyOrderListVo>> buyOrderList(BuyOrderListReq req) {

        if (req == null) {
            req = new BuyOrderListReq();
        }

        Page<CollectionOrder> pageCollectionOrder = new Page<>();
        pageCollectionOrder.setCurrent(req.getPageNo());
        pageCollectionOrder.setSize(req.getPageSize());

        LambdaQueryChainWrapper<CollectionOrder> lambdaQuery = lambdaQuery();

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null){
            log.error("查询买入订单列表失败: 获取会员信息失败: {}", memberInfo);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        //查询当前会员的买入订单
        lambdaQuery.eq(CollectionOrder::getMemberId, memberInfo.getId());

        //--动态查询 订单状态
        if (!StringUtils.isEmpty(req.getOrderStatus())) {

            //对手动完成和已完成做兼容处理
            if (OrderStatusEnum.MANUAL_COMPLETION.getCode().equals(req.getOrderStatus()) || OrderStatusEnum.SUCCESS.getCode().equals(req.getOrderStatus())){
                lambdaQuery.nested(i -> i.eq(CollectionOrder::getOrderStatus, OrderStatusEnum.MANUAL_COMPLETION.getCode())
                        .or()
                        .eq(CollectionOrder::getOrderStatus, OrderStatusEnum.SUCCESS.getCode()));
            }else{
                lambdaQuery.eq(CollectionOrder::getOrderStatus, req.getOrderStatus());
            }
        }


        //--动态查询 时间 某天
        if (StringUtils.isNotEmpty(req.getDate())){
            LocalDate localDate = LocalDate.parse(req.getDate());
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.MAX);

            lambdaQuery.ge(CollectionOrder::getCreateTime, startOfDay);
            lambdaQuery.le(CollectionOrder::getCreateTime, endOfDay);
        }

        // 倒序排序
        lambdaQuery.orderByDesc(CollectionOrder::getId);

        baseMapper.selectPage(pageCollectionOrder, lambdaQuery.getWrapper());

        List<CollectionOrder> records = pageCollectionOrder.getRecords();

        PageReturn<CollectionOrder> flush = PageUtils.flush(pageCollectionOrder, records);

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<BuyOrderListVo> buyOrderListVoList = new ArrayList<>();

        for (CollectionOrder collectionOrder : flush.getList()) {

            BuyOrderListVo buyOrderListVo = new BuyOrderListVo();
            BeanUtil.copyProperties(collectionOrder, buyOrderListVo);

            //设置待支付剩余时间
            buyOrderListVo.setPaymentExpireTime(redisUtil.getPaymentRemainingTime(buyOrderListVo.getPlatformOrder()));

            //设置确认中 剩余时间
            buyOrderListVo.setConfirmExpireTime(redisUtil.getConfirmRemainingTime(buyOrderListVo.getPlatformOrder()));

            //设置是否经过申诉
            if (collectionOrder.getAppealTime() != null){
                buyOrderListVo.setIsAppealed(1);
            }

            //判断如果订单是确认中状态, 但是确认剩余时间低于0 那么将返回前端的订单状态改为确认超时
            if (buyOrderListVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) && (buyOrderListVo.getConfirmExpireTime() == null || buyOrderListVo.getConfirmExpireTime() < 1)){
                buyOrderListVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
            }

            //判断如果订单是支付中状态, 但是支付剩余时间低于0 那么将返回前端的订单状态改为支付超时
            if (buyOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode()) && (buyOrderListVo.getPaymentExpireTime() == null || buyOrderListVo.getPaymentExpireTime() < 1)){
                buyOrderListVo.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());
            }
            // 人工审核状态
            if (OrderStatusEnum.isAuditing(buyOrderListVo.getOrderStatus(), buyOrderListVo.getAuditDelayTime())) {
                buyOrderListVo.setOrderStatus(OrderStatusEnum.AUDITING.getCode());
            }

            //如果是手动完成状态, 改为已完成状态
            if (buyOrderListVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())){
                buyOrderListVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            }

            buyOrderListVoList.add(buyOrderListVo);
        }

        PageReturn<BuyOrderListVo> buyOrderListVoPageReturn = new PageReturn<>();
        buyOrderListVoPageReturn.setPageNo(flush.getPageNo());
        buyOrderListVoPageReturn.setPageSize(flush.getPageSize());
        buyOrderListVoPageReturn.setTotal(flush.getTotal());
        buyOrderListVoPageReturn.setList(buyOrderListVoList);

        log.info("获取买入订单列表成功: 会员账号: {}, req: {}, 返回数据: {}", memberInfo.getMemberAccount(), req, buyOrderListVoPageReturn);

        return RestResult.ok(buyOrderListVoPageReturn);
    }

    @Override
    public List<CollectionOrder> processingBuyOrderList(Long memberId) {
        List<String> processStatusList = getProcessStatus();
        //获取当前会员信息
        LambdaQueryChainWrapper<CollectionOrder> queryChainWrapper = lambdaQuery();
        queryChainWrapper.eq(CollectionOrder::getMemberId, memberId).in(CollectionOrder::getOrderStatus, processStatusList);
        queryChainWrapper.orderByDesc(CollectionOrder::getCreateTime);
        return baseMapper.selectList(queryChainWrapper.getWrapper());
    }

    @Override
    public List<CollectionOrder> processingBuyOrderList(List<String> platformOrderList) {
        if(ObjectUtils.isEmpty(platformOrderList)){
            return Collections.emptyList();
        }
        // 获取进行中的买入订单信息
        List<String> collectionPlatformOrderList = platformOrderList.stream().filter(p -> p.startsWith("MR")).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(collectionPlatformOrderList)){
            return Collections.emptyList();
        }
        LambdaQueryChainWrapper<CollectionOrder> queryChainWrapper = lambdaQuery();
        queryChainWrapper.in(CollectionOrder::getPlatformOrder, collectionPlatformOrderList);
        queryChainWrapper.orderByDesc(CollectionOrder::getCreateTime);
        return baseMapper.selectList(queryChainWrapper.getWrapper());
    }

    private static @NotNull List<String> getProcessStatus() {
        String bePaid = OrderStatusEnum.BE_PAID.getCode();
        String confirmation = OrderStatusEnum.CONFIRMATION.getCode();
        String confirmationTimeout = OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode();
        String complaint = OrderStatusEnum.COMPLAINT.getCode();
        List<String> processStatusList = new ArrayList<>();
        processStatusList.add(bePaid);
        processStatusList.add(confirmation);
        processStatusList.add(complaint);
        processStatusList.add(confirmationTimeout);
        return processStatusList;
    }

    /**
     * 查看买入订单详情
     *
     * @param platformOrder
     * @return {@link ViewBuyOrderDetailsVo}
     */
    @Override
    public ViewBuyOrderDetailsVo viewBuyOrderDetails(String platformOrder) {

        CollectionOrder collectionOrder = lambdaQuery().eq(CollectionOrder::getPlatformOrder, platformOrder).one();
        ViewBuyOrderDetailsVo viewBuyOrderDetailsVo = new ViewBuyOrderDetailsVo();
        BeanUtil.copyProperties(collectionOrder, viewBuyOrderDetailsVo);
        return viewBuyOrderDetailsVo;
    }

    @Override
    public PageReturn<CollectionOrderDTO> listRecordPage(CollectionOrderListPageReq req) {

        Page<CollectionOrder> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<CollectionOrder> lambdaQuery = lambdaQuery();

        //--动态查询 商户号
        if (!StringUtils.isEmpty(req.getMerchantCode())) {
            lambdaQuery.eq(CollectionOrder::getMerchantCode, req.getMerchantCode());
        }

        //--动态查询 会员ID
        if (!StringUtils.isEmpty(req.getMemberId())) {
            lambdaQuery.eq(CollectionOrder::getMemberId, req.getMemberId());
        }
        //--动态查询 会员ID
        if (!StringUtils.isEmpty(req.getUtr())) {
            lambdaQuery.eq(CollectionOrder::getUtr, req.getUtr());
        }

        //--动态查询 商户订单号
        if (!StringUtils.isEmpty(req.getMerchantOrder())) {
            lambdaQuery.eq(CollectionOrder::getMerchantOrder, req.getMerchantOrder());
        }

        //--动态查询 平台订单号
        if (!StringUtils.isEmpty(req.getPlatformOrder())) {
            lambdaQuery.eq(CollectionOrder::getPlatformOrder, req.getPlatformOrder());
        }

        //--动态查询 支付状态
        if (!StringUtils.isEmpty(req.getOrderStatus())) {
            lambdaQuery.eq(CollectionOrder::getOrderStatus, req.getOrderStatus());
        }

        //--动态查询 回调状态
        if (!StringUtils.isEmpty(req.getTradeCallbackStatus())) {
            lambdaQuery.eq(CollectionOrder::getTradeCallbackStatus, req.getTradeCallbackStatus());
        }


        //--动态查询 完成时间开始
        if (req.getCompletionTimeStart() != null) {
            lambdaQuery.ge(CollectionOrder::getCompletionTime, req.getCompletionTimeStart());
        }

        //--动态查询 完成时间结束
        if (req.getCompletionTimeEnd() != null) {
            lambdaQuery.le(CollectionOrder::getCompletionTime, req.getCompletionTimeEnd());
        }

        //--完成时长开始
        if (req.getCompleteDurationStart()!=null) {
            lambdaQuery.ge(CollectionOrder::getCompleteDuration, req.getCompleteDurationStart());
        }

        //--完成时长结束
        if (req.getCompleteDurationEnd()!=null) {
            lambdaQuery.le(CollectionOrder::getCompleteDuration, req.getCompleteDurationEnd());
        }

        // 倒序排序
        lambdaQuery.orderByDesc(CollectionOrder::getId);

        baseMapper.selectPage(page, lambdaQuery.getWrapper());
        List<CollectionOrder> records = page.getRecords();

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<CollectionOrderDTO> collectionOrderListVos = new ArrayList<>();
        for (CollectionOrder record : records) {
            CollectionOrderDTO collectionOrderListVo = new CollectionOrderDTO();
            BeanUtil.copyProperties(record, collectionOrderListVo);
            //CollectionOrderDTO.setPayType(PayTypeEnum.getNameByCode(collectionOrderListVo.getPayType()));
            collectionOrderListVos.add(collectionOrderListVo);
        }
//        IPage<CollectionOrderListVo> convert = page.convert(CollectionOrder -> BeanUtil.copyProperties(CollectionOrder, CollectionOrderListVo.class));
        return PageUtils.flush(page, collectionOrderListVos);
    }


    @Override
    @SneakyThrows
    public PageReturn<CollectionOrderDTO> listPage(CollectionOrderListPageReq req) {

        Page<CollectionOrder> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());

        LambdaQueryChainWrapper<CollectionOrder> lambdaQuery = lambdaQuery();

        // 新增统计金额字段总计字段
        LambdaQueryWrapper<CollectionOrder> queryWrapper = new QueryWrapper<CollectionOrder>()
                .select("IFNULL(sum(amount),0) as amountTotal,IFNULL(sum(actual_amount), 0) as actualAmountTotal,IFNULL(sum(bonus),0) as bonusTotal").lambda();

        //--动态查询 商户号
        if (!StringUtils.isEmpty(req.getMerchantCode())) {
            lambdaQuery.eq(CollectionOrder::getMerchantCode, req.getMerchantCode());
            queryWrapper.eq(CollectionOrder::getMerchantCode, req.getMerchantCode());
        }
        //--动态查询 会员ID
        if (!StringUtils.isEmpty(req.getMemberId())) {
            lambdaQuery.eq(CollectionOrder::getMemberId, req.getMemberId());
            queryWrapper.eq(CollectionOrder::getMemberId, req.getMemberId());
        }
        //--动态查询 会员ID
        if (!StringUtils.isEmpty(req.getUtr())) {
            lambdaQuery.eq(CollectionOrder::getUtr, req.getUtr());
            queryWrapper.eq(CollectionOrder::getUtr, req.getUtr());
        }

        //--动态查询 商户订单号
        if (!StringUtils.isEmpty(req.getMerchantOrder())) {
            lambdaQuery.eq(CollectionOrder::getMerchantOrder, req.getMerchantOrder());
            queryWrapper.eq(CollectionOrder::getMerchantOrder, req.getMerchantOrder());
        }

        //--动态查询 平台订单号
        if (!StringUtils.isEmpty(req.getPlatformOrder())) {
            lambdaQuery.eq(CollectionOrder::getPlatformOrder, req.getPlatformOrder());
            queryWrapper.eq(CollectionOrder::getPlatformOrder, req.getPlatformOrder());
        }

        //--动态查询 支付状态
        if (!StringUtils.isEmpty(req.getOrderStatus())) {
            lambdaQuery.eq(CollectionOrder::getOrderStatus, req.getOrderStatus());
            queryWrapper.eq(CollectionOrder::getOrderStatus, req.getOrderStatus());
        }

        //--动态查询 回调状态
        if (!StringUtils.isEmpty(req.getTradeCallbackStatus())) {
            lambdaQuery.eq(CollectionOrder::getTradeCallbackStatus, req.getTradeCallbackStatus());
            queryWrapper.eq(CollectionOrder::getTradeCallbackStatus, req.getTradeCallbackStatus());
        }


        //--动态查询 完成时间开始
        if (req.getCompletionTimeStart() != null) {
            lambdaQuery.ge(CollectionOrder::getCompletionTime, req.getCompletionTimeStart());
            queryWrapper.ge(CollectionOrder::getCompletionTime, req.getCompletionTimeStart());
        }

        //--动态查询 完成时间结束
        if (req.getCompletionTimeEnd() != null) {
            lambdaQuery.le(CollectionOrder::getCompletionTime, req.getCompletionTimeEnd());
            queryWrapper.le(CollectionOrder::getCompletionTime, req.getCompletionTimeEnd());
        }

        //--动态查询 完成时间开始
        if (ObjectUtils.isNotEmpty(req.getStartTime())) {
            lambdaQuery.ge(CollectionOrder::getCreateTime, req.getStartTime());
            queryWrapper.ge(CollectionOrder::getCreateTime, req.getStartTime());
        }

        //--动态查询 完成时间结束
        if (ObjectUtils.isNotEmpty(req.getEndTime())) {
            lambdaQuery.le(CollectionOrder::getCreateTime, req.getEndTime());
            queryWrapper.le(CollectionOrder::getCreateTime, req.getEndTime());
        }

        //--完成时长开始
        if (req.getCompleteDurationStart()!=null) {
            lambdaQuery.ge(CollectionOrder::getCompleteDuration, req.getCompleteDurationStart());
            queryWrapper.ge(CollectionOrder::getCompleteDuration, req.getCompleteDurationStart());
        }

        //--完成时长结束
        if (req.getCompleteDurationEnd()!=null) {
            lambdaQuery.le(CollectionOrder::getCompleteDuration, req.getCompleteDurationEnd());
            queryWrapper.le(CollectionOrder::getCompleteDuration, req.getCompleteDurationEnd());
        }
        // 查询风控标识
        if (!ObjectUtils.isEmpty(req.getRiskTag())) {
            String blackIpCode = RiskTagEnum.BLACK_IP.getCode();
            String normalCode = RiskTagEnum.Normal.getCode();
            if(req.getRiskTag().equals(blackIpCode)){
                lambdaQuery.eq(CollectionOrder::getRiskTagBlack, 1);
                queryWrapper.eq(CollectionOrder::getRiskTagBlack, 1);
            }
            else if(req.getRiskTag().equals(normalCode)){
                lambdaQuery.eq(CollectionOrder::getRiskTagBlack, 0);
                queryWrapper.eq(CollectionOrder::getRiskTagBlack, 0);
            }
            // 列表无余额过低筛选和操作超时
            else{
                lambdaQuery.eq(CollectionOrder::getId, -1);
                queryWrapper.eq(CollectionOrder::getId, -1);
            }
        }

        if (ObjectUtils.isNotEmpty(req.getKycAutoCompletionStatus())) {
            lambdaQuery.eq(CollectionOrder::getKycAutoCompletionStatus, req.getKycAutoCompletionStatus());
            queryWrapper.eq(CollectionOrder::getKycAutoCompletionStatus, req.getKycAutoCompletionStatus());
        }


        // 倒序排序
        lambdaQuery.orderByDesc(CollectionOrder::getId);

        Page<CollectionOrder> finalPage = page;
        CompletableFuture<CollectionOrder> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<CollectionOrder>> collectionFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));

        CompletableFuture.allOf(totalFuture, collectionFuture);

        CollectionOrder totalInfo = totalFuture.get();

        JSONObject extent = new JSONObject();
        extent.put("amountTotal", totalInfo.getAmountTotal());
        extent.put("actualAmountTotal", totalInfo.getActualAmountTotal());
        extent.put("bonusPageTotal", totalInfo.getBonusTotal());

        page = collectionFuture.get();
        BigDecimal amountPageTotal = BigDecimal.ZERO;
        BigDecimal actualAmountPageTotal = BigDecimal.ZERO;
        BigDecimal bonusPageTotal = BigDecimal.ZERO;

        List<CollectionOrder> records = page.getRecords();
        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<CollectionOrderDTO> collectionOrderListVos = new ArrayList<>();
        for (CollectionOrder record : records) {
            List<String> riskTag = new ArrayList<>();
            amountPageTotal = amountPageTotal.add(record.getAmount());
            actualAmountPageTotal = actualAmountPageTotal.add(record.getActualAmount());
            bonusPageTotal = bonusPageTotal.add(record.getBonus());
            CollectionOrderDTO collectionOrderListVo = new CollectionOrderDTO();
            BeanUtil.copyProperties(record, collectionOrderListVo);
            if(!ObjectUtils.isEmpty(record.getRiskTagBlack()) && record.getRiskTagBlack() != 0){
                riskTag.add(RiskTagEnum.BLACK_IP.getCode());
            }
            if(riskTag.isEmpty()){
                riskTag.add(RiskTagEnum.Normal.getCode());
            }
            collectionOrderListVo.setRiskTag(String.join(",", riskTag));
            collectionOrderListVos.add(collectionOrderListVo);
        }

        extent.put("amountPageTotal", amountPageTotal);
        extent.put("actualPageAmountTotal", actualAmountPageTotal);
        extent.put("bonusPagePageTotal", bonusPageTotal);
        return PageUtils.flush(page, collectionOrderListVos, extent);
    }

    @Override
    public PageReturn<CollectionOrderExportDTO> listPageExport(CollectionOrderListPageReq req) {
        PageReturn<CollectionOrderDTO> collectionOrderReturn = listPage(req);

        List<CollectionOrderExportDTO> resultList = new ArrayList<>();

        for (CollectionOrderDTO collectionOrderDTO : collectionOrderReturn.getList()) {
            CollectionOrderExportDTO collectionOrderExportDTO = new CollectionOrderExportDTO();
            BeanUtils.copyProperties(collectionOrderDTO, collectionOrderExportDTO);
            String nameByCode = OrderStatusEnum.getNameByCode(collectionOrderDTO.getOrderStatus());
            collectionOrderExportDTO.setOrderStatus(nameByCode);
            if(collectionOrderDTO.getActualAmount() != null){
                collectionOrderExportDTO.setActualAmount(collectionOrderDTO.getActualAmount().toString());
            }
            if(collectionOrderDTO.getAmount() != null){
                collectionOrderExportDTO.setAmount(collectionOrderDTO.getAmount().toString());
            }
            if(collectionOrderDTO.getCompleteDuration() != null){
                String orderCompleteDuration = DurationCalculatorUtil.getOrderCompleteDuration(collectionOrderDTO.getCompleteDuration().toString());
                collectionOrderExportDTO.setCompleteDuration(orderCompleteDuration);
            }
            resultList.add(collectionOrderExportDTO);
        }
        Page<CollectionOrderExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(collectionOrderReturn.getTotal());
        return PageUtils.flush(page, resultList);
    }


    @Transactional
    @Override
    public CollectionOrderDTO pay(CollectionOrderIdReq req) {
        CollectionOrder collectionOrder = new CollectionOrder();
        collectionOrder.setId(req.getId());
        collectionOrder = this.baseMapper.selectById(collectionOrder);
        if(collectionOrder.getOrderStatus().equals(OrderStatusEnum.SUCCESS.getCode())){
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        MatchingOrder matchingOrder = matchingOrderService.getMatchingOrderByCollection(collectionOrder.getPlatformOrder());
        if(!ObjectUtils.isEmpty(matchingOrder)){

            // 更新卖出订单状态
            matchingOrder.setStatus(OrderStatusEnum.SUCCESS.getCode());
            matchingOrderMapper.updateById(matchingOrder);

            collectionOrder.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
            collectionOrder.setActualAmount(req.getActualAmount());
            collectionOrder.setCompletionTime(LocalDateTime.now());
            collectionOrder.setUpdateBy(req.getCompletedBy());

            baseMapper.updateById(collectionOrder);

            //sellService.transactionSuccessHandler(matchingOrder.getCollectionPlatformOrder());

        }

        //amountChangeUtil.insertMemberChangeAmountRecord(collectionOrder.getMemberId(), req.getActualAmount(), ChangeModeEnum.ADD, "ARB", collectionOrder.getPlatformOrder(),  MemberAccountChangeEnum.RECHARGE, req.getCompletedBy());
        CollectionOrderDTO collectionOrderDTO = new CollectionOrderDTO();
        BeanUtil.copyProperties(collectionOrder, collectionOrderDTO);
        return collectionOrderDTO;

    }

    @Override
    public CollectionOrderDTO listPageRecordTotal(CollectionOrderListPageReq req) {

        QueryWrapper<CollectionOrder> queryWrapper = new QueryWrapper<>();

        queryWrapper.select(
                "sum(amount) as amount"

        );

        //--动态查询 会员ID
        if (!StringUtils.isEmpty(req.getMemberId())) {
            queryWrapper.eq("member_id", req.getMemberId());
        }
        //--动态查询 会员ID
        if (!StringUtils.isEmpty(req.getUtr())) {
            queryWrapper.eq("utr", req.getUtr());
        }

        //--动态查询 商户号
        if (!StringUtils.isEmpty(req.getMerchantCode())) {
            queryWrapper.eq("merchant_code", req.getMerchantCode());
        }

        //--动态查询 商户订单号
        if (!StringUtils.isEmpty(req.getMerchantOrder())) {
            queryWrapper.eq("merchant_order", req.getMerchantOrder());
        }

        //--动态查询 平台订单号
        if (!StringUtils.isEmpty(req.getPlatformOrder())) {
            queryWrapper.eq("platform_order", req.getPlatformOrder());
        }

        //--动态查询 支付状态
        if (!StringUtils.isEmpty(req.getOrderStatus())) {
            queryWrapper.eq("order_status", req.getOrderStatus());
        }

        //--动态查询 回调状态
        if (!StringUtils.isEmpty(req.getTradeCallbackStatus())) {
            queryWrapper.eq("trade_callback_status", req.getTradeCallbackStatus());
        }


        //--动态查询 完成时间开始
        if (req.getCompletionTimeStart() != null) {
            queryWrapper.ge("completion_time", req.getCompletionTimeStart());
        }

        //--动态查询 完成时间结束
        if (req.getCompletionTimeEnd() != null) {
            queryWrapper.le("completion_time", req.getCompletionTimeEnd());
        }

        //--完成时长开始
        if (req.getCompleteDurationStart()!=null) {
            queryWrapper.ge("complete_duration", req.getCompleteDurationStart());
        }

        //--完成时长结束
        if (req.getCompleteDurationEnd()!=null) {
            queryWrapper.le("complete_duration", req.getCompleteDurationEnd());
        }


        // 倒序排序
        //queryWrapper.orderByDesc(CollectionOrder::getId);
        Page<Map<String, Object>> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        baseMapper.selectMapsPage(page, queryWrapper);
        List<Map<String, Object>> records = page.getRecords();
        if (records == null) return new CollectionOrderDTO();
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(records);
        List<CollectionOrderDTO> list = jsonArray.toJavaList(CollectionOrderDTO.class);
        CollectionOrderDTO collectionOrderDTO = list.get(0);

        return collectionOrderDTO;


    }

    @Override
    @Transactional
    public boolean updateOrderByOrderNo(String merchantNo, String orderId, String realAmount, String payType) {
//        boolean req = false;
//        LambdaQueryChainWrapper<CollectionOrder> lambdaQuery = lambdaQuery();
//        CollectionOrder collectionOrder = lambdaQuery().eq(CollectionOrder::getPlatformOrder, orderId).one();
//        if (StringUtils.isEmpty(collectionOrder.getCurrency())) {
//            log.info("商户号{}订单{}币种返回错误", merchantNo, orderId);
//            return false;
//        }
//        String key = "AR-PAY" + collectionOrder.getCurrency();
//        RLock lock = redissonUtil.getLock(key);
//
//        try {
//
//
//            req = lock.tryLock(10, TimeUnit.SECONDS);
//
//            boolean updateOrderStatus = this.updateById(collectionOrder);
//            return updateOrderStatus;
//
//        } catch (Exception e) {
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//        } finally {
//            if (req) {
//                lock.unlock();
//            }
//        }
//        return false;
        return false;
    }

    @Override
    public Boolean manualCallback(Long id, String opName) {
        boolean result = false;
        CollectionOrder paymentOrder = this.baseMapper.selectById(id);

        if(ObjectUtils.isEmpty(paymentOrder)){
            throw new BizException(ResultCode.ORDER_NOT_EXIST);
        }
        // 判断交易回调状态
        if(paymentOrder.getTradeCallbackStatus().equals(NotifyStatusEnum.SUCCESS.getCode()) || paymentOrder.getTradeCallbackStatus().equals(NotifyStatusEnum.MANUAL_SUCCESS.getCode())){
            throw new BizException(ResultCode.ORDER_ALREADY_CALLBACK);
        }else {
            // 更新订单状态
            result = this.baseMapper.updateOrderStatusById(id);
            // 记录会员账变
            amountChangeUtil.insertMemberChangeAmountRecord(paymentOrder.getMemberId(), paymentOrder.getActualAmount(), ChangeModeEnum.ADD,
                    CurrenceEnum.ARB.getCode(), paymentOrder.getPlatformOrder(), MemberAccountChangeEnum.RECHARGE, opName);
        }
        return result;
    }

    /*
     * 手动回调
     * */
    @Override
    public RestResult manualCallback(String merchantOrder) {

        log.info("手动回调商户订单号: {}, 手动回调...", merchantOrder);

        //根据商户订单号定位到该笔订单
        QueryWrapper<CollectionOrder> collectionOrderQueryWrapper = new QueryWrapper<>();
        collectionOrderQueryWrapper.eq("merchant_order", merchantOrder);
        CollectionOrder collectionOrder = getOne(collectionOrderQueryWrapper);
        if (collectionOrder != null) {
            Map<String, Object> dataMap = new HashMap<>();
            //商户号
            dataMap.put("merchantCode", collectionOrder.getMerchantCode());

            //平台订单号
            dataMap.put("platformOrder", collectionOrder.getPlatformOrder());

            //商户订单号
            dataMap.put("merchantOrder", collectionOrder.getMerchantOrder());

            //回调地址
            dataMap.put("notifyUrl", collectionOrder.getTradeNotifyUrl());

            //回调金额
            dataMap.put("amount", collectionOrder.getAmount());

            //md5签名
            String signinfo = SignUtil.sortData(dataMap, "&");

            log.info("手动回调平台订单号: {}, dataMap: {}, 手动回调商户签名串: {}", collectionOrder.getPlatformOrder(), dataMap, signinfo);

            String sign = SignAPI.sign(signinfo, merchantInfoService.getMd5KeyByCode(collectionOrder.getMerchantCode()));
            dataMap.put("sign", sign);

            //封装整体参数
            HashMap<String, Object> reqMap = new HashMap<>();
            reqMap.put("code", ResultCode.SUCCESS.getCode());
            reqMap.put("data", dataMap);
            reqMap.put("msg", "OK");

            String reqinfo = JSON.toJSONString(reqMap);

            log.info("手动回调平台订单号: {}, 手动回调商户请求地址: {}, 手动回调商户请求数据: {}", collectionOrder.getPlatformOrder(), collectionOrder.getTradeNotifyUrl(), reqinfo);

            CollectionOrderServiceImpl collectionOrderService = SpringContextUtil.getBean(CollectionOrderServiceImpl.class);

            try {
                String resultCallBack = RequestUtil.HttpRestClientToJson(collectionOrder.getTradeNotifyUrl(), reqinfo);
                log.info("手动回调平台订单号: {}, 手动回调商户返回数据: {}", collectionOrder.getPlatformOrder(), resultCallBack);
                if ("SUCCESS".equals(resultCallBack)) {
                    log.info("手动回调平台订单号: {}, 手动回调成功: {}", collectionOrder.getPlatformOrder(), resultCallBack);
                    //更新订单回调状态为4 --手动回调成功
                    collectionOrder.setTradeCallbackStatus(NotifyStatusEnum.MANUAL_SUCCESS.getCode());
                    //更新订单回调时间
                    collectionOrder.setTradeCallbackTime(LocalDateTime.now());
                    collectionOrderService.updateById(collectionOrder);
                    return RestResult.ok();
                } else {
                    //更新订单回调状态为5 --手动回调失败
                    log.info("手动回调平台订单号: {}, 手动回调失败: {}, 商户未返回SUCCESS", collectionOrder.getPlatformOrder(), resultCallBack);
                    collectionOrder.setTradeCallbackStatus(NotifyStatusEnum.MANUAL_FAILED.getCode());
                    collectionOrderService.updateById(collectionOrder);
                    return RestResult.failed("回调失败");
                }
            } catch (Exception e) {
                //更新订单回调状态为5 --手动回调失败
                log.info("手动回调平台订单号: {}, 手动回调失败", collectionOrder.getPlatformOrder());
                collectionOrder.setTradeCallbackStatus(NotifyStatusEnum.MANUAL_FAILED.getCode());
                collectionOrderService.updateById(collectionOrder);
                throw new RuntimeException(e);
            }
        } else {
            log.info("手动回调商户订单号: {}, 手动回调失败, 该订单不存在", merchantOrder);
            return RestResult.failed("该订单不存在");
        }
    }

    @Override
    public RestResult getCollectionOrderInfoByOrderNo(String merchantOrder) {
        //根据订单号查询代收订单详情
        QueryWrapper<CollectionOrder> collectionOrderQueryWrapper = new QueryWrapper<>();
        collectionOrderQueryWrapper.select("merchant_code", "amount", "collected_amount", "order_rate", "currency",
                "create_time", "callback_time", "order_status", "pay_type", "cost", "third_code").eq("merchant_order", merchantOrder);
        CollectionOrder collectionOrder = getOne(collectionOrderQueryWrapper);

        if (collectionOrder != null) {
            CollectionOrderInfoVo collectionOrderInfoVo = new CollectionOrderInfoVo();
            BeanUtil.copyProperties(collectionOrder, collectionOrderInfoVo);

            //通过商户号查询商户名称
            QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
            merchantInfoQueryWrapper.select("username").eq("code", collectionOrder.getMerchantCode());
            MerchantInfo merchantInfo = merchantInfoService.getOne(merchantInfoQueryWrapper);
            if (merchantInfo != null) {
                collectionOrderInfoVo.setUsername(merchantInfo.getUsername());
            }
            System.out.println("collectionOrderInfoVo: " + collectionOrderInfoVo);

            //通过三方代码 查询支付通道名称

            //匹配支付类型枚举值 将支付类型名称返回给前端
            collectionOrderInfoVo.setPayType(PayTypeEnum.getNameByCode(collectionOrder.getPayType()));
            return RestResult.ok(collectionOrderInfoVo);
        } else {

            return RestResult.failed("该笔订单不存在");
        }

    }

    /*
     * 查询下拉列表数据(币种,支付类型)
     * */
    @Override
    public RestResult selectList() {
        //获取当前用户的商户ID
        Long currentUserId = UserContext.getCurrentUserId();


        //查询该商户存在的币种和支付类型
        //币种  一个商户只对应一个币种
        //查询该商户的币种字段
        QueryWrapper<MerchantInfo> merchantInfoQueryWrapper = new QueryWrapper<>();
        merchantInfoQueryWrapper.select("currency").eq("id", currentUserId);
        MerchantInfo merchantInfo = merchantInfoService.getOne(merchantInfoQueryWrapper);

        selectListVo selectListVo = new selectListVo();

        if (merchantInfo != null) {
            //设置币种
            JSONObject currencyJson = new JSONObject();
            currencyJson.put("value", merchantInfo.getCurrency());
            currencyJson.put("label", merchantInfo.getCurrency());
            ArrayList<JSONObject> currencyList = new ArrayList<>();
            currencyList.add(currencyJson);
            selectListVo.setCurrency(currencyList);


            return RestResult.ok(selectListVo);
        } else {
            return RestResult.failed("商户不存在");
        }
    }

    /*
     * 根据id更改订单已发送状态
     * */
    @Override
    public int updateOrderSendById(String id) {
        LambdaUpdateWrapper<CollectionOrder> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(CollectionOrder::getId, id).set(CollectionOrder::getTradeNotifySend, SendStatusEnum.HAS_BEEN_SENT.getCode());
        return getBaseMapper().update(null, lambdaUpdateWrapper);
    }

    /*
     * 更新支付订单状态为: 确认中
     * */
    @Override
    public boolean updateOrderStatusToConfirmation(String merchantOrder) {
        return lambdaUpdate().eq(CollectionOrder::getMerchantOrder, merchantOrder).set(CollectionOrder::getOrderStatus, OrderStatusEnum.CONFIRMATION.getCode()).update();
    }

    /**
     * 根据支付订单匹配代付订单
     */
    @Override
    public JSONObject matchWithdrawOrder(CollectionOrder collectionOrder) {

        //(代付池) 查询所有代付订单: 条件: 待匹配状态
        List<PaymentOrder> payments = paymentOrderService.getPaymentOrderBySatus();
        log.info("代付池: {}", JSON.toJSONString(payments, SerializerFeature.WriteMapNullValue));

        //将代付池的数据转为Map key=金额  value=订单信息
        Map<String, PaymentOrder> pmap = payments.stream().collect(Collectors.toMap(PaymentOrder::getAmountStr, Function.identity(), (key1, key2) -> key2));

        log.info("匹配代付订单: 金额->订单信息: {}", JSON.toJSONString(pmap, SerializerFeature.WriteMapNullValue));

        //匹配相同金额的代付订单
        PaymentOrder paymentOrder = pmap.get(collectionOrder.getAmount() + ".00");

        if (paymentOrder != null) {

            //同步匹配成功

            MatchingOrder matchingOrder = new MatchingOrder();

            //充值商户订单号
            matchingOrder.setCollectionMerchantOrder(collectionOrder.getMerchantOrder());

            //充值平台订单号
            matchingOrder.setCollectionPlatformOrder(collectionOrder.getPlatformOrder());

            //提现商户订单号
            matchingOrder.setPaymentMerchantOrder(paymentOrder.getMerchantOrder());

            //提现平台订单号
            matchingOrder.setPaymentPlatformOrder(paymentOrder.getPlatformOrder());

            //订单提交金额
            matchingOrder.setOrderSubmitAmount(collectionOrder.getAmount());

            //订单实际金额
            matchingOrder.setOrderActualAmount(paymentOrder.getAmount());

            //充值商户号
            matchingOrder.setCollectionMerchantCode(collectionOrder.getMerchantCode());

            //提现商户号
            matchingOrder.setPaymentMerchantCode(paymentOrder.getMerchantCode());

            //充值会员ID
            matchingOrder.setCollectionMemberId(collectionOrder.getMemberId());

            //充值会员账号
            matchingOrder.setCollectionMemberAccount(collectionOrder.getMemberAccount());

            //提现会员ID
            matchingOrder.setPaymentMemberId(paymentOrder.getMemberId());

            //提现会员账号
            matchingOrder.setPaymentMemberAccount(paymentOrder.getMemberAccount());

            //充值交易回调地址
            matchingOrder.setCollectionTradeNotifyUrl(collectionOrder.getTradeNotifyUrl());

            //提现交易回调地址
            matchingOrder.setPaymentTradeNotifyUrl(paymentOrder.getTradeNotifyUrl());

            //匹配回调地址
            matchingOrder.setMatchNotifyUrl(paymentOrder.getMatchNotifyUrl());

            //UPI_ID
            matchingOrder.setUpiId(paymentOrder.getUpiId());

            //UPI_Name
            matchingOrder.setUpiName(paymentOrder.getUpiName());

            //匹配订单入库
            matchingOrderService.save(matchingOrder);

            //组装返回数据
            CollectionResVo collectionResVo = new CollectionResVo();
            //商户号
            collectionResVo.setMerchantCode(collectionOrder.getMerchantCode());
            //商户订单号
            collectionResVo.setMerchantOrder(collectionOrder.getMerchantOrder());
            //平台订单号
            collectionResVo.setPlatformOrder(collectionOrder.getPlatformOrder());
            //订单金额
            collectionResVo.setAmount(paymentOrder.getAmount());
            //时间戳
            collectionResVo.setTimestamp(String.valueOf(System.currentTimeMillis()));
            //UPI_ID
            collectionResVo.setUpiId(paymentOrder.getUpiId());
            //UPI_Name
            collectionResVo.setUpiName(paymentOrder.getUpiName());

            //将支付订单状态改为待支付并设置实际金额
            updateCollectionOrderStatusToBePaid(collectionOrder.getMerchantOrder(), paymentOrder.getAmount());

            //更改代付订单状态为待支付
            paymentOrderService.updateOrderStatusBePaid(paymentOrder.getMerchantOrder());

            //webSocket推送 推荐金额列表给前端
//            merchantSendRecommendAmount.send();

            //发送代付匹配成功回调MQ
//            QueueInfo queueInfo = new QueueInfo(RabbitMqConstants.AR_WALLET_MATCH_QUEUE_NAME, matchingOrder.getId(), matchingOrder.getPaymentMerchantOrder());
//            rabbitTemplate.convertAndSend(RabbitMqConstants.AR_WALLET_MATCH_QUEUE_NAME, matchingOrder, new CorrelationData(JSON.toJSONString(queueInfo)));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", "1");
            jsonObject.put("data", collectionResVo);

            return jsonObject;

        } else {
            //匹配失败 将推荐金额返回给前端

            HashMap<String, Integer> paymentAmountMap = new HashMap<>();

            //遍历当前代付池里面的金额 最接近支付金额的10笔
            for (PaymentOrder payment : payments) {
                paymentAmountMap.put(payment.getMerchantOrder(), (int) Double.parseDouble(payment.getAmountStr()));
            }

            //查询最接近充值金额的前10笔代付订单
            List<Map.Entry<String, Integer>> recommendAmounts = findClosestValues(paymentAmountMap, Integer.parseInt(collectionOrder.getAmountStr()), 10);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("code", "2");
            jsonObject.put("data", recommendAmounts);

            return jsonObject;
        }
    }

    /*
     * 查询最接近给定数字的前10个元素
     * p1 代付池金额列表
     * p2 充值金额
     * p3 列表推荐个数
     * */
    @Override
    public List<Map.Entry<String, Integer>> findClosestValues(Map<String, Integer> map, int collectionAmount, int count) {
        if (map == null || map.isEmpty()) {
            return new ArrayList<>(); // 返回一个空列表表示原始 Map 为空
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());

        // 对 Map.Entry 进行排序，根据值与目标数字的差值
        entries.sort(Comparator.comparingInt(entry -> Math.abs(entry.getValue() - collectionAmount)));

        // 返回前 count 个 Map.Entry
        return entries.subList(0, Math.min(count, entries.size()));
    }


    /**
     * 更新充值订单状态为待支付并设置实际金额(匹配成功调用)
     */
    @Override
    public boolean updateCollectionOrderStatusToBePaid(String merchantOrder, BigDecimal actualAmount) {
        return lambdaUpdate().eq(CollectionOrder::getMerchantOrder, merchantOrder).set(CollectionOrder::getOrderStatus, OrderStatusEnum.BE_PAID.getCode()).set(CollectionOrder::getActualAmount, actualAmount).update();
    }


    /**
     * 根据订单号获取买入订单
     *
     * @param platformOrder
     * @return {@link CollectionOrder}
     */
    @Override
    public CollectionOrder getCollectionOrderByPlatformOrder(String platformOrder) {
        return lambdaQuery().eq(CollectionOrder::getPlatformOrder, platformOrder).or().eq(CollectionOrder::getMerchantOrder, platformOrder).one();
    }

    /**
     * 根据会员id 查看进行中的买入订单数量
     *
     * @param memberId
     */
    @Override
    public CollectionOrder countActiveBuyOrders(String memberId) {
        return lambdaQuery().in(
                        CollectionOrder::getOrderStatus,
                        OrderStatusEnum.BE_PAID.getCode(),//待支付
                        OrderStatusEnum.CONFIRMATION.getCode(),//确认中
                        OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode(),//确认超时
                        OrderStatusEnum.COMPLAINT.getCode(),//申诉中
                        OrderStatusEnum.AMOUNT_ERROR.getCode())//金额错误
                .eq(CollectionOrder::getMemberId, memberId).one();
    }

    /**
     * 根据会员id 获取待支付和支付超时的买入订单
     *
     * @param memberId
     * @return {@link CollectionOrder}
     */
    @Override
    public CollectionOrder getPendingBuyOrder(String memberId) {
        return lambdaQuery().eq(CollectionOrder::getMemberId, memberId).eq(CollectionOrder::getOrderStatus, OrderStatusEnum.BE_PAID.getCode()).one();
    }

    /**
     * 获取买入订单详情
     *
     * @param platformOrderReq
     * @return {@link RestResult}<{@link BuyOrderDetailsVo}>
     */
    @Override
    public RestResult<BuyOrderDetailsVo> getBuyOrderDetails(PlatformOrderReq platformOrderReq) {

        //获取当前会员信息
        MemberInfo memberInfo = memberInfoService.getMemberInfo();

        if (memberInfo == null){
            log.error("查询买入订单列表失败: 获取会员信息失败: {}", memberInfo);
            return RestResult.failure(ResultCode.RELOGIN);
        }

        CollectionOrder collectionOrder = getCollectionOrderByPlatformOrder(platformOrderReq.getPlatformOrder());

        //返回数据vo
        BuyOrderDetailsVo buyOrderDetailsVo = new BuyOrderDetailsVo();

        BeanUtil.copyProperties(collectionOrder, buyOrderDetailsVo);

        //兼容取消原因和失败原因
        if (buyOrderDetailsVo.getRemark() == null){
            buyOrderDetailsVo.setRemark(buyOrderDetailsVo.getCancellationReason());
        }

        //判断如果订单状态是手动完成 那么改为已完成
        if (buyOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())){
            buyOrderDetailsVo.setOrderStatus(OrderStatusEnum.SUCCESS.getCode());
        }

        //是否经过申诉
        if (collectionOrder.getAppealTime() != null){
            buyOrderDetailsVo.setIsAppealed(1);
        }

        //设置待支付剩余时间
        buyOrderDetailsVo.setPaymentExpireTime(redisUtil.getPaymentRemainingTime(platformOrderReq.getPlatformOrder()));

        //设置确认中 剩余时间
        buyOrderDetailsVo.setConfirmExpireTime(redisUtil.getConfirmRemainingTime(platformOrderReq.getPlatformOrder()));

        //判断如果订单是确认中状态, 但是确认剩余时间低于0 那么将返回前端的订单状态改为确认超时
        if (buyOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION.getCode()) && (buyOrderDetailsVo.getConfirmExpireTime() == null || buyOrderDetailsVo.getConfirmExpireTime() < 1)){
            buyOrderDetailsVo.setOrderStatus(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
        }

        //判断如果订单是支付中状态, 但是支付剩余时间低于0 那么将返回前端的订单状态改为支付超时
        if (buyOrderDetailsVo.getOrderStatus().equals(OrderStatusEnum.BE_PAID.getCode()) && (buyOrderDetailsVo.getPaymentExpireTime() == null || buyOrderDetailsVo.getPaymentExpireTime() < 1)){
            buyOrderDetailsVo.setOrderStatus(OrderStatusEnum.PAYMENT_TIMEOUT.getCode());
        }

        // 人工
        if(OrderStatusEnum.isAuditing(collectionOrder.getOrderStatus(), collectionOrder.getAuditDelayTime())){
            buyOrderDetailsVo.setIsAuditingStatus(Boolean.TRUE);
            TradeConfig tradeConfig = tradeConfigService.getById(1);
            // 防止人工审参数被置空
            Long delayTime = tradeConfig.getManualReviewTime() == null ? 5L : Long.valueOf(tradeConfig.getManualReviewTime());
            buyOrderDetailsVo.setDelayMinutes(delayTime);
        }

        log.info("获取买入订单详情成功: 会员账号: {}, req: {}, 返回数据: {}", memberInfo.getMemberAccount(), platformOrderReq, buyOrderDetailsVo);

        return RestResult.ok(buyOrderDetailsVo);
    }

    /**
     * 根据IP获取买入订单
     *
     * @param ip
     * @return
     */
    @Override
    public List<CollectionOrder> getCollectOrderByByIp(String ip) {
        LocalDate localDate = LocalDate.now().minusMonths(6);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        return lambdaQuery().eq(CollectionOrder::getClientIp, ip).ge(CollectionOrder::getCreateTime, startOfDay)
                .select(CollectionOrder::getMemberId, CollectionOrder::getPlatformOrder)
                .list();
    }

    /**
     * 标记订单为指定的tag
     *
     * @param riskTag
     * @param platformOrders
     */
    @Override
    @Transactional
    public void taggingOrders(String riskTag, List<String> platformOrders) {
        if (RiskTagEnum.getNameByCode(riskTag) == null) {
            return;
        }
        if (CollectionUtils.isEmpty(platformOrders)) {
            return;
        }
        LambdaUpdateChainWrapper<CollectionOrder> updateWrapper = lambdaUpdate().in(CollectionOrder::getPlatformOrder, platformOrders);
        if (RiskTagEnum.BLACK_IP.getCode().equals(riskTag)) {
            updateWrapper.set(CollectionOrder::getRiskTagBlack, 1);
        }else if(RiskTagEnum.Normal.getCode().equals(riskTag)){
            updateWrapper.set(CollectionOrder::getRiskTagBlack, 0);
        }else {
            return;
        }
        updateWrapper.update();

    }



}
