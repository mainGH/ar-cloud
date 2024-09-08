package org.ar.wallet.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.result.RestResult;
import org.ar.common.core.result.ResultCode;
import org.ar.common.redis.util.RedissonUtil;
import org.ar.common.web.exception.BizException;
import org.ar.wallet.Enum.*;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.*;
import org.ar.wallet.req.CancelOrderReq;
import org.ar.wallet.service.IBuyService;
import org.ar.wallet.service.IMatchPoolService;
import org.ar.wallet.service.IMemberAccountChangeService;
import org.ar.wallet.webSocket.NotifyOrderStatusChangeSend;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * @author Admin
 */
@Slf4j
@Component
public class AmountChangeUtil {

    @Resource
    MerchantInfoMapper merchantInfoMapper;
    @Resource
    AccountChangeMapper accountChangeMapper;
    @Resource
    RedissonUtil redissonUtil;
    @Resource
    IBuyService buyService;
    @Resource
    MemberAccountChangeMapper memberAccountChangeMapper;
    @Resource
    MemberInfoMapper memberInfoMapper;
    @Autowired
    private NotifyOrderStatusChangeSend notifyOrderStatusChangeSend;
    @Autowired
    CollectionOrderMapper collectionOrderMapper;
    @Autowired
    MatchingOrderMapper matchingOrderMapper;
    @Autowired
    PaymentOrderMapper paymentOrderMapper;
    @Autowired
    CollectionInfoMapper collectionInfoMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    IMemberAccountChangeService memberAccountChangeService;

    @Resource
    DataSourceTransactionManager  dataSourceTransactionManager;
    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    private MatchPoolMapper matchPoolMapper;

    @Autowired
    private IMatchPoolService matchPoolService;

    public AmountChangeUtil() {
    }

