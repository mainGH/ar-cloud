package org.ar.job.processor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.ar.common.core.constant.GlobalConstants;
import org.ar.common.core.constant.RedisConstants;
import org.ar.common.core.utils.StringUtils;
import org.ar.common.redis.util.RedisUtils;
import org.ar.manager.entity.*;
import org.ar.manager.mapper.*;
import org.ar.wallet.Enum.AppealStatusEnum;
import org.ar.wallet.Enum.OrderStatusEnum;
import org.ar.wallet.entity.AppealOrder;
import org.ar.wallet.entity.CollectionOrder;
import org.ar.wallet.entity.PaymentOrder;
import org.ar.wallet.mapper.AppealOrderMapper;
import org.ar.wallet.mapper.PaymentOrderMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


/**
 * 清洗卖出订单数据
 *
 * @author Admin
 */
@Component("HandleWithdrawOrderProcessor")
@Slf4j
@RequiredArgsConstructor
public class HandleWithdrawOrderProcessor implements BasicProcessor {

    private final BiWithdrawOrderDailyMapper biWithdrawOrderDailyMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final BiWithdrawOrderMonthMapper biWithdrawOrderMonthMapper;
    private final AppealOrderMapper appealOrderMapper;

    private final RedisUtils redisUtils;

    /**
     * 批次大小
     */
    private static final int BATCH_SIZE = 1000;

    private static final String TODAY = "today";


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
            omsLogger.info("清洗代付订单数据参数->{}", context.getJobParams());
            log.info("清洗代付订单数据参数->{}", context.getJobParams());
            String jobSwitch = (String) redisUtils.get(RedisConstants.JOB_SWITCH);
            if (!StringUtils.isEmpty(jobSwitch) && GlobalConstants.STATUS_OFF.equals(Integer.parseInt(jobSwitch))) {
                log.info("清洗卖出订单数据Job未执行,Job开关已关闭.");
                return new ProcessResult(true, "Job switch turned off");
            }
            String params = !StringUtils.isEmpty(context.getJobParams()) ? context.getJobParams() : context.getInstanceParams();
            JSONObject jsonObject = StringUtils.isEmpty(params) ? new JSONObject() : (JSONObject) JSONObject.parse(params);
            String startTime = jsonObject.getString("startTime");
            String endTime = jsonObject.getString("endTime");

