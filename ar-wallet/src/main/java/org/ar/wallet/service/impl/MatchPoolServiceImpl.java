package org.ar.wallet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ar.common.core.page.PageReturn;
import org.ar.common.mybatis.util.PageUtils;
import org.ar.common.pay.dto.MatchPoolListPageDTO;
import org.ar.common.pay.dto.PaymentOrderChildDTO;
import org.ar.common.pay.req.MatchPoolGetChildReq;
import org.ar.common.pay.req.MatchPoolListPageReq;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.entity.MatchPool;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.entity.PaymentOrder;
import org.ar.wallet.entity.UsdtBuyOrder;
import org.ar.wallet.mapper.MatchPoolMapper;
import org.ar.wallet.req.BuyListReq;
import org.ar.wallet.req.SellOrderListReq;
import org.ar.wallet.service.IMatchPoolService;
import org.ar.wallet.service.IPaymentOrderService;
import org.ar.wallet.service.ISellService;
import org.ar.wallet.util.RedisUtil;
import org.ar.wallet.vo.SellOrderListVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchPoolServiceImpl extends ServiceImpl<MatchPoolMapper, MatchPool> implements IMatchPoolService {

    @Autowired
    private IPaymentOrderService paymentOrderService;
    private final RedisUtil redisUtil;

    @Autowired
    private ISellService sellService;

    /**
     * 获取最老的匹配池订单列表
     *
     * @param buyListReq
     * @param memberId
     * @return {@link List}<{@link MatchPool}>
     */
    @Override
    public PageReturn<MatchPool> getOldestOrders(BuyListReq buyListReq, String memberId, Page<MatchPool> pageMatchPool) {

        LambdaQueryChainWrapper<MatchPool> lambdaQuery = lambdaQuery();

        //代付池订单状态为匹配中
        lambdaQuery.eq(MatchPool::getOrderStatus, OrderStatusEnum.BE_MATCHED.getCode());

        //排除自己的订单
        lambdaQuery.ne(MatchPool::getMemberId, memberId);

        //查询剩余金额大于100的订单
        lambdaQuery.ge(MatchPool::getRemainingAmount, new BigDecimal("100"));

        //--动态查询 最小金额
        if (buyListReq.getMinimumAmount() != null) {
            lambdaQuery.ge(MatchPool::getAmount, buyListReq.getMinimumAmount());
        }

        //--动态查询 最大金额
        if (buyListReq.getMaximumAmount() != null) {
            lambdaQuery.le(MatchPool::getMaximumAmount, buyListReq.getMaximumAmount());
        }

        //--动态查询 支付类型
        if (StringUtils.isNotEmpty(buyListReq.getPaymentType())) {
            lambdaQuery.eq(MatchPool::getPayType, buyListReq.getPaymentType());
        }

        //升序排序(查最老的订单)
        lambdaQuery.orderByAsc(MatchPool::getId);

        baseMapper.selectPage(pageMatchPool, lambdaQuery.getWrapper());
        List<MatchPool> records = pageMatchPool.getRecords();

        return PageUtils.flush(pageMatchPool, records);
    }

    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo
     * @return {@link MatchPool}
     */
    @Override
    public MatchPool getMatchPoolOrderByOrderNo(String orderNo) {
        return lambdaQuery().eq(MatchPool::getMatchOrder, orderNo).one();
    }


    @Override
    @SneakyThrows
    public PageReturn<MatchPoolListPageDTO> listPage(MatchPoolListPageReq req) {
        Page<MatchPool> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        LambdaQueryChainWrapper<MatchPool> lambdaQuery = lambdaQuery();
        lambdaQuery.orderByDesc(MatchPool::getCreateTime);
        // 新增统计金额字段总计字段
        LambdaQueryWrapper<MatchPool> queryWrapper = new QueryWrapper<MatchPool>()
                .select("IFNULL(sum(amount),0) as amountTotal,IFNULL(sum(sold_amount), 0) as soldAmountTotal,IFNULL(sum(remaining_amount),0) as sumRemainingAmount").lambda();

        if (StringUtils.isNotBlank(req.getMatchOrder())) {
            lambdaQuery.eq(MatchPool::getMatchOrder, req.getMatchOrder());
            queryWrapper.eq(MatchPool::getMatchOrder, req.getMatchOrder());
        }

        if (StringUtils.isNotBlank(req.getOrderStatus())) {
            lambdaQuery.eq(MatchPool::getOrderStatus, req.getOrderStatus());
            queryWrapper.eq(MatchPool::getOrderStatus, req.getOrderStatus());
        }


        if (!ObjectUtils.isEmpty(req.getCreateTimeStart())) {
            lambdaQuery.ge(MatchPool::getCreateTime, req.getCreateTimeStart());
            queryWrapper.ge(MatchPool::getCreateTime, req.getCreateTimeStart());
        }

        if (!ObjectUtils.isEmpty(req.getCreateTimeEnd())) {
            lambdaQuery.le(MatchPool::getCreateTime, req.getCreateTimeEnd());
            queryWrapper.le(MatchPool::getCreateTime, req.getCreateTimeEnd());
        }

        if (!ObjectUtils.isEmpty(req.getAmountStart())) {
            lambdaQuery.ge(MatchPool::getAmount, req.getAmountStart());
            queryWrapper.ge(MatchPool::getAmount, req.getAmountStart());
        }

        if (!ObjectUtils.isEmpty(req.getAmountEnd())) {
            lambdaQuery.le(MatchPool::getAmount, req.getAmountEnd());
            queryWrapper.le(MatchPool::getAmount, req.getAmountEnd());
        }

        if (!ObjectUtils.isEmpty(req.getMinimumAmountStart())) {
            lambdaQuery.ge(MatchPool::getMinimumAmount, req.getMinimumAmountStart());
            queryWrapper.ge(MatchPool::getMinimumAmount, req.getMinimumAmountStart());
        }

        if (!ObjectUtils.isEmpty(req.getMinimumAmountEnd())) {
            lambdaQuery.le(MatchPool::getMinimumAmount, req.getMinimumAmountEnd());
            queryWrapper.le(MatchPool::getMinimumAmount, req.getMinimumAmountEnd());
        }
        Page<MatchPool> finalPage = page;
        CompletableFuture<MatchPool> totalFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectOne(queryWrapper));
        CompletableFuture<Page<MatchPool>> matchPoolFuture = CompletableFuture.supplyAsync(() -> baseMapper.selectPage(finalPage, lambdaQuery.getWrapper()));

        CompletableFuture.allOf(totalFuture, matchPoolFuture);

        page = matchPoolFuture.get();
        MatchPool totalInfo = totalFuture.get();
        JSONObject extent = new JSONObject();
        extent.put("sumRemainingAmount", totalInfo.getSumRemainingAmount());
        extent.put("amountTotal", totalInfo.getAmountTotal());
        extent.put("soldAmountTotal", totalInfo.getSoldAmountTotal());
        List<MatchPool> records = page.getRecords();
        BigDecimal remainingAmountPageTotal = new BigDecimal(0);
        BigDecimal amountPageTotal = new BigDecimal(0);
        BigDecimal soldAmountPageTotal = new BigDecimal(0);
        List<MatchPoolListPageDTO> listDto = new ArrayList<MatchPoolListPageDTO>();
        for (MatchPool matchPool : records) {
            remainingAmountPageTotal = remainingAmountPageTotal.add(matchPool.getRemainingAmount());
            amountPageTotal = amountPageTotal.add(matchPool.getAmount());
            soldAmountPageTotal = soldAmountPageTotal.add(matchPool.getSoldAmount());
            MatchPoolListPageDTO matchPoolListPageDTO = new MatchPoolListPageDTO();
            BeanUtils.copyProperties(matchPool, matchPoolListPageDTO);
            listDto.add(matchPoolListPageDTO);
        }
        extent.put("remainingAmountPageTotal", remainingAmountPageTotal);
        extent.put("amountPageTotal", amountPageTotal);
        extent.put("soldAmountPageTotal", soldAmountPageTotal);
        // List<MatchPoolDTO> listDTO = walletMapStruct.matchPoolTransform(records);
        return PageUtils.flush(page, listDto, extent);
    }

    @Override
    public MatchPoolListPageDTO matchPooTotal(MatchPoolListPageReq req) {
        QueryWrapper<MatchPool> queryWrapper = new QueryWrapper<>();

        queryWrapper.select(
                "sum(amount) as amount"
        );
        if (req.getCreateTimeStart()!=null) {
            queryWrapper.ge("create_time", req.getCreateTimeStart());
        }
        if (req.getCreateTimeEnd()!=null) {
            queryWrapper.le("create_time", req.getCreateTimeEnd());
        }
        if (!StringUtils.isBlank(req.getOrderStatus())) {
            queryWrapper.eq("order_status", req.getOrderStatus());
        }
        if (!StringUtils.isBlank(req.getMatchOrder())) {
            queryWrapper.eq("match_order", req.getMatchOrder());
        }
        if (req.getMinimumAmountStart() != null) {
            queryWrapper.ge("mininum_amount", req.getMinimumAmountStart());
        }
        if (req.getMinimumAmountEnd() != null) {
            queryWrapper.le("mininum_amount", req.getMinimumAmountEnd());
        }
        if (req.getAmountStart() != null) {
            queryWrapper.le("amount", req.getAmountStart());
        }
        if (req.getAmountEnd() != null) {
            queryWrapper.le("amount", req.getAmountEnd());
        }

        Page<Map<String, Object>> page = new Page<>();
        page.setCurrent(req.getPageNo());
        page.setSize(req.getPageSize());
        baseMapper.selectMapsPage(page, queryWrapper);
        List<Map<String, Object>> records = page.getRecords();
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(records);
        List<MatchPoolListPageDTO> list = jsonArray.toJavaList(MatchPoolListPageDTO.class);
        MatchPoolListPageDTO matchPoolDTO = list.get(0);

        return matchPoolDTO;

    }

    @Override
    public List<PaymentOrderChildDTO> getChildren(MatchPoolGetChildReq req) {

        List<PaymentOrder> list = paymentOrderService.lambdaQuery().eq(PaymentOrder::getMatchOrder, req.getMatchOrder()).list();
        //List<PaymentOrderDTO> listDto = walletMapStruct.paymentOrderTransform(list);
        List<PaymentOrderChildDTO> listDto = new ArrayList<PaymentOrderChildDTO>();
        for (PaymentOrder paymentOrder : list) {
            PaymentOrderChildDTO paymentOrderChildDTO = new PaymentOrderChildDTO();
            BeanUtils.copyProperties(paymentOrder, paymentOrderChildDTO);
            listDto.add(paymentOrderChildDTO);
        }
        return listDto;
    }

    /**
     * 根据会员id 查询匹配池中 状态为 匹配中 进行中 匹配超时的订单
     *
     * @param memberId
     * @return {@link List}<{@link MatchPool}>
     */
    @Override
    public List<MatchPool> getOngoingSellOrder(String memberId) {
        return lambdaQuery()
                .eq(MatchPool::getMemberId, memberId)
                .in(MatchPool::getOrderStatus, new String[]{"1", "2", "14"})
                .list();
    }


    /**
     * 根据会员id 查询匹配池中 状态为 待匹配 匹配中 待支付 确认中 确认超时 申诉中 进行中 匹配超时的订单
     *
     * @param memberId
     * @return {@link List}<{@link MatchPool}>
     */
    @Override
    public List<MatchPool> getProcessingOrderByMemberId(String memberId) {
        return lambdaQuery()
                .eq(MatchPool::getMemberId, memberId)
                .in(MatchPool::getOrderStatus, new String[]{"1", "2", "3", "4", "5", "6"})
                .list();
    }

    /**
     * 根据订单号获取订单
     *
     * @return {@link List}<{@link MatchPool}>
     */
    @Override
    public List<MatchPool> getSellOrderList(List<String> platformOrderList) {
        List<String> orderList = platformOrderList.stream().filter(p -> p.startsWith("C2C")).collect(Collectors.toList());
        if(ObjectUtils.isEmpty(orderList)){
            return Collections.emptyList();
        }
        return lambdaQuery()
                .in(MatchPool::getMatchOrder, orderList)
                .list();
    }

    /**
     * 根据收款id获取正在匹配中的订单
     *
     * @param collectionInfoId
     * @return {@link Integer}
     */
    @Override
    public Integer getMatchingOrdersBycollectionId(Long collectionInfoId) {
        return lambdaQuery().eq(MatchPool::getCollectionInfoId, collectionInfoId).eq(MatchPool::getOrderStatus, OrderStatusEnum.BE_MATCHED.getCode()).count().intValue();
    }

    /**
     * 统计该会员的剩余金额 (状态为 匹配中或匹配超时)
     *
     * @param memberId
     * @return {@link BigDecimal}
     */
    @Override
    public BigDecimal sumRemainingAmount(String memberId) {

        QueryWrapper<MatchPool> wrapper = new QueryWrapper<>();

        wrapper.eq("member_id", memberId)
                .and(wq -> wq.eq("order_status", OrderStatusEnum.BE_MATCHED.getCode())
                        .or()
                        .eq("order_status", OrderStatusEnum.MATCH_TIMEOUT.getCode()));

        wrapper.select("SUM(remaining_amount) as sumRemainingAmount");

        MatchPool matchPool = getBaseMapper().selectOne(wrapper);

        return matchPool != null ? matchPool.getSumRemainingAmount() : BigDecimal.ZERO;
    }

    /**
     * 查询匹配池订单
     *
     * @param req
     * @param memberInfo
     * @return {@link List}<{@link SellOrderListVo}>
     */
    @Override
    public List<SellOrderListVo> getMatchPoolOrderList(SellOrderListReq req, MemberInfo memberInfo) {

        if (req == null) {
            req = new SellOrderListReq();
        }

        LambdaQueryChainWrapper<MatchPool> lambdaQuery = lambdaQuery();

        //查询当前会员的匹配池订单
        lambdaQuery.eq(MatchPool::getMemberId, memberInfo.getId());

        //--动态查询 订单状态
        if (!StringUtils.isEmpty(req.getOrderStatus())) {
            //对手动完成和已完成做兼容处理
            if (OrderStatusEnum.MANUAL_COMPLETION.getCode().equals(req.getOrderStatus()) || OrderStatusEnum.SUCCESS.getCode().equals(req.getOrderStatus())){
                lambdaQuery.nested(i -> i.eq(MatchPool::getOrderStatus, OrderStatusEnum.MANUAL_COMPLETION.getCode())
                        .or()
                        .eq(MatchPool::getOrderStatus, OrderStatusEnum.SUCCESS.getCode()));
            }else{
                lambdaQuery.eq(MatchPool::getOrderStatus, req.getOrderStatus());
            }
        }

        //--动态查询 时间 某天
        if (StringUtils.isNotEmpty(req.getDate())){
            LocalDate localDate = LocalDate.parse(req.getDate());
            LocalDateTime startOfDay = localDate.atStartOfDay();
            LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.MAX);

            lambdaQuery.ge(MatchPool::getCreateTime, startOfDay);
            lambdaQuery.le(MatchPool::getCreateTime, endOfDay);
        }

        // 倒序排序
        lambdaQuery.orderByDesc(MatchPool::getId);

        List<MatchPool> matchPoolList = lambdaQuery.list();

        //IPage＜实体＞转 IPage＜Vo＞
        ArrayList<SellOrderListVo> sellOrderListVoList = new ArrayList<>();
        for (MatchPool matchPoolOrder : matchPoolList) {

            //判断如果订单处于 进行中状态, 那么查询子订单是否还有未完成的订单
            if (matchPoolOrder.getOrderStatus().equals(OrderStatusEnum.IN_PROGRESS.getCode())) {
                //查询匹配池订单下面的子订单 并根据子订单状态 更新匹配池订单状态
                sellService.updateMatchPoolOrderStatus(matchPoolOrder.getMatchOrder());
            }

            SellOrderListVo sellOrderListVo = new SellOrderListVo();
            BeanUtil.copyProperties(matchPoolOrder, sellOrderListVo);

            //设置订单号
            sellOrderListVo.setPlatformOrder(matchPoolOrder.getMatchOrder());

            //设置匹配剩余时间
            sellOrderListVo.setMatchExpireTime(redisUtil.getMatchRemainingTime(sellOrderListVo.getPlatformOrder()));

            //判断如果订单是匹配中状态, 但是匹配剩余时间低于0 那么将返回前端的订单状态改为匹配超时
            if (sellOrderListVo.getOrderStatus().equals(OrderStatusEnum.BE_MATCHED.getCode()) && (sellOrderListVo.getMatchExpireTime() == null || sellOrderListVo.getMatchExpireTime() < 1)){
                sellOrderListVo.setOrderStatus(OrderStatusEnum.MATCH_TIMEOUT.getCode());
            }

            //将订单金额赋值给实际金额
            sellOrderListVo.setActualAmount(matchPoolOrder.getAmount());

            sellOrderListVoList.add(sellOrderListVo);
        }

        log.info("卖出订单列表: 会员账号: {}, 返回数据: {}", memberInfo.getMemberAccount(), sellOrderListVoList);

        return sellOrderListVoList;
    }


    /**
     * 根据匹配池订单id 将匹配池订单改为已完成状态
     *
     * @param id
     * @return {@link Boolean}
     */
    @Override
    public Boolean updateMatchPoolToSuccess(Long id) {
        return lambdaUpdate()
                .eq(MatchPool::getId, id)
                .set(MatchPool::getOrderStatus, OrderStatusEnum.SUCCESS.getCode())
                .update();
    }

    /**
     * 根据匹配池订单id 更改匹配池订单状态
     *
     * @param id
     * @return {@link Boolean}
     */
    @Override
    public Boolean updateMatchPoolStatus(Long id, String status) {
        return lambdaUpdate()
                .eq(MatchPool::getId, id)
                .set(MatchPool::getOrderStatus, status)
                .update();
    }

    @Override
    public MatchPool getMatchSellOrderByAmount(String memberId, BigDecimal amount) {
        return baseMapper.selectMatchSellOrderByAmount(memberId,amount);
    }
}
