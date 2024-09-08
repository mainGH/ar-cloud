package org.ar.job.processor;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.utils.StringUtils;
import org.ar.common.redis.util.RedisUtils;
import org.ar.manager.entity.BiMemberReconciliation;
import org.ar.manager.entity.BiMerchantReconciliation;
import org.ar.manager.mapper.BiMemberReconciliationMapper;
import org.ar.wallet.entity.*;
import org.ar.wallet.mapper.MemberAccountChangeMapper;
import org.ar.wallet.mapper.MemberInfoMapper;
import org.ar.wallet.mapper.MerchantInfoMapper;
import org.ar.wallet.mapper.UsdtBuyOrderMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * 清洗会员对账数据
 *
 * @author Admin
 */
@Component("handleMemberReconciliationProcessor")
@Slf4j
@RequiredArgsConstructor
public class HandleMemberReconciliationProcessor implements BasicProcessor {

    private final BiMemberReconciliationMapper biMemberReconciliationMapper;
    private final MemberInfoMapper memberInfoMapper;
    private final MerchantInfoMapper merchantInfoMapper;
    private final UsdtBuyOrderMapper usdtBuyOrderMapper;
    private final MemberAccountChangeMapper memberAccountChangeMapper;

    private final RedisUtils redisUtils;

    /**
     * 批次大小
     */
    private static final int BATCH_SIZE = 1000;



    /**
     * 跨天时长(毫秒)
     */
    private static final int OVER_TIME = 30 * 60 * 1000;


