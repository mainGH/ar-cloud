package org.ar.job.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.job.rabbitmq.DailyRewardClaimTaskMessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.time.LocalDateTime;


@Component("dailyRewardClaimTaskProcessor")
@Slf4j
@RequiredArgsConstructor
public class DailyRewardClaimTaskProcessor implements BasicProcessor {

    @Autowired
    private DailyRewardClaimTaskMessageSender dailyRewardClaimTaskMessageSender;

    /**
     * 定时任务 凌晨01:00自动领取前一日任务奖励
     *
     * @author Simon
     * @date 2023/12/01
     */
    @Override
    public ProcessResult process(TaskContext context) {

        log.info("定时任务执行: 凌晨01:00自动领取前一日任务奖励, 当前时间: {}", LocalDateTime.now());

        //发送MQ 凌晨01:00自动领取前一日任务奖励
        dailyRewardClaimTaskMessageSender.sendDailyRewardClaimTaskMessage("autoClaimReward");

        // 在线日志功能，可以直接在控制台查看任务日志，非常便捷
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.info("BasicProcessorDemo start to process, current JobParams is {}.", context.getJobParams());

        return new ProcessResult(true, "return success");
    }

}