            String dateType = jsonObject.getString("dateType");
            String dateStr = DateUtil.format(LocalDateTime.now(ZoneId.systemDefault()).plusDays(-1), GlobalConstants.DATE_FORMAT_DAY);
            // dateType为空代表自动
            if (StringUtils.isEmpty(dateType)) {
                biWithdrawOrderDailyMapper.deleteDailyByDateTime(dateStr);
                handleCollectionOrder(dateStr, startTime, endTime, dateType);

            } else {
                // 自定义时间
                log.info("进入自定义时间");
                LocalDateTime startDate = DateUtil.parseLocalDateTime(startTime, GlobalConstants.DATE_FORMAT);
                LocalDateTime endDate = DateUtil.parseLocalDateTime(endTime, GlobalConstants.DATE_FORMAT);
                long value = startDate.until(endDate, ChronoUnit.DAYS);

                for (int i = 0; i <= value; i++) {
                    String dateStr1 = DateUtil.format(startDate.plusDays(i), GlobalConstants.DATE_FORMAT_DAY);
                    biWithdrawOrderDailyMapper.deleteDailyByDateTime(dateStr1);
                    handleCollectionOrder(dateStr1, null, null, dateType);
                }

                // 重新统计月数据
                long monthVal = startDate.until(endDate, ChronoUnit.MONTHS);

                for (int i = 0; i <= monthVal; i++) {
                    String monthStr = DateUtil.format(startDate.plusMonths(i), GlobalConstants.DATE_FORMAT_MONTH);
                    biWithdrawOrderMonthMapper.deleteMonthByDateTime(monthStr);
                    List<BiWithdrawOrderMonth> biWithdrawOrderMonthList = biWithdrawOrderMonthMapper.selectDataInfoByMonth(monthStr);
                    for (BiWithdrawOrderMonth item : biWithdrawOrderMonthList) {
                        item.setDateTime(monthStr);
                        biWithdrawOrderMonthMapper.updateByDateTime(item);
                    }
                }


            }
        } catch (Exception e) {
            log.error("HandleWithdrawOrderProcessor.process" + e.getMessage());
        }

        return new ProcessResult(true, "return success");
    }

    private void handleCollectionOrder(String dateStr, String startTime, String endTime, String dateType) throws ExecutionException, InterruptedException {
        int pageNo = 1;
        long totalSize = 0;
        Page<PaymentOrder> page = new Page<>();
        page.setCurrent(pageNo);
        page.setSize(BATCH_SIZE);
        // startTime <= time < endTime
        page = updateBiWithdrawOrder(dateStr, startTime, endTime, page, dateType);
        List<PaymentOrder> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            log.info("查询代付记录数为空,直接返回");
            return;
        }
        if (page.getTotal() > BATCH_SIZE && page.getTotal() % BATCH_SIZE > 0) {
            totalSize = (page.getTotal() / BATCH_SIZE) + 1;
            log.info("总记录数大于批次,且余数大于0,totalSize->{}", totalSize);
        } else if (page.getTotal() > BATCH_SIZE && page.getTotal() % BATCH_SIZE <= 0) {
            totalSize = (page.getTotal() / BATCH_SIZE);
            log.info("总记录数大于批次,且余数等于0,totalSize->{}", totalSize);
        }
        for (int i = 0; i < totalSize; i++) {
            pageNo++;
            page.setCurrent(pageNo);
            page.setSize(BATCH_SIZE);
            updateBiWithdrawOrder(dateStr, startTime, endTime, page, dateType);
        }
    }

    @NotNull
    private Page<PaymentOrder> updateBiWithdrawOrder(String dateStr, String startTime, String endTime, Page<PaymentOrder> page, String dateType) throws ExecutionException, InterruptedException {
        String dateTmp = DateUtil.format(LocalDateTime.now(), GlobalConstants.DATE_FORMAT_MINUTE);
        String todayStr = DateUtil.format(LocalDateTime.now(), GlobalConstants.DATE_FORMAT_DAY);
        LambdaQueryChainWrapper<PaymentOrder> lambdaQuery2 = new LambdaQueryChainWrapper<>(paymentOrderMapper);
        lambdaQuery2.ge(PaymentOrder::getUpdateTime, dateStr + " 00:00:00");
        lambdaQuery2.le(PaymentOrder::getUpdateTime, dateStr + " 23:59:59");

        LambdaQueryChainWrapper<PaymentOrder> lambdaQuery = new LambdaQueryChainWrapper<>(paymentOrderMapper);
        lambdaQuery.ge(PaymentOrder::getCreateTime, dateStr + " 00:00:00");
        lambdaQuery.le(PaymentOrder::getCreateTime, dateStr + " 23:59:59");

        // 查询申诉订单数量
        CompletableFuture<List<AppealOrder>> appealNumFuture = CompletableFuture.supplyAsync(() -> {
            return appealOrderMapper.selectAppealNum(dateStr, 1);
        });

        // 查询申诉订单数量
        CompletableFuture<Integer> totalFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.selectCount(lambdaQuery.getWrapper());
        });

        // 查询申诉订单总数量
        CompletableFuture<Long> appealTotalNumFuture = CompletableFuture.supplyAsync(() -> {
            return appealOrderMapper.selectAppealTotalNum(dateStr, 1);
        });

        // 查询申诉订单数量
        CompletableFuture<Long> appealWrongAmountNumFuture = CompletableFuture.supplyAsync(() -> {
            return appealOrderMapper.selectWrongAmountNum(dateStr, 1, 2);
        });

        Page<PaymentOrder> finalPage = page;
        CompletableFuture<Page<PaymentOrder>> pageFuture = CompletableFuture.supplyAsync(() -> {
            return paymentOrderMapper.selectPage(finalPage, lambdaQuery2.getWrapper());
        });
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(appealNumFuture, appealTotalNumFuture, pageFuture, totalFuture, appealWrongAmountNumFuture);
        allFutures.get();
        page = pageFuture.get();
        List<AppealOrder> appealList = appealNumFuture.get();
        Integer totalCount = totalFuture.get();
        log.info("清洗卖出订单总记录数->{}", page.getRecords().size());
        List<PaymentOrder> records = page.getRecords();


        BiWithdrawOrderDaily biWithdrawOrder = new BiWithdrawOrderDaily();
        for (PaymentOrder paymentOrder : records) {
            if (paymentOrder.getOrderStatus().equals(OrderStatusEnum.SUCCESS.getCode()) || paymentOrder.getOrderStatus().equals(OrderStatusEnum.MANUAL_COMPLETION.getCode())) {
                biWithdrawOrder.setMoney(biWithdrawOrder.getMoney().add(paymentOrder.getAmount()));
                biWithdrawOrder.setActualMoney(biWithdrawOrder.getActualMoney().add(paymentOrder.getActualAmount()));
                biWithdrawOrder.setTotalFee(biWithdrawOrder.getTotalFee().add(paymentOrder.getBonus()));
                // 计算订单状态为成功数量
                biWithdrawOrder.setSuccessOrderNum(biWithdrawOrder.getSuccessOrderNum() + 1);
                // 匹配总时长
                if (!StringUtils.isEmpty(paymentOrder.getMatchDuration())) {
                    biWithdrawOrder.setMatchDuration(biWithdrawOrder.getMatchDuration() + Long.parseLong(paymentOrder.getMatchDuration()));
                }

                // 完成总时长
                if (!StringUtils.isEmpty(paymentOrder.getCompleteDuration())) {
                    biWithdrawOrder.setFinishDuration(biWithdrawOrder.getFinishDuration() + Long.parseLong(paymentOrder.getCompleteDuration()));
                }

            }

            if (!StringUtils.isEmpty(paymentOrder.getMatchTimeout()) && paymentOrder.getMatchTimeout().equals(1)) {
                biWithdrawOrder.setOverTimeNum(biWithdrawOrder.getOverTimeNum() + 1);
            }
            if (!StringUtils.isEmpty(paymentOrder.getCancelMatching()) && paymentOrder.getCancelMatching().equals(1)) {
                biWithdrawOrder.setCancelMatchNum(biWithdrawOrder.getCancelMatchNum() + 1);
            }
            if (!StringUtils.isEmpty(paymentOrder.getContinueMatching()) && paymentOrder.getContinueMatching().equals(1)) {
                biWithdrawOrder.setContinueMatchNum(biWithdrawOrder.getContinueMatchNum() + 1);
            }
            if(paymentOrder.getOrderStatus().equals(OrderStatusEnum.CONFIRMATION_TIMEOUT.getCode())){
                biWithdrawOrder.setConfirmOverTime(biWithdrawOrder.getConfirmOverTime() + 1);
            }
            if(paymentOrder.getOrderStatus().equals(OrderStatusEnum.WAS_CANCELED.getCode())){
                biWithdrawOrder.setCancel(biWithdrawOrder.getCancel() + 1);
            }
        }
        if(ObjectUtils.isNotEmpty(totalCount)){
            biWithdrawOrder.setOrderNum(totalCount.longValue());
        }
        biWithdrawOrder.setAppealNum(appealTotalNumFuture.get());
        biWithdrawOrder.setDateTime(dateStr);
        biWithdrawOrder.setAmountError(appealWrongAmountNumFuture.get());
        biWithdrawOrder.setDateTime(dateStr);
        for (AppealOrder item : appealList) {
            if(item.getAppealStatus().equals(Integer.parseInt(AppealStatusEnum.PAID.getCode()))){
                biWithdrawOrder.setAppealSuccess(item.getOrderAmount().longValue());
            }else if(item.getAppealStatus().equals(Integer.parseInt(AppealStatusEnum.UNPAID.getCode()))){
                biWithdrawOrder.setAppealFail(item.getOrderAmount().longValue());
            }
        }
        // 自动跑今天数据endTime不为空,手动跑历史数据endTime为空
        if (!StringUtils.isEmpty(endTime)) {
            biWithdrawOrder.setLastMinute(endTime);
        }

        // dateType 为空为自动执行
        if (StringUtils.isEmpty(dateType)) {
            biWithdrawOrderDailyMapper.updateByDateTime(biWithdrawOrder);
            // 更新月表,自动跑是累积方式，所以也需要累积月表数据
            LocalDateTime dateTime = DateUtil.parseLocalDateTime(dateStr, GlobalConstants.DATE_FORMAT_DAY);
            biWithdrawOrder.setDateTime(DateUtil.format(dateTime, GlobalConstants.DATE_FORMAT_MONTH));
            BiWithdrawOrderMonth biWithdrawOrderMonth = BeanUtil.toBean(biWithdrawOrder, BiWithdrawOrderMonth.class);
            biWithdrawOrderMonthMapper.updateByDateTime(biWithdrawOrderMonth);

        } else {
            // 只有手动跑当日数据才更新 lastMinute
            if (todayStr.equals(dateStr)) {
                biWithdrawOrder.setLastMinute(dateTmp);
            }
            biWithdrawOrderDailyMapper.updateByDateTime(biWithdrawOrder);
        }
        return page;
    }
}
