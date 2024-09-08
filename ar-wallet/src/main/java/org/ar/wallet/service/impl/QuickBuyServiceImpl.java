package org.ar.wallet.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.common.web.exception.BizException;
import org.ar.wallet.Enum.BuyStatusEnum;
import org.ar.wallet.Enum.MemberStatusEnum;
import org.ar.wallet.Enum.PayTypeEnum;
import org.ar.wallet.Enum.SwitchIdEnum;
import org.ar.wallet.entity.MatchPool;
import org.ar.wallet.req.BuyReq;
import org.ar.wallet.service.*;
import org.ar.wallet.util.RedisUtil;
import org.ar.wallet.vo.BuyListVo;
import org.ar.wallet.vo.MemberInformationVo;
import org.ar.wallet.vo.QuickBuyMatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.ar.common.core.result.ResultCode.*;

@Service
@Slf4j
public class QuickBuyServiceImpl implements QuickBuyService {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IMemberInfoService memberInfoService;
    @Autowired
    private IMatchPoolService matchPoolService;
    @Autowired
    private IBuyService buyService;
    @Autowired
    private ITradeConfigService tradeConfigService;

    @Autowired
    private IControlSwitchService controlSwitchService;

    /**
     * 匹配卖出订单
     *
     * @param amount
     * @return
     */
    @Override
    public QuickBuyMatchResult matchSellOrder(BigDecimal amount) {
        MemberInformationVo currentMemberInfo = memberInfoService.getCurrentMemberInfo().getData();
        validateAmount(currentMemberInfo, amount);
        List<BuyListVo> buyListVos = redisUtil.queryCurrentSellOrdersByAmount(currentMemberInfo.getMemberType(), currentMemberInfo.getMemberId(), amount, amount, 0, 100, null);
        if (!CollectionUtils.isEmpty(buyListVos)) {
            List<BuyListVo> c2cList = new ArrayList<>();
            List<BuyListVo> mcList = new ArrayList<>();
            for (BuyListVo order : buyListVos) {
                if (order.getPlatformOrder().startsWith("MC")) {
                    mcList.add(order);
                } else {
                    c2cList.add(order);
                }
            }
            if (!CollectionUtils.isEmpty(mcList)) {
                mcList.sort((o1, o2) -> o2.getCreditScore().compareTo(o1.getCreditScore()));
                return QuickBuyMatchResult.builder().orderNo(mcList.get(0).getPlatformOrder()).amount(mcList.get(0).getAmount()).payType(PayTypeEnum.INDIAN_UPI.getCode()).build();
            }
            if (!CollectionUtils.isEmpty(c2cList)) {
                c2cList.sort((o1, o2) -> o2.getCreditScore().compareTo(o1.getCreditScore()));
                return QuickBuyMatchResult.builder().orderNo(c2cList.get(0).getPlatformOrder()).amount(c2cList.get(0).getAmount()).payType(PayTypeEnum.INDIAN_UPI.getCode()).build();
            }
        }

        // 匹配区间金额
        MatchPool matchPoolOrder = matchPoolService.getMatchSellOrderByAmount(currentMemberInfo.getMemberId(), amount);
        if (matchPoolOrder != null) {
            // 兼容异常数据,防止无法买入
            BuyListVo orderDetails = redisUtil.getOrderDetails(matchPoolOrder.getMatchOrder());
            if (orderDetails != null) {
                return QuickBuyMatchResult.builder().orderNo(matchPoolOrder.getMatchOrder()).amount(amount).payType(PayTypeEnum.INDIAN_UPI.getCode()).build();
            }
        }

        // 查询推荐金额
        List<BuyListVo> littleList = redisUtil.queryCurrentSellOrdersByAmount(currentMemberInfo.getMemberType(), currentMemberInfo.getMemberId(), new BigDecimal(currentMemberInfo.getQuickBuyMinLimit()), amount, 0, 100, Boolean.TRUE);
        List<BuyListVo> bigList = redisUtil.queryCurrentSellOrdersByAmount(currentMemberInfo.getMemberType(), currentMemberInfo.getMemberId(), amount.add(BigDecimal.ONE), new BigDecimal(currentMemberInfo.getQuickBuyMaxLimit()), 0, 100, null);
        List<QuickBuyMatchResult.SuggestOrderItem> littleSuggestOrderItems = new ArrayList<>();
        List<QuickBuyMatchResult.SuggestOrderItem> bigSuggestOrderItems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(littleList)) {
            littleList = distinct(littleList, 3, Boolean.TRUE);
            littleList.forEach(o -> {
                QuickBuyMatchResult.SuggestOrderItem item = new QuickBuyMatchResult.SuggestOrderItem();
                item.setOrderNo(o.getPlatformOrder());
                item.setAmount(o.getAmount());
                littleSuggestOrderItems.add(item);
            });
            littleSuggestOrderItems.sort(Comparator.comparing(QuickBuyMatchResult.SuggestOrderItem::getAmount));
        }
        if (!CollectionUtils.isEmpty(bigList)) {
            bigList = distinct(bigList, 6, Boolean.FALSE);
            bigList.forEach(o -> {
                QuickBuyMatchResult.SuggestOrderItem item = new QuickBuyMatchResult.SuggestOrderItem();
                item.setOrderNo(o.getPlatformOrder());
                item.setAmount(o.getAmount());
                bigSuggestOrderItems.add(item);
            });
        }