    /**
     * 更新商户余额并记录账变
     *
     * @param merchantCode    商户code
     * @param changeAmount    账变金额
     * @param changeModeEnum  账变类型
     * @param currentcy       币种
     * @param orderNo         订单号-平台订单号
     * @param orderCreateTime 订单创建时间       '2023-10-27'
     * @param merchantOrderNo 商户订单号
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public  Boolean insertChangeAmountRecord(
                                                String merchantCode,
                                                BigDecimal changeAmount,
                                                ChangeModeEnum changeModeEnum,
                                                String currentcy,
                                                String orderNo,
                                                AccountChangeEnum accountChangeEnum,
                                                String orderCreateTime,
                                                String remark,
                                                String merchantOrderNo
                                                )
    {

        log.info("开始记录商户余额账变,商户ID->{},账变金额->{},账变类型->{},订单号->{}", merchantCode, changeAmount,
                changeModeEnum.getName(), orderNo);
        String key = "ar-wallet" + merchantCode;
        RLock lock = redissonUtil.getLock(key);
        boolean req = false;
        BigDecimal commission = BigDecimal.ZERO;

        try {


            req = lock.tryLock(10, TimeUnit.SECONDS);
            if (req) {

                MerchantInfo merchantInfo = merchantInfoMapper.getMerchantInfoById(merchantCode);
                if (ObjectUtils.isEmpty(merchantInfo)) {
                    log.error("商户不存在,商户ID->{}", merchantCode);
                    throw new BizException(ResultCode.MERCHANT_NOT_EXIST);
                }
                if(accountChangeEnum.getCode().equals(AccountChangeEnum.PAYMENT.getCode())){
                    commission = merchantInfo.getTransferRate().multiply(changeAmount).divide(new BigDecimal(100), 2, RoundingMode.DOWN);
                }else if(accountChangeEnum.getCode().equals(AccountChangeEnum.COLLECTION.getCode())){
                    commission = merchantInfo.getPayRate().multiply(changeAmount).divide(new BigDecimal(100), 2, RoundingMode.DOWN);
                }
                AccountChange accountChange = new AccountChange();
                BigDecimal balance = merchantInfo.getBalance();
                accountChange.setBeforeChange(balance);
                accountChange.setAmountChange(changeAmount);
                BigDecimal afterAmount = BigDecimal.ZERO;
                if (changeModeEnum.getCode().equals(ChangeModeEnum.ADD.getCode())) {
                    afterAmount = balance.add(changeAmount);
                } else {
                    if (balance.compareTo(changeAmount) < 0) {
                        log.error("商户余额不足,商户ID->{}", merchantCode);
                        throw new BizException(ResultCode.MERCHANT_OUTSTANDING_BALANCE);
                    }
                    afterAmount = balance.subtract(changeAmount);
                    log.info("商户ID->{},账变金额->{},账变类型->{},订单号->{},账变前金额->{},账变后金额->{}", merchantCode, changeAmount,
                            changeModeEnum.getName(), orderNo, accountChange.getBeforeChange(), accountChange.getAfterChange());
                }

                accountChange.setMerchantCode(merchantCode);
                accountChange.setAfterChange(afterAmount);
                accountChange.setCurrentcy(currentcy);
                accountChange.setChangeMode(changeModeEnum.getCode());
                accountChange.setOrderNo(merchantOrderNo);
                accountChange.setChangeType(Integer.parseInt(accountChangeEnum.getCode()));
                accountChange.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));
                accountChange.setRemark(remark);
                accountChange.setCommission(commission);
                accountChange.setMerchantName(merchantInfo.getUsername());
                accountChange.setMerchantOrder(orderNo);
                // 代收-玩家支付
                if (accountChangeEnum.getCode().equals(AccountChangeEnum.COLLECTION.getCode())) {

                    // 统计累积业务数据

                    merchantInfo.setTotalPayAmount(merchantInfo.getTotalPayAmount().add(changeAmount));
                    merchantInfo.setTotalPayCount(merchantInfo.getTotalPayCount() + 1);
                    merchantInfo.setTotalPayFee(merchantInfo.getTotalPayFee().add(merchantInfo.getPayRate().multiply(changeAmount)
                            .divide(new BigDecimal("100")).setScale(2)));


                    // 代收-玩家提现
                } else if (accountChangeEnum.getCode().equals(AccountChangeEnum.PAYMENT.getCode())) {

                    // 统计累积业务数据

                    merchantInfo.setTotalWithdrawAmount(merchantInfo.getTotalWithdrawAmount().add(changeAmount));
                    merchantInfo.setTotalWithdrawCount(merchantInfo.getTotalWithdrawCount() + 1);
                    merchantInfo.setTotalWithdrawFee(merchantInfo.getTotalWithdrawFee().add(merchantInfo.getTransferRate().multiply(changeAmount)
                            .divide(new BigDecimal("100")).setScale(2)));


                    // 下分-商户提现
                } else if (accountChangeEnum.getCode().equals(AccountChangeEnum.WITHDRAW.getCode())) {
                    merchantInfo.setTransferDownAmount(merchantInfo.getTransferDownAmount().add(changeAmount));
                    merchantInfo.setTransferDownCount(merchantInfo.getTransferDownCount() + 1);
                    // 上分-商户充值
                } else if (accountChangeEnum.getCode().equals(AccountChangeEnum.RECHARGE.getCode())) {
                    merchantInfo.setTransferUpAmount(merchantInfo.getTransferUpAmount().add(changeAmount));
                    merchantInfo.setTransferUpCount(merchantInfo.getTransferUpCount() + 1);
                }
                merchantInfo.setBalance(afterAmount);
                int i = merchantInfoMapper.updateById(merchantInfo);
                if(i<1) throw  new Exception("更新商户余额失败");
                int j = accountChangeMapper.insert(accountChange);
                if(j<1) throw  new Exception("新增商户账变失败");
            }else{
                log.info("获取锁失败回滚操作,商户ID->{},账变金额->{},账变类型->{},订单号->{}", merchantCode, changeAmount,
                        changeModeEnum.getName(), orderNo);
                throw new Exception("获取锁失败,回滚操作");
            }
        } catch (Exception e) {
            log.error("商户ID->{},账变金额->{},账变类型->{},订单号->{},异常信息->{}", merchantCode, changeAmount,
                    changeModeEnum.getName(), orderNo, e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Boolean.FALSE;
        } finally {
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return Boolean.TRUE;
    }

    public Boolean insertChangeAmountRecord(String merchantCode,
                                            BigDecimal changeAmount,
                                            ChangeModeEnum changeModeEnum,
                                            String currentcy,
                                            String orderNo,
                                            AccountChangeEnum accountChangeEnum,
                                            String orderCreateTime,
                                            String merchantOrderNo
                                            ) {
        return insertChangeAmountRecord(merchantCode,
                                        changeAmount,
                                        changeModeEnum,
                                        currentcy,
                                        orderNo,
                                        accountChangeEnum,
                                        orderCreateTime,
                                        null,
                                        merchantOrderNo
                );
    }




    public Boolean insertMemberChangeAmountRecord(String mid, BigDecimal changeAmount, ChangeModeEnum changeModeEnum, String currentcy, String orderNo, MemberAccountChangeEnum memberAccountChangeEnum, String createBy) {
        return insertMemberChangeAmountRecord(mid, changeAmount, changeModeEnum, currentcy, orderNo, memberAccountChangeEnum, createBy, null);
    }

    /**
     * 更新会员余额并记录账变
     *
     * @param mid            商户ID
     * @param changeAmount   账变金额
     * @param changeModeEnum 账变类型
     * @param currentcy      币种
     * @param orderNo        订单号
     * @param orderNo        订单号
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Boolean insertMemberChangeAmountRecord(String mid, BigDecimal changeAmount, ChangeModeEnum changeModeEnum, String currentcy, String orderNo, MemberAccountChangeEnum memberAccountChangeEnum, String createBy, String remark) {
        log.info("开始记录会员余额账变,会员ID->{},账变金额->{},账变类型->{},订单号->{}", mid, changeAmount,
                changeModeEnum.getName(), orderNo);
        String key = "ar-wallet-sell" + mid;
        RLock lock = redissonUtil.getLock(key);
        boolean req = false;

        try {
            req = lock.tryLock(10, TimeUnit.SECONDS);
            if (req) {

                MemberInfo memberInfo = memberInfoMapper.getMemberInfoById(mid);
                if (ObjectUtils.isEmpty(memberInfo)) {
                    log.error("会员不存在,会员ID->{}", mid);
                    throw new BizException(ResultCode.MEMBER_NOT_EXIST);
                }


                MemberAccountChange accountChange = new MemberAccountChange();
                BigDecimal balance = memberInfo.getBalance();
                BigDecimal frozenAmount = memberInfo.getBiFrozenAmount();
                accountChange.setBeforeChange(balance);
                accountChange.setAmountChange(changeAmount);
                BigDecimal afterAmount = BigDecimal.ZERO;
                BigDecimal afterFrozenAmount = BigDecimal.ZERO;
                // 后台冻结金额修改标识
                String frozenAmountFlag = null;
                if (changeModeEnum.getCode().equals(ChangeModeEnum.ADD.getCode())) {
                    // 回退
                    if(memberAccountChangeEnum.getCode().equals(MemberAccountChangeEnum.UNFREEZE.getCode())){
                        afterFrozenAmount = frozenAmount.subtract(changeAmount);
                        frozenAmountFlag = "1";
                    }
                    afterAmount = balance.add(changeAmount);
                } else {
                    if (balance.compareTo(changeAmount) == -1) {
                        log.error("会员余额不足,会员ID->{}", mid);
                        throw new BizException(ResultCode.MEMBER_OUTSTANDING_BALANCE);
                    }
                    if(memberAccountChangeEnum.getCode().equals(MemberAccountChangeEnum.FREEZE.getCode())){
                        afterFrozenAmount = frozenAmount.add(changeAmount);
                        frozenAmountFlag = "1";
                    }
                    afterAmount = balance.subtract(changeAmount);
                    log.info("会员ID->{},账变金额->{},账变类型->{},订单号->{},账变前金额->{},账变后金额->{}", mid, changeAmount,
                            changeModeEnum.getName(), orderNo, accountChange.getBeforeChange(), accountChange.getAfterChange());
                }


                accountChange.setMid(mid);
                accountChange.setAfterChange(afterAmount);
                accountChange.setCurrentcy(currentcy);
                accountChange.setChangeMode(changeModeEnum.getCode());
                accountChange.setOrderNo(orderNo);
                accountChange.setChangeType(memberAccountChangeEnum.getCode());
                accountChange.setCreateTime(LocalDateTime.now(ZoneId.systemDefault()));
                accountChange.setCreateBy(createBy);
                accountChange.setUpdateBy(createBy);
                accountChange.setRemark(remark);
                // 账变记录新增三个字段
                if(!memberInfo.getMemberType().equals(MemberTypeEnum.WALLET_MEMBER.getCode())
                        && org.apache.commons.lang3.StringUtils.isNotBlank(memberInfo.getMemberId())
                        && org.apache.commons.lang3.StringUtils.isNotBlank(memberInfo.getMerchantCode())){
                    String externalMemberId = memberInfo.getMemberId().substring(memberInfo.getMerchantCode().length());
                    accountChange.setMemberId(externalMemberId);
                }
                // 设置商户名称
                if(!ObjectUtils.isEmpty(memberInfo.getMerchantName())){
                    accountChange.setMerchantName(memberInfo.getMerchantName());
                }

                // 设置会员账号
                if(!ObjectUtils.isEmpty(memberInfo.getMemberAccount())){
                    accountChange.setMemberAccount(memberInfo.getMemberAccount());
                }

                int i = memberInfoMapper.updateBalanceById(accountChange.getAfterChange(), mid, frozenAmountFlag, afterFrozenAmount);
                if(i<1) throw new Exception("更新会员余额失败");
                int j = memberAccountChangeMapper.insert(accountChange);
                if(j<1) throw new Exception("新增会员账变");

            }else{
                log.info("获取锁失败回滚操作,会员ID->{},账变金额->{},账变类型->{},订单号->{}", mid, changeAmount,
                        changeModeEnum.getName(), orderNo);
               throw   new Exception("获取锁失败回滚操作");
            }
        } catch (Exception e) {
            log.error("会员ID->{},账变金额->{},账变类型->{},订单号->{},异常信息->{}", mid, changeAmount,
                    changeModeEnum.getName(), orderNo, e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Boolean.FALSE;
        } finally {
            if (req && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return Boolean.TRUE;
    }


    /**
     * 取消买入订单处理
     *
     * @param cancelOrderReq
     * @return {@link RestResult}
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public RestResult cancelPurchaseOrder(CancelOrderReq cancelOrderReq,
                                          OrderStatusEnum buyStatus,
                                          OrderStatusEnum sellStatus,
                                          OrderStatusEnum matchStatus,
                                          MemberInfo memberInfo) {

        return buyService.cancelPurchaseOrder(cancelOrderReq, buyStatus, sellStatus, matchStatus, memberInfo);
    }

    public Boolean collectionOrderToWasCanceled(CollectionOrder collectionOrder, CancelOrderReq cancelOrderReq, OrderStatusEnum orderStatusEnum) {
        //更新订单状态为: 已取消
        collectionOrder.setOrderStatus(orderStatusEnum.getCode());
        // 添加取消原因
        collectionOrder.setCancellationReason(cancelOrderReq.getReason());

        boolean b = collectionOrderMapper.updateById(collectionOrder) > 0;

        log.info("取消买入订单处理 更新买入订单信息: {}, req: {}, sql执行结果: {}", collectionOrder, cancelOrderReq, b);

        return b;
    }

    /**
     * 取消买入订单 更新撮合列表状态为: 取消支付
     *
     * @param matchingOrder
     * @return {@link Boolean}
     */
    public Boolean matchingOrderToWasCanceled(MatchingOrder matchingOrder, CancelOrderReq cancelOrderReq, OrderStatusEnum orderStatusEnum) {
        //更新撮合列表订单状态为: 已取消
        matchingOrder.setStatus(orderStatusEnum.getCode());

        //填写取消原因
        matchingOrder.setCancellationReason(cancelOrderReq.getReason());

        boolean b = matchingOrderMapper.updateById(matchingOrder) > 0;

        log.info("取消买入订单 更新撮合列表订单: {}, sql执行结果: {}", matchingOrder, b);

        return b;
    }


    /**
     * 取消买入订单 更新收款信息
     *
     * @param collectionInfo
     * @return {@link Boolean}
     */
    public Boolean updateCollectionInfo(CollectionInfo collectionInfo, PaymentOrder paymentOrder) {

        //减去今日收款金额
        collectionInfo.setTodayCollectedAmount(collectionInfo.getTodayCollectedAmount().subtract(paymentOrder.getAmount()));

        //今日收款笔数-1
        collectionInfo.setTodayCollectedCount(collectionInfo.getTodayCollectedCount() - 1);

        //更新收款信息
        return collectionInfoMapper.updateById(collectionInfo) > 0;
    }


    /**
     * 获取当前会员信息
     *
     * @return {@link MemberInfo}
     */
    public MemberInfo getMemberInfo(Long buyMemberId) {

        LambdaQueryWrapper<MemberInfo> sectionQueryWrapper = new LambdaQueryWrapper<MemberInfo>();
        sectionQueryWrapper.eq(MemberInfo::getId, buyMemberId).eq(MemberInfo::getDeleted, 0);

        if (buyMemberId != null) {
            return memberInfoMapper.selectOne(sectionQueryWrapper);
        }

        log.error("获取当前会员信息失败: 会员id为null");
        return null;
    }
}
