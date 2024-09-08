package org.ar.wallet.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.core.utils.StringUtils;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.MatchingOrderDTO;
import org.ar.common.pay.dto.MatchingOrderExportDTO;
import org.ar.common.pay.dto.MatchingOrderPageListDTO;
import org.ar.common.pay.dto.*;
import org.ar.common.pay.req.*;
import org.ar.common.web.exception.BizException;
import org.ar.common.web.utils.UserContext;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.*;
import org.ar.wallet.property.ArProperty;
import org.ar.wallet.rabbitmq.RabbitMQService;
import org.ar.wallet.req.CancelOrderReq;
import org.ar.wallet.service.*;
import org.ar.wallet.util.AmountChangeUtil;
import org.ar.wallet.util.DurationCalculatorUtil;
import org.ar.wallet.util.TradeConfigHelperUtil;
import org.ar.wallet.vo.BuyCompletedVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @author
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingOrderServiceImpl extends ServiceImpl<MatchingOrderMapper, MatchingOrder> implements IMatchingOrderService {

    private final IPaymentOrderService paymentOrderService;
    private final CollectionOrderMapper collectionOrderMapper;
    private final AppealOrderMapper appealOrderMapper;
    private final AmountChangeUtil amountChangeUtil;
    private final PaymentOrderMapper paymentOrderMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final MatchingOrderMapper matchingOrderMapper;
    private final ITradeConfigService tradeConfigService;
    private final IMemberInfoService memberInfoService;
    private final RabbitMQService rabbitMQService;
    private final TradeConfigHelperUtil tradeConfigHelperUtil;
    @Autowired
    private ArProperty arProperty;
    @Autowired
    private IAppealOrderService appealOrderService;


    /*
     * 更新匹配订单 支付状态为: 确认中 更新订单支付时间
     * */
    @Override
    public boolean updateCollectionOrderStatusToConfirmation(String collectionOrder) {
        return lambdaUpdate().eq(MatchingOrder::getCollectionMerchantOrder, collectionOrder).set(MatchingOrder::getPayStatus, OrderStatusEnum.CONFIRMATION.getCode()).set(MatchingOrder::getPaymentTime, LocalDateTime.now(ZoneId.systemDefault())).update();
    }

    /*
     * 更新匹配订单代付状态为: 确认中
     * */
    @Override
    public boolean updatePaymentOrderStatusToConfirmation(String paymentOrder) {
        return lambdaUpdate().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).set(MatchingOrder::getPaymentStatus, OrderStatusEnum.CONFIRMATION.getCode()).update();
    }

    /*
     * 查询10秒前匹配成功并且未发送MQ的订单
     * */
    @Override
    public List<MatchingOrder> getMatchSuccessAndUnsent() {
        //查询条件 10秒前
        LocalDateTime tenSecondsAgo = LocalDateTime.now(ZoneId.systemDefault()).minusSeconds(10);
        return lambdaQuery().eq(MatchingOrder::getMatchSend, SendStatusEnum.UNSENT.getCode()).lt(MatchingOrder::getCreateTime, tenSecondsAgo).select().list();
    }

    /*
     * 根据代付订单号更新订单的匹配发送状态
     * */
    @Override
    public boolean updateOrderMatchSendByOrder(String paymentOrder) {
        return lambdaUpdate().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).set(MatchingOrder::getMatchSend, SendStatusEnum.HAS_BEEN_SENT.getCode()).update();
    }

    /**
     * 更新充值交易回调MQ发送状态
     */
    @Override
    public boolean updateCollectionTradeSend(String collectionOrder) {
        return lambdaUpdate().eq(MatchingOrder::getCollectionMerchantOrder, collectionOrder).set(MatchingOrder::getCollectionTradeSend, SendStatusEnum.HAS_BEEN_SENT.getCode()).update();
    }

    /**
     * 更新提现交易回调MQ发送状态
     */
    @Override
    public boolean updatePaymentTradeSend(String paymentOrder) {
        return lambdaUpdate().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).set(MatchingOrder::getPaymentTradeSend, SendStatusEnum.HAS_BEEN_SENT.getCode()).update();
    }

    /*
     * 更新匹配回调成功的状态
     * */
    @Override
    public boolean updateMatchSuccess(String paymentOrder) {
        return lambdaUpdate().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).set(MatchingOrder::getMatchCallbackStatus, NotifyStatusEnum.SUCCESS.getCode()).set(MatchingOrder::getMatchCallbackTime, LocalDateTime.now(ZoneId.systemDefault())).update();
    }

    /*
     * 更新匹配回调失败的状态
     * */
    @Override
    public boolean updateMatchFailed(String paymentOrder) {
        return lambdaUpdate().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).set(MatchingOrder::getMatchCallbackStatus, NotifyStatusEnum.FAILED.getCode()).update();
    }

    /*
     * 更新充值交易回调成功
     * */
    @Override
    public boolean updateTradeCollectionSuccess(String collectionOrder) {
        //获取订单创建时间
        LocalDateTime createTime = lambdaQuery().eq(MatchingOrder::getCollectionMerchantOrder, collectionOrder).select(MatchingOrder::getCreateTime).one().getCreateTime();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        //计算订单完成时长
        String completeDuration = DurationCalculatorUtil.orderCompleteDuration(createTime, now);
        return lambdaUpdate().eq(MatchingOrder::getCollectionMerchantOrder, collectionOrder).set(MatchingOrder::getCollectionTradeCallbackStatus, NotifyStatusEnum.SUCCESS.getCode()).set(MatchingOrder::getCollectionTradeCallbackTime, now).set(MatchingOrder::getCompleteDuration, completeDuration).update();
    }

    /*
     * 更新充值交易回调失败
     * */
    @Override
    public boolean updateTradeCollectionFailed(String collectionOrder) {
        return lambdaUpdate().eq(MatchingOrder::getCollectionMerchantOrder, collectionOrder).set(MatchingOrder::getCollectionTradeCallbackStatus, NotifyStatusEnum.FAILED.getCode()).update();
    }

    /*
     * 更新提现交易回调成功
     * */
    @Override
    public boolean updateTradePaymentSuccess(String paymentOrder) {
        //获取订单创建时间
        LocalDateTime createTime = lambdaQuery().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).select(MatchingOrder::getCreateTime).one().getCreateTime();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        //计算订单完成时长
        String completeDuration = DurationCalculatorUtil.orderCompleteDuration(createTime, now);
        return lambdaUpdate().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).set(MatchingOrder::getPaymentTradeCallbackStatus, NotifyStatusEnum.SUCCESS.getCode()).set(MatchingOrder::getPaymentTradeCallbackTime, now).set(MatchingOrder::getCompleteDuration, completeDuration).update();
    }

    /*
     * 更新提现交易回调失败
     * */
    @Override
    public boolean updateTradePaymentFailed(String paymentOrder) {
        return lambdaUpdate().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).set(MatchingOrder::getPaymentTradeCallbackStatus, NotifyStatusEnum.FAILED.getCode()).update();
    }

    /*
     * 查询10秒前所有交易成功并且未发送MQ的订单
     * */
    @Override
    public List<MatchingOrder> getTradeSuccessAndUnsent() {
        //查询条件 10秒前
        LocalDateTime tenSecondsAgo = LocalDateTime.now(ZoneId.systemDefault()).minusSeconds(10);
        return lambdaQuery().eq(MatchingOrder::getPaymentStatus, OrderStatusEnum.CONFIRMATION.getCode()).eq(MatchingOrder::getPayStatus, OrderStatusEnum.CONFIRMATION.getCode()).and(i -> i.eq(MatchingOrder::getCollectionTradeSend, SendStatusEnum.UNSENT.getCode()).or().eq(MatchingOrder::getPaymentTradeSend, SendStatusEnum.UNSENT.getCode())).lt(MatchingOrder::getCreateTime, tenSecondsAgo).select().list();
    }

    @Override
    public MatchingOrder getMatchingOrder(String matchingPlatformOrder) {
        return lambdaQuery().eq(MatchingOrder::getPlatformOrder, matchingPlatformOrder).one();
    }

    @Override
    public MatchingOrder getPaymentMatchingOrder(String paymentOrder) {
        return lambdaQuery().eq(MatchingOrder::getPaymentPlatformOrder, paymentOrder).or().eq(MatchingOrder::getPaymentMerchantOrder, paymentOrder).one();
    }


    @Override
    public MatchingOrder getMatchingOrderByCollection(String collectionOrder) {
        return lambdaQuery().eq(MatchingOrder::getCollectionPlatformOrder, collectionOrder).or().eq(MatchingOrder::getCollectionMerchantOrder, collectionOrder).one();
    }


    @Override
    @SneakyThrows
    public PageReturn<MatchingOrderPageListDTO> listPage(MatchingOrderReq req) {
        Page<MatchingOrder> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MatchingOrder> lambdaQuery = lambdaQuery();

        LambdaQueryWrapper<MatchingOrder> queryWrapper = new QueryWrapper<MatchingOrder>()
                .select("IFNULL(sum(order_submit_amount),0) as orderSubmitAmountTotal,IFNULL(sum(order_actual_amount), 0) as orderActualAmountTotal").lambda();

        lambdaQuery.orderByDesc(MatchingOrder::getId);
        if (!ObjectUtils.isEmpty(req.getId())) {
            lambdaQuery.eq(MatchingOrder::getId, req.getId());
            queryWrapper.eq(MatchingOrder::getId, req.getId());
        }
        if (!StringUtils.isEmpty(req.getCollectionPlatformOrder())) {
            lambdaQuery.eq(MatchingOrder::getCollectionPlatformOrder, req.getCollectionPlatformOrder());
            queryWrapper.eq(MatchingOrder::getCollectionPlatformOrder, req.getCollectionPlatformOrder());
        }
        if (!StringUtils.isEmpty(req.getCollectionPlatformOrder())) {
            lambdaQuery.eq(MatchingOrder::getCollectionPlatformOrder, req.getCollectionPlatformOrder());
            queryWrapper.eq(MatchingOrder::getCollectionPlatformOrder, req.getCollectionPlatformOrder());
        }
        if (!StringUtils.isEmpty(req.getPaymentPlatformOrder())) {
            lambdaQuery.eq(MatchingOrder::getPaymentPlatformOrder, req.getPaymentPlatformOrder());
            queryWrapper.eq(MatchingOrder::getPaymentPlatformOrder, req.getPaymentPlatformOrder());
        }
        // 新增买入、卖出会员id筛选
        if (!StringUtils.isEmpty(req.getCollectionMemberId())) {
            lambdaQuery.eq(MatchingOrder::getCollectionMemberId, req.getCollectionMemberId());
            queryWrapper.eq(MatchingOrder::getCollectionMemberId, req.getCollectionMemberId());
        }
        if (!StringUtils.isEmpty(req.getPaymentMemberId())) {
            lambdaQuery.eq(MatchingOrder::getPaymentMemberId, req.getPaymentMemberId());
            queryWrapper.eq(MatchingOrder::getPaymentMemberId, req.getPaymentMemberId());
        }
        if (!StringUtils.isEmpty(req.getKycAutoCompletionStatus())) {
            lambdaQuery.eq(MatchingOrder::getKycAutoCompletionStatus, req.getKycAutoCompletionStatus());
            queryWrapper.eq(MatchingOrder::getKycAutoCompletionStatus, req.getKycAutoCompletionStatus());
        }
        if (!StringUtils.isEmpty(req.getStatus())) {
            List<String> statusList = Arrays.asList(req.getStatus().split(","));
            statusList = new ArrayList<>(statusList);
            // 筛选人工审核中的订单
            if (statusList.contains(OrderStatusEnum.AUDITING.getCode())) {
                // 去除虚假状态 人工审核中
                statusList.remove(OrderStatusEnum.AUDITING.getCode());
                List<String> auditingStatusList = new ArrayList<>();
                // 确认中
                auditingStatusList.add(OrderStatusEnum.CONFIRMATION.getCode());
                // 确认超时
                auditingStatusList.add(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode());
                // 申诉中
                auditingStatusList.add(OrderStatusEnum.COMPLAINT.getCode());
                // 并且过期时间不为null
                lambdaQuery.and(w -> w.in(MatchingOrder::getStatus, auditingStatusList).ge(MatchingOrder::getAuditDelayTime, LocalDateTime.now()));
                queryWrapper.and(w -> w.in(MatchingOrder::getStatus, auditingStatusList).ge(MatchingOrder::getAuditDelayTime, LocalDateTime.now()));
            }
            if(!statusList.isEmpty()){
                lambdaQuery.or().in(MatchingOrder::getStatus, statusList);
                queryWrapper.or().in(MatchingOrder::getStatus, statusList);
            }
        }
        if (!StringUtils.isEmpty(req.getCreateTimeStart())) {
            lambdaQuery.ge(MatchingOrder::getCreateTime, req.getCreateTimeStart());
            queryWrapper.ge(MatchingOrder::getCreateTime, req.getCreateTimeStart());
        }
        if (!StringUtils.isEmpty(req.getCreateTimeEnd())) {
            lambdaQuery.le(MatchingOrder::getCreateTime, req.getCreateTimeEnd());
            queryWrapper.le(MatchingOrder::getCreateTime, req.getCreateTimeEnd());
        }
        if (!StringUtils.isEmpty(req.getPaymentTimeStart())) {
            lambdaQuery.ge(MatchingOrder::getPaymentTime, req.getPaymentTimeStart());
            queryWrapper.ge(MatchingOrder::getPaymentTime, req.getPaymentTimeStart());
        }
        if (!StringUtils.isEmpty(req.getPaymentTimeEnd())) {
            lambdaQuery.le(MatchingOrder::getPaymentTime, req.getPaymentTimeEnd());
            queryWrapper.le(MatchingOrder::getPaymentTime, req.getPaymentTimeEnd());
        }
        if (!ObjectUtils.isEmpty(req.getCompletionTimeStart())) {
            lambdaQuery.ge(MatchingOrder::getCompletionTime, req.getCompletionTimeStart());
            queryWrapper.ge(MatchingOrder::getCompletionTime, req.getCompletionTimeStart());
        }
        if (!ObjectUtils.isEmpty(req.getCompletionTimeEnd())) {
            lambdaQuery.le(MatchingOrder::getCompletionTime, req.getCompletionTimeEnd());
            queryWrapper.le(MatchingOrder::getCompletionTime, req.getCompletionTimeEnd());
        }
        // 查询风控标识
        if (!ObjectUtils.isEmpty(req.getRiskTag())) {
            String timeOutCode = RiskTagEnum.ORDER_TIME_OUT.getCode();
            String blackIpCode = RiskTagEnum.BLACK_IP.getCode();
            String normalCode = RiskTagEnum.Normal.getCode();
            if (req.getRiskTag().equals(timeOutCode)) {
                lambdaQuery.eq(MatchingOrder::getRiskTagTimeout, 1);
                queryWrapper.eq(MatchingOrder::getRiskTagTimeout, 1);
            } else if (req.getRiskTag().equals(blackIpCode)) {
                lambdaQuery.eq(MatchingOrder::getRiskTagBlack, 1);
                queryWrapper.eq(MatchingOrder::getRiskTagBlack, 1);
            } else if (req.getRiskTag().equals(normalCode)) {
                lambdaQuery.eq(MatchingOrder::getRiskTagTimeout, 0);
                queryWrapper.eq(MatchingOrder::getRiskTagTimeout, 0);
                lambdaQuery.eq(MatchingOrder::getRiskTagBlack, 0);
                queryWrapper.eq(MatchingOrder::getRiskTagBlack, 0);
            }
            // 列表无余额过低筛选
            else {
                lambdaQuery.eq(MatchingOrder::getId, -1);
                queryWrapper.eq(MatchingOrder::getId, -1);
            }
        }
        Page<MatchingOrder> finalPage = page;
        CompletableFuture<MatchingOrder> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<MatchingOrder>> matchPoolFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));

        CompletableFuture.allOf(totalFuture, matchPoolFuture);

        MatchingOrder totalInfo = totalFuture.get();


        JSONObject extent = new JSONObject();
        extent.put("orderSubmitAmountTotal", totalInfo.getOrderSubmitAmountTotal());
        extent.put("orderActualAmountTotal", totalInfo.getOrderActualAmountTotal());
        page = matchPoolFuture.get();
        BigDecimal orderSubmitAmountPageTotal = new BigDecimal(0);
        BigDecimal orderActualAmountPageTotal = new BigDecimal(0);

        List<MatchingOrderPageListDTO> listDTO = new ArrayList<MatchingOrderPageListDTO>();
        List<MatchingOrder> records = page.getRecords();
        for (MatchingOrder matchingOrder : records) {
            List<String> riskTag = new ArrayList<>();
            MatchingOrderPageListDTO matchingOrderDTO = new MatchingOrderPageListDTO();
            matchingOrderDTO.setCollectionPlatformOrder(matchingOrder.getCollectionPlatformOrder());
            matchingOrderDTO.setPaymentPlatformOrder(matchingOrder.getPaymentPlatformOrder());
            matchingOrderDTO.setOrderSubmitAmount(matchingOrder.getOrderSubmitAmount());
            matchingOrderDTO.setOrderActualAmount(matchingOrder.getOrderActualAmount());
            // 统计本业金额总计
            orderSubmitAmountPageTotal = orderSubmitAmountPageTotal.add(matchingOrder.getOrderSubmitAmount());
            orderActualAmountPageTotal = orderActualAmountPageTotal.add(matchingOrder.getOrderActualAmount());

            matchingOrderDTO.setId(matchingOrder.getId());
            matchingOrderDTO.setPaymentTime(matchingOrder.getPaymentTime());
            matchingOrderDTO.setUpiId(matchingOrder.getUpiId());
            matchingOrderDTO.setUpiName(matchingOrder.getUpiName());
            matchingOrderDTO.setUtr(matchingOrder.getUtr());
            matchingOrderDTO.setStatus(matchingOrder.getStatus());
//            // 人工审核状态
//            if (OrderStatusEnum.isAuditing(matchingOrder.getStatus(), matchingOrder.getAuditDelayTime())) {
//                matchingOrderDTO.setStatus(OrderStatusEnum.AUDITING.getCode());
//            }
            matchingOrderDTO.setCreateTime(matchingOrder.getCreateTime());
            matchingOrderDTO.setUpdateBy(matchingOrder.getUpdateBy());
            matchingOrderDTO.setCompletionTime(matchingOrder.getCompletionTime());
            matchingOrderDTO.setPaymentMemberId(matchingOrder.getPaymentMemberId());
            matchingOrderDTO.setCollectionMemberId(matchingOrder.getCollectionMemberId());
            matchingOrderDTO.setCompleteDuration(matchingOrder.getCompleteDuration());
            matchingOrderDTO.setDisplayAppealType(matchingOrder.getDisplayAppealType());
            matchingOrderDTO.setRiskTagBlack(matchingOrder.getRiskTagBlack());
            matchingOrderDTO.setRiskTagTimeout(matchingOrder.getRiskTagTimeout());
            matchingOrderDTO.setRiskOrderType(matchingOrder.getRiskOrderType());
            if (!ObjectUtils.isEmpty(matchingOrder.getRiskTagTimeout()) && matchingOrder.getRiskTagTimeout() != 0) {
                riskTag.add(RiskTagEnum.ORDER_TIME_OUT.getCode());
            }
            if (!ObjectUtils.isEmpty(matchingOrder.getRiskTagBlack()) && matchingOrder.getRiskTagBlack() != 0) {
                riskTag.add(RiskTagEnum.BLACK_IP.getCode());
            }
            if (riskTag.isEmpty()) {
                riskTag.add(RiskTagEnum.Normal.getCode());
            }
            matchingOrderDTO.setRiskTag(String.join(",", riskTag));
            // 计算审核时间
            LocalDateTime auditDelayTime = matchingOrder.getAuditDelayTime();
            LocalDateTime nowTime = LocalDateTime.now();
            String auditSeconds = null;
            if (ObjectUtils.isNotEmpty(auditDelayTime)
                    && OrderStatusEnum.isAuditing(matchingOrder.getStatus(), matchingOrder.getAuditDelayTime())
            ) {
                auditSeconds = DurationCalculatorUtil.secondsBetween(nowTime, auditDelayTime);
                if (Integer.parseInt(auditSeconds) < 0) {
                    auditSeconds = "0";
                }
            }
            matchingOrderDTO.setAuditSeconds(auditSeconds);
            listDTO.add(matchingOrderDTO);
        }
        extent.put("orderSubmitAmountPageTotal", orderSubmitAmountPageTotal);
        extent.put("orderActualAmountPageTotal", orderActualAmountPageTotal);
        return PageUtils.flush(page, listDTO, extent);
    }

    @Override
    public PageReturn<MatchingOrderExportDTO> listPageExport(MatchingOrderReq req) {
        PageReturn<MatchingOrderPageListDTO> matchingOrderReturn = listPage(req);

        List<MatchingOrderExportDTO> list = new ArrayList<>();
        List<MatchingOrderPageListDTO> data = matchingOrderReturn.getList();
        for (MatchingOrderPageListDTO item : data) {
            MatchingOrderExportDTO matchingOrderPageListDTO = new MatchingOrderExportDTO();
            BeanUtils.copyProperties(item, matchingOrderPageListDTO);
            String nameByCode = OrderStatusEnum.getNameByCode(item.getStatus());
            matchingOrderPageListDTO.setStatus(nameByCode);
            matchingOrderPageListDTO.setOrderActualAmount(item.getOrderActualAmount().toString());
            matchingOrderPageListDTO.setOrderSubmitAmount(item.getOrderSubmitAmount().toString());
            list.add(matchingOrderPageListDTO);
        }
        Page<MatchingOrderExportDTO> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        page.setTotal(matchingOrderReturn.getTotal());
        return PageUtils.flush(page, list);

    }


    @Override
    public MatchingOrderDTO getInfo(MatchingOrderIdReq req) {
        MatchingOrder matchingOrder = new MatchingOrder();
        BeanUtils.copyProperties(req, matchingOrder);
        matchingOrder = baseMapper.selectById(matchingOrder);
        PaymentOrder paymentOrder = paymentOrderMapper.selectPaymentForUpdate(matchingOrder.getPaymentPlatformOrder());
        if (matchingOrder == null) return null;
        MatchingOrderDTO matchingOrderDTO = new MatchingOrderDTO();
        BeanUtils.copyProperties(matchingOrder, matchingOrderDTO);
        if (ObjectUtils.isNotEmpty(paymentOrder)) {
            matchingOrderDTO.setMobileNumber(paymentOrder.getMobileNumber());
        }
        if (matchingOrder.getStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())) {
            matchingOrderDTO.setRemark(matchingOrder.getCancellationReason());
        }
        // 查询是否开启人工审核
        TradeManualConfigDTO manualReview = tradeConfigService.manualReview();
        matchingOrderDTO.setIsManualReview(manualReview.getIsManualReview());
        return matchingOrderDTO;

    }