        return QuickBuyMatchResult.builder().payType(PayTypeEnum.INDIAN_UPI.getCode()).littleSuggestItems(littleSuggestOrderItems).bigSuggestItems(bigSuggestOrderItems).build();

    }

    /**
     * 确认买入
     *
     * @param buyReq
     * @return
     */
    @Override
    @Transactional
    public RestResult confirmBuy(BuyReq buyReq, HttpServletRequest request) {
        MemberInformationVo currentMemberInfo = memberInfoService.getCurrentMemberInfo().getData();
        validateAmount(currentMemberInfo, buyReq.getAmount());
        return buyService.buyProcessor(buyReq, request);
    }

    private void validateAmount(MemberInformationVo memberInfo, BigDecimal amount) {

        if (controlSwitchService.isSwitchEnabled(SwitchIdEnum.REAL_NAME_VERIFICATION.getSwitchId())) {
            // memberInfo里存的认证值不一样,不能用枚举
            if ("0".equals(memberInfo.getAuthenticationStatus())) {
                throw new BizException(MEMBER_NOT_VERIFIED);
            }
        }
        if (amount.compareTo(new BigDecimal(memberInfo.getQuickBuyMinLimit())) < 0) {
            throw new BizException(NOT_MORE_THAN_MIN_LIMIT);
        }

        if (amount.compareTo(new BigDecimal(memberInfo.getQuickBuyMaxLimit())) > 0) {
            throw new BizException(NOT_LESS_THAN_MAX_LIMIT);
        }

        /*// 获取配置信息
        TradeConfig tradeConfig = tradeConfigService.getById(1);
        BigDecimal tradeCreditScoreLimit = tradeConfig.getTradeCreditScoreLimit();
        if (memberInfo.getCreditScore().compareTo(tradeCreditScoreLimit) < 0) {
            throw new BizException(LOW_CREDIT_SCORE);
        }*/

        //判断当前会员状态和买入状态是否可用
        if (BuyStatusEnum.DISABLE.getCode().equals(memberInfo.getBuyStatus())) {
            throw new BizException(MEMBER_BUY_STATUS_NOT_AVAILABLE);
        }

        if (MemberStatusEnum.DISABLE.getCode().equals(memberInfo.getStatus())) {
            throw new BizException(MEMBER_STATUS_NOT_AVAILABLE);
        }

    }

    private List<BuyListVo> distinct(List<BuyListVo> buyOrderList, int limit, boolean orderByDesc) {
        // 1.去重
        Map<Long, List<BuyListVo>> orderMap = buyOrderList.stream().collect(Collectors.groupingBy(o -> o.getAmount().longValue()));
        List<Long> sortedAmountKeys = new ArrayList<>(orderMap.keySet());
        sortedAmountKeys = sortedAmountKeys.stream().sorted().collect(Collectors.toList());
        if (orderByDesc) {
            Collections.reverse(sortedAmountKeys);
        }
        limit = sortedAmountKeys.size() < limit ? sortedAmountKeys.size() : limit;
        List<BuyListVo> resultList = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Long amount = sortedAmountKeys.get(i);
            // 2.相同金额的订单按信用分排序后只取信用分最高的
            List<BuyListVo> orderList = orderMap.get(amount);
            orderList.sort((o1, o2) -> o2.getCreditScore().compareTo(o1.getCreditScore()));
            resultList.add(orderList.get(0));
        }
        return resultList;
    }


}