    /**
     * dateType：today，interval
     * 1.当天执行重跑: {"dateType":"today","startTime":"2023-11-15 00:00:00", "endTime":"2023-11-15 23:59:59"}
     * 2.跨天数据处理 ：凌晨半个小时处理前天数据
     * 3.重跑一段时间数据：连续天数
     * 4.Job开关,停服开关控制,服务重启以免数据丢失
     * 5.自动跑今天数据
     *
     * @return
     */
    @Override
    public ProcessResult process(TaskContext context) {

        try {
            // 在线日志功能，可以直接在控制台查看任务日志，非常便捷
            OmsLogger omsLogger = context.getOmsLogger();
            omsLogger.info("清洗会员对账数据参数->{}", context.getJobParams());
            log.info("清洗会员对账数据参数->{}", context.getJobParams());
            String jobSwitch = (String) redisUtils.get(RedisConstants.JOB_SWITCH);
            if (!StringUtils.isEmpty(jobSwitch) && GlobalConstants.STATUS_OFF.equals(Integer.parseInt(jobSwitch))) {
                log.info("清洗会员对账数据Job未执行,Job开关已关闭.");
                return new ProcessResult(true, "Job switch turned off");
            }
            String dateStr = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()), GlobalConstants.DATE_FORMAT_DAY);
            // dateType为空代表自动
            biMemberReconciliationMapper.deleteByDateTime(dateStr);
            handleMemberReconciliation(dateStr);
        } catch (Exception e) {
            log.error("HandlePaymentOrderProcessor.process" + e.getMessage());
        }

        return new ProcessResult(true, "return success");
    }

    private void handleMemberReconciliation(String dateStr) throws ExecutionException, InterruptedException {
        updateBiMemberReconciliation(dateStr);
    }

    @NotNull
    private void updateBiMemberReconciliation(String dateStr) throws ExecutionException, InterruptedException {

        // 查询商户信息
        CompletableFuture<List<MerchantInfo>> merchantListFuture = CompletableFuture.supplyAsync(() -> {
            return merchantInfoMapper.selectList(null);
        });

        // 查询会员信息
        CompletableFuture<List<MemberInfo>> memberInfoFuture = CompletableFuture.supplyAsync(() -> {
            return memberInfoMapper.selectSumInfo();
        });

        // 查询usdt买入信息
        CompletableFuture<List<UsdtBuyOrder>> usdtInfoFuture = CompletableFuture.supplyAsync(() -> {
            return usdtBuyOrderMapper.selectSumInfo(dateStr);
        });

        // 查询会员领取奖励
        CompletableFuture<List<MemberInfo>> taskRewardFuture = CompletableFuture.supplyAsync(() -> {
            return memberInfoMapper.selectTaskReward();
        });

        // 查询会员人工上分
        CompletableFuture<List<MemberAccountChange>> accountChangeUpFuture = CompletableFuture.supplyAsync(() -> {
            return memberAccountChangeMapper.selectUpSumInfo(dateStr);
        });

        // 人工下分
        CompletableFuture<List<MemberAccountChange>> accountChangeDownFuture = CompletableFuture.supplyAsync(() -> {
            return memberAccountChangeMapper.selectDownSumInfo(dateStr);
        });
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(merchantListFuture, memberInfoFuture, usdtInfoFuture, accountChangeUpFuture,accountChangeDownFuture );
        allFutures.get();
        List<MerchantInfo> merchantInfoList = merchantListFuture.get();
        List<MemberInfo> memberInfoList = memberInfoFuture.get();
        List<UsdtBuyOrder> usdtBuyOrderList = usdtInfoFuture.get();
        List<MemberAccountChange> upList = accountChangeUpFuture.get();
        List<MemberAccountChange> downList = accountChangeDownFuture.get();
        List<MemberInfo> taskRewardList = taskRewardFuture.get();

        for (MerchantInfo merchantInfo : merchantInfoList) {
            BiMemberReconciliation biPaymentOrder = new BiMemberReconciliation();
            Optional<UsdtBuyOrder> usdt = usdtBuyOrderList.stream().filter(m -> !StringUtils.isEmpty(m.getMerchantName()) && m.getMerchantName().equals(merchantInfo.getUsername())).findFirst();
            if(usdt.isPresent()){
                UsdtBuyOrder usdtBuyOrder = usdt.get();
                biPaymentOrder.setUsdtBuyMoney(usdtBuyOrder.getArbNum());
            }
            Optional<MemberAccountChange> upOpt = upList.stream().filter(m -> !StringUtils.isEmpty(m.getMerchantName()) && m.getMerchantName().equals(merchantInfo.getUsername())).findFirst();
            if(upOpt.isPresent()){
                MemberAccountChange up = upOpt.get();
                biPaymentOrder.setMemberUp(up.getAmountChange());
            }
            Optional<MemberAccountChange> downOpt = downList.stream().filter(m -> !StringUtils.isEmpty(m.getMerchantName()) && m.getMerchantName().equals(merchantInfo.getUsername())).findFirst();
            if(downOpt.isPresent()){
                MemberAccountChange down = downOpt.get();
                biPaymentOrder.setMemberDown(down.getAmountChange());
            }

            Optional<MemberInfo> taskRewardOpt = taskRewardList.stream().filter(m -> !StringUtils.isEmpty(m.getMerchantName()) && m.getMerchantName().equals(merchantInfo.getUsername())).findFirst();
            if(downOpt.isPresent()){
                MemberInfo down = taskRewardOpt.get();
                biPaymentOrder.setTaskReward(down.getTotalTaskRewards());
            }

            Optional<MemberInfo> memberOpt = memberInfoList.stream().filter(m -> !StringUtils.isEmpty(m.getMerchantName()) && m.getMerchantName().equals(merchantInfo.getUsername())).findFirst();
            if(memberOpt.isPresent()){
                MemberInfo memberInfo = memberOpt.get();
                biPaymentOrder.setMemberBalance(memberInfo.getBalance());
                biPaymentOrder.setBuyReward(memberInfo.getTotalBuyBonus());
                biPaymentOrder.setSellReward(memberInfo.getTotalSellBonus());
            }
            biPaymentOrder.setMerchantName(merchantInfo.getUsername());
            biPaymentOrder.setMerchantCode(merchantInfo.getCode());
            biPaymentOrder.setMerchantType(merchantInfo.getMerchantType());

            biPaymentOrder.setPayMoney(merchantInfo.getTotalPayAmount());
            biPaymentOrder.setWithdrawMoney(merchantInfo.getTotalWithdrawAmount());
            biPaymentOrder.setPayFee(merchantInfo.getTotalPayFee());
            biPaymentOrder.setWithdrawFee(merchantInfo.getTotalWithdrawFee());
            biPaymentOrder.setPayMoney(merchantInfo.getTotalPayAmount());
            biPaymentOrder.setWithdrawMoney(merchantInfo.getTotalWithdrawAmount());
            biPaymentOrder.setWithdrawFee(merchantInfo.getTotalWithdrawFee());
            biPaymentOrder.setCreateTime(LocalDateTime.now());
            biPaymentOrder.setUpdateTime(LocalDateTime.now());
            BigDecimal diff = biPaymentOrder.getWithdrawMoney().subtract(biPaymentOrder.getPayFee()).subtract(biPaymentOrder.getPayMoney())
                    .subtract(biPaymentOrder.getWithdrawFee()).add(biPaymentOrder.getMemberUp()).subtract(biPaymentOrder.getMemberDown())
                    .add(biPaymentOrder.getUsdtBuyMoney()).add(biPaymentOrder.getBuyReward()).add(biPaymentOrder.getSellReward()).add(biPaymentOrder.getTaskReward())
                    .subtract(biPaymentOrder.getMemberBalance());
            biPaymentOrder.setMemberDiff(diff);
            biPaymentOrder.setDateTime(dateStr);
            biMemberReconciliationMapper.insert(biPaymentOrder);
        }



    }
}