//    public AppealOrderDTO appealDetail(MatchingOrderReq req){
//        List<AppealOrder> list = appealOrderService.lambdaQuery().eq(AppealOrder::getWithdrawOrderNo,req.getCollectionPlatformOrder()).or().eq(AppealOrder::getRechargeOrderNo,req.getPaymentPlatformOrder()).list();
//        AppealOrderDTO appealOrderDTO = new AppealOrderDTO();
//        if(list==null||list.size()<1) return null;
//        BeanUtils.copyProperties(list.get(0),appealOrderDTO);
//        return appealOrderDTO;
//    }


    @Override
    public MatchingOrderDTO update(MatchingOrderReq req) {
        MatchingOrder matchingOrder = new MatchingOrder();
        BeanUtils.copyProperties(req, matchingOrder);
        baseMapper.updateById(matchingOrder);
        MatchingOrderDTO matchingOrderDTO = new MatchingOrderDTO();
        BeanUtils.copyProperties(matchingOrder, matchingOrderDTO);
        return matchingOrderDTO;

    }


    @Override
    public MatchingOrderDTO getMatchingOrderTotal(MatchingOrderReq req) {
        QueryWrapper<MatchingOrder> queryWrapper = new QueryWrapper<>();

        queryWrapper.select(
                "sum(order_submit_amount) as orderSubmitAmount",
                "sum(order_actual_amount) as orderActualAmount"

        );
        if (req.getId() != null) {
            queryWrapper.eq("id", req.getId());
        }
        if (!StringUtils.isEmpty(req.getCollectionPlatformOrder())) {
            queryWrapper.eq("collection_platform_order", req.getCollectionPlatformOrder());
        }

        if (!StringUtils.isEmpty(req.getPaymentPlatformOrder())) {
            queryWrapper.eq("payment_platform_order", req.getPaymentPlatformOrder());
        }
        if (!StringUtils.isEmpty(req.getStatus())) {
            queryWrapper.eq("status", req.getStatus());
        }
        if (!StringUtils.isEmpty(req.getCreateTimeStart())) {
            queryWrapper.ge("create_time", req.getCreateTimeStart());
        }
        if (!StringUtils.isEmpty(req.getCreateTimeEnd())) {
            queryWrapper.le("create_time", req.getCreateTimeEnd());
        }
        if (!StringUtils.isEmpty(req.getPaymentTimeStart())) {
            queryWrapper.ge("payment_time", req.getPaymentTimeStart());
        }
        if (!StringUtils.isEmpty(req.getPaymentTimeEnd())) {
            queryWrapper.le("payment_time", req.getPaymentTimeEnd());
        }
        if (!StringUtils.isEmpty(req.getCompletionTimeStart())) {
            queryWrapper.ge("completion_time", req.getCompletionTimeStart());
        }
        if (!StringUtils.isEmpty(req.getCompletionTimeEnd())) {
            queryWrapper.le("completion_time", req.getCompletionTimeEnd());
        }

        Page<Map<String, Object>> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        baseMapper.selectMapsPage(page, queryWrapper);
        List<Map<String, Object>> records = page.getRecords();
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(records);
        List<MatchingOrderDTO> list = jsonArray.toJavaList(MatchingOrderDTO.class);
        MatchingOrderDTO matchingOrderDTO = list.get(0);

        return matchingOrderDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MatchingOrderDTO nopay(MatchingOrderAppealReq req) {
        String updateBy = UserContext.getCurrentUserName();
        MatchingOrder matchingOrder = new MatchingOrder();
        BeanUtils.copyProperties(req, matchingOrder);
        matchingOrder = baseMapper.selectById(matchingOrder);
        log.info("BI后台未支付,卖出订单号-{},买入订单号->{}", matchingOrder.getPaymentPlatformOrder(), matchingOrder.getCollectionPlatformOrder());
        AppealOrder appealOrder = appealOrderMapper.queryAppealOrderByNo(matchingOrder.getPaymentPlatformOrder());

        matchingOrder.setUpdateBy(updateBy);
        matchingOrder.setUpdateTime(LocalDateTime.now());
        matchingOrder.setRemark(req.getRemark());
        matchingOrder.setCompletedBy(updateBy);
        matchingOrder.setStatus(OrderStatusEnum.NO_PAY.getCode());
        matchingOrder.setAppealTime(LocalDateTime.now(ZoneId.systemDefault()));
        matchingOrder.setCompletionTime(LocalDateTime.now(ZoneId.systemDefault()));

        CancelOrderReq cancelOrderReq = new CancelOrderReq();
        cancelOrderReq.setPlatformOrder(matchingOrder.getCollectionPlatformOrder());
        cancelOrderReq.setReason(req.getRemark());
        MemberInfo memberInfo = memberInfoMapper.getMemberInfoById(matchingOrder.getCollectionMemberId());
        RestResult restResult = amountChangeUtil.cancelPurchaseOrder(cancelOrderReq, OrderStatusEnum.BUY_FAILED, OrderStatusEnum.FAIL, OrderStatusEnum.MANUAL_COMPLETION, memberInfo);
        if (!restResult.getCode().equals("1")) {
            throw new BizException("BI后台未支付失败");
        }
        CollectionOrder collectionOrder = collectionOrderMapper.getOrderByOrderNo(matchingOrder.getCollectionPlatformOrder());
        collectionOrder.setUpdateBy(updateBy);
        collectionOrder.setOrderStatus(OrderStatusEnum.BUY_FAILED.getCode());
        collectionOrder.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));
        collectionOrder.setRemark(req.getRemark());


        PaymentOrder paymentOrder = paymentOrderService.getPaymentOrderByOrderNo(matchingOrder.getPaymentPlatformOrder());
        paymentOrder.setUpdateBy(updateBy);
        paymentOrder.setOrderStatus(OrderStatusEnum.FAIL.getCode());
        paymentOrder.setUpdateTime(LocalDateTime.now(ZoneId.systemDefault()));
        paymentOrder.setRemark(req.getRemark());


        if (!ObjectUtils.isEmpty(appealOrder) && appealOrder.getAppealStatus().equals(1)) {
            log.info("未支付更新申诉订单");
            appealOrder.setAppealStatus(3);
            appealOrderMapper.updateById(appealOrder);
            matchingOrder.setAppealReviewBy(UserContext.getCurrentUserName());
            matchingOrder.setAppealReviewTime(LocalDateTime.now(ZoneId.systemDefault()));

            paymentOrder.setAppealReviewBy(UserContext.getCurrentUserName());
            paymentOrder.setAppealReviewTime(LocalDateTime.now(ZoneId.systemDefault()));

            collectionOrder.setAppealReviewBy(UserContext.getCurrentUserName());
            collectionOrder.setAppealReviewTime(LocalDateTime.now(ZoneId.systemDefault()));
        }
        baseMapper.updateById(matchingOrder);
        collectionOrderMapper.updateById(collectionOrder);
        paymentOrderService.updateById(paymentOrder);

        if (appealOrder != null) {
            // 变更会员信用分
            appealOrderService.changeCreditScore(Boolean.FALSE, collectionOrder.getMemberId(), paymentOrder.getMemberId(), appealOrder);
        } else {
            log.info("BI后台未支付, 申诉单为空无需更新信用分, 卖出订单号-{},买入订单号->{}", matchingOrder.getPaymentPlatformOrder(), matchingOrder.getCollectionPlatformOrder());
        }


        MatchingOrderDTO matchingOrderDTO = new MatchingOrderDTO();
        BeanUtils.copyProperties(matchingOrder, matchingOrderDTO);
        return matchingOrderDTO;


    }

    @Override
    public Map<String, String> getMatchMemberIdByPlatOrderIdList(List<String> platOrderIdList, boolean isBuy) {
        Map<String, String> matchMemberId = new HashMap<>();
        if (platOrderIdList.isEmpty()) {
            return matchMemberId;
        }
        LambdaQueryChainWrapper<MatchingOrder> queryChainWrapper = lambdaQuery();
        if (isBuy) {
            queryChainWrapper.in(MatchingOrder::getCollectionPlatformOrder, platOrderIdList);
        } else {
            queryChainWrapper.in(MatchingOrder::getPaymentPlatformOrder, platOrderIdList);
        }
        List<MatchingOrder> matchingOrders = baseMapper.selectList(queryChainWrapper.getWrapper());
        if (isBuy) {
            matchMemberId = matchingOrders.stream().collect(Collectors.toMap(MatchingOrder::getCollectionPlatformOrder, MatchingOrder::getPaymentMemberId));
        } else {
            matchingOrders.sort(Comparator.comparing(MatchingOrder::getCreateTime).reversed());
            matchMemberId = matchingOrders.stream().collect(Collectors.toMap(MatchingOrder::getPaymentPlatformOrder, MatchingOrder::getCollectionMemberId, (k1, k2) -> k1));
        }
        return matchMemberId;
    }

    /**
     * 根据买入订单、卖出订单批量汇总查询关联的撮合订单ID列表
     *
     * @param buyOrderIds
     * @param sellOrderIds
     * @return
     */
    @Override
    public Map<String, String> getMatchOrderIdsByPlatOrderId(List<String> buyOrderIds, List<String> sellOrderIds) {
        Set<String> orderSet = new HashSet<>();
        Set<String> buySet = new HashSet<>();
        Set<String> sellSet = new HashSet<>();
        if (!CollectionUtils.isEmpty(buyOrderIds)) {
            List<MatchingOrder> buyList = lambdaQuery().in(MatchingOrder::getCollectionPlatformOrder, buyOrderIds)
                    .select(MatchingOrder::getPlatformOrder)
                    .list();
            if (!CollectionUtils.isEmpty(buyList)) {
                buySet = buyList.stream().map(MatchingOrder::getPlatformOrder).collect(Collectors.toSet());
                orderSet.addAll(buySet);
            }
        }

        if (!CollectionUtils.isEmpty(sellOrderIds)) {
            List<MatchingOrder> sellList = lambdaQuery().in(MatchingOrder::getPaymentPlatformOrder, sellOrderIds)
                    .select(MatchingOrder::getPlatformOrder)
                    .list();
            if (!CollectionUtils.isEmpty(sellList)) {
                sellSet = sellList.stream().map(MatchingOrder::getPlatformOrder).collect(Collectors.toSet());
                orderSet.addAll(sellSet);
            }
        }
        Map<String, String> orderMap = Maps.newHashMapWithExpectedSize(orderSet.size());
        if (CollectionUtils.isEmpty(orderSet)) {
            return orderMap;
        }
        for (String platformOrder : orderSet) {
            if (buySet.contains(platformOrder) && sellSet.contains(platformOrder)) {
                orderMap.put(platformOrder, RiskOrderTypeEnum.ALL.getCode());
            } else if (buySet.contains(platformOrder)) {
                orderMap.put(platformOrder, RiskOrderTypeEnum.COLLECTION.getCode());
            } else {
                orderMap.put(platformOrder, RiskOrderTypeEnum.PAYMENT.getCode());
            }
        }
        return orderMap;
    }

    /**
     * 标记订单为指定的tag
     *
     * @param riskTag
     * @param platformOrderTags
     */
    @Override
    @Transactional
    public void taggingOrders(String riskTag, Map<String, String> platformOrderTags) {
        if (RiskTagEnum.getNameByCode(riskTag) == null) {
            return;
        }
        if (CollectionUtils.isEmpty(platformOrderTags)) {
            return;
        }
        Map<String, List<String>> typeIdMap = new HashMap<>();
        platformOrderTags.keySet().forEach(orderId -> {
            String riskOrderType = platformOrderTags.get(orderId);
            if (!typeIdMap.containsKey(riskOrderType)) {
                typeIdMap.put(riskOrderType, new ArrayList<>());
            }
            typeIdMap.get(riskOrderType).add(orderId);
        });
        for (Map.Entry<String, List<String>> entry : typeIdMap.entrySet()) {
            log.info("订单标记, 修改DB撮合订单状态, riskOrderType:{}, orderIds:{}", entry.getKey(), entry.getValue());
            LambdaUpdateChainWrapper<MatchingOrder> updateWrapper = lambdaUpdate().in(MatchingOrder::getPlatformOrder, entry.getValue());
            if (RiskTagEnum.BLACK_IP.getCode().equals(riskTag)) {
                updateWrapper.set(MatchingOrder::getRiskTagBlack, 1);
            }
            if (RiskTagEnum.ORDER_TIME_OUT.getCode().equals(riskTag)) {
                updateWrapper.set(MatchingOrder::getRiskTagTimeout, 1);
            }
            updateWrapper.set(MatchingOrder::getRiskOrderType, entry.getKey());
            updateWrapper.update();
        }
    }

    @SneakyThrows
    @Override
    public Page<RelationOrderDTO> relationOrderList(RelationshipOrderReq req) {

        Page<RelationOrderDTO> pageInfo = new Page<>();
        long page = (req.getPageNo() - 1) * req.getPageSize();
        long size = req.getPageSize();

        // 查询商户信息
        CompletableFuture<List<RelationOrderDTO>> listFuture = CompletableFuture.supplyAsync(() -> {
            return matchingOrderMapper.selectMyPage(page, size, req);
        });
        // 查询商户信息
        CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(() -> {
            return matchingOrderMapper.count(req);
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(listFuture, countFuture);
        allFutures.get();
        List<RelationOrderDTO> resultList = listFuture.get();
        for (RelationOrderDTO item : resultList) {
            if (org.apache.commons.lang3.StringUtils.isNotBlank(item.getMemberId()) && org.apache.commons.lang3.StringUtils.isNotBlank(item.getMerchantCode()) &&
                    item.getMemberId().contains(item.getMerchantCode())) {
                String externalMemberId = item.getMemberId().substring(item.getMerchantCode().length());
                item.setMemberId(externalMemberId);
            }
        }
        pageInfo.setRecords(resultList);
        pageInfo.setTotal(countFuture.get());
        return pageInfo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean manualReview(MatchingOrderManualReq req, ISellService sellService) {
        // 判断开关状态
        TradeManualConfigDTO manualReview = tradeConfigService.manualReview();
        if (manualReview.getIsManualReview() == 0) {
            return true;
        }
        // 获取撮合订单信息
        MatchingOrder matchingOrder = baseMapper.selectById(req.getId());
        if (OrderStatusEnum.SUCCESS.getCode().equals(matchingOrder.getStatus()) || OrderStatusEnum.WAS_CANCELED.getCode().equals(matchingOrder.getStatus())) {
            // 排除不需要处理的状态
            log.error("人工审核处理订单, 该订单无需处理, orderNo: {}, status:{}", matchingOrder.getPlatformOrder(), matchingOrder.getStatus());
            return true;
        }
        try {

            if (req.getIsPass().equals(ManualReviewStatus.PASS.getCode())) {
                // 人工审核暂时无通过操作
                // 审核通过确认订单
//                RestResult<BuyCompletedVo> buyCompletedVoRestResult = sellService.transactionSuccessHandler(matchingOrder.getPaymentPlatformOrder(), Long.parseLong(matchingOrder.getPaymentMemberId()), null, null, "2", null);
//                if (!buyCompletedVoRestResult.getCode().equals(ResultCode.SUCCESS.getCode())) {
//                    throw new Exception("人工审核确认订单失败: paymentOrder:{" + matchingOrder.getPaymentPlatformOrder() + "} , paymentMemberId:{" + matchingOrder.getPaymentMemberId() + "}" + ", buyCompletedVoRestMsg:{" + buyCompletedVoRestResult.getMsg() + "}");
//                }
            } else {
                // 获取拒绝原因
                String refuseReason = req.getReasonRemark();
                String nameByCode = RefuseReasonEnum.getNameByCode(req.getRefuseReason());
                if (Objects.nonNull(nameByCode)) {
                    refuseReason = nameByCode;
                }
                // 审核不通过 取消订单
                CancelOrderReq cancelOrderReq = new CancelOrderReq();
                cancelOrderReq.setPlatformOrder(matchingOrder.getCollectionPlatformOrder());
                cancelOrderReq.setReason(refuseReason);
                MemberInfo memberInfo = memberInfoMapper.getMemberInfoById(matchingOrder.getCollectionMemberId());
                RestResult cancelPurchaseOrderResult = amountChangeUtil.cancelPurchaseOrder(cancelOrderReq, OrderStatusEnum.BUY_FAILED, OrderStatusEnum.FAIL, OrderStatusEnum.MANUAL_COMPLETION, memberInfo);
                if (!cancelPurchaseOrderResult.getCode().equals(ResultCode.SUCCESS.getCode())) {
                    throw new Exception("人工审核取消订单失败: paymentOrder:{" + matchingOrder.getPaymentPlatformOrder() + "} , paymentMemberId:{" + matchingOrder.getPaymentMemberId() + "}" + " , cancelPurchaseOrderResultMsg:{" + cancelPurchaseOrderResult.getMsg() + "}");
                }
            }
            return true;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error("人工审核处理失败: req: {}, e: {}", req, e.getMessage());
            return false;
        }
    }

    /**
     * 取消会员确认超时的订单
     */
    @Override
    public void cancelConfirmTimeoutOrder(Integer startDays) {
        log.info("确认超时取消订单, 查询开始天数:{}", startDays);
        Integer queryStartDays = startDays == null ? 3 : startDays;
        LocalDateTime queryStartTime = LocalDateTime.now().minusDays(queryStartDays);
        long delayMinutes = arProperty.getConfirmTimeoutCancelOrderTime();
        LocalDateTime payStartTime = LocalDateTime.now().minusMinutes(delayMinutes);

        List<MatchingOrder> orderList = lambdaQuery().eq(MatchingOrder::getStatus, OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode())
                .gt(MatchingOrder::getCreateTime, queryStartTime)
                .lt(MatchingOrder::getPaymentTime, payStartTime)
                .select(MatchingOrder::getPaymentMemberId, MatchingOrder::getPaymentTime, MatchingOrder::getCollectionPlatformOrder, MatchingOrder::getCollectionMemberId).list();
        if (CollectionUtils.isEmpty(orderList)) {
            log.info("确认超时取消订单, 未查询到符合条件的超时订单, payStartTime:{}", payStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return;
        }
        List<MatchingOrder> timeoutOrderList = new ArrayList<>();
        for (MatchingOrder order : orderList) {
            MemberInfo sellMember = memberInfoService.getMemberInfoById(order.getPaymentMemberId());
            TradeConfigScheme schemeConfigByMemberTag = tradeConfigHelperUtil.getSchemeConfigByMemberTag(sellMember);
            Integer durationMinutes = schemeConfigByMemberTag.getSchemeConfirmExpirationTime();
            LocalDateTime overTime = payStartTime.minusMinutes(durationMinutes);
            if (order.getPaymentTime().isBefore(overTime)) {
                timeoutOrderList.add(order);
            }
        }
        log.info("确认超时取消订单, 查询到符合条件的超时订单数量:{}, payStartTime:{}", timeoutOrderList.size(), payStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        if (CollectionUtils.isEmpty(timeoutOrderList)) {
            return;
        }
        for (MatchingOrder order : timeoutOrderList) {
            // 随机生成延时时长，避免同一会员的多个订单同时取消
            int delaySeconds = new Random().nextInt(timeoutOrderList.size() * 10);
            TaskInfo cancleOrdertaskInfo = new TaskInfo(order.getCollectionPlatformOrder() + "|" + order.getPaymentMemberId(), TaskTypeEnum.AUTO_CANCEL_ORDER_ON_WALLET_MEMBER_CONFIRM_TIMEOUT.getCode(), System.currentTimeMillis());
            rabbitMQService.sendTimeoutTask(cancleOrdertaskInfo, delaySeconds * 1000L);
        }

    }

}