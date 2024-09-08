package org.ar.wallet.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;
import org.ar.common.core.page.PageReturn;
import org.ar.common.pay.dto.MatchPoolListPageDTO;
import org.ar.common.pay.dto.PaymentOrderChildDTO;
import org.ar.common.pay.req.MatchPoolGetChildReq;
import org.ar.common.pay.req.MatchPoolListPageReq;
import org.ar.wallet.entity.MatchPool;
import org.ar.wallet.entity.MemberInfo;
import org.ar.wallet.req.BuyListReq;
import org.ar.wallet.req.SellOrderListReq;
import org.ar.wallet.vo.SellOrderListVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author
 */
public interface IMatchPoolService extends IService<MatchPool> {

    /**
     * 获取最老的代付池订单列表
     *
     * @param buyListReq
     * @param memberId
     * @return {@link List}<{@link MatchPool}>
     */
    PageReturn<MatchPool> getOldestOrders(BuyListReq buyListReq, String memberId, Page<MatchPool> pageMatchPool);


    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo
     * @return {@link MatchPool}
     */
    MatchPool getMatchPoolOrderByOrderNo(String orderNo);


    PageReturn<MatchPoolListPageDTO> listPage(MatchPoolListPageReq req);

    MatchPoolListPageDTO matchPooTotal(MatchPoolListPageReq req);


    List<PaymentOrderChildDTO> getChildren(MatchPoolGetChildReq req);

    /**
     * 根据会员id 查询匹配池中 状态为匹配中 或 进行中订单数>0的订单
     *
     * @param memberId
     * @return {@link List}<{@link MatchPool}>
     */
    List<MatchPool> getOngoingSellOrder(String memberId);

    List<MatchPool> getProcessingOrderByMemberId(String memberId);

    /**
     * 根据订单号获取订单列表
     * @param platformOrderList
     * @return
     */
    List<MatchPool> getSellOrderList(List<String> platformOrderList);

    /**
     * 根据收款id获取正在匹配中的订单
     *
     * @param collectionInfoId
     * @return {@link Integer}
     */
    Integer getMatchingOrdersBycollectionId(Long collectionInfoId);

    /**
     * 统计该会员的剩余金额 (状态为 匹配中或匹配超时)
     *
     * @param memberId
     * @return {@link BigDecimal}
     */
    BigDecimal sumRemainingAmount(String memberId);

    /**
     * 查询匹配池订单
     *
     * @param sellOrderListReq
     * @param memberInfo
     * @return {@link List}<{@link SellOrderListVo}>
     */
    List<SellOrderListVo> getMatchPoolOrderList(SellOrderListReq sellOrderListReq, MemberInfo memberInfo);


    /**
     * 根据匹配池订单id 将匹配池订单改为已完成状态
     *
     * @param id
     * @return {@link Boolean}
     */
    Boolean updateMatchPoolToSuccess(Long id);

    /**
     * 根据匹配池订单id 更改匹配池订单状态
     *
     * @param id
     * @return {@link Boolean}
     */
    Boolean updateMatchPoolStatus(Long id, String status);

    /**
     * 根据金额查询匹配会员的卖出订单
     *
     * @param memberId
     * @param amount
     * @return
     */
    MatchPool getMatchSellOrderByAmount(String memberId, BigDecimal amount);
}
