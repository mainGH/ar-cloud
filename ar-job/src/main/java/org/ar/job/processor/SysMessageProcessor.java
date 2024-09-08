package org.ar.job.processor;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.common.core.result.RestResult;
import org.ar.common.pay.dto.MerchantLastOrderWarnDTO;
import org.ar.common.pay.dto.SmsBalanceWarnDTO;
import org.ar.common.redis.constants.RedisKeys;
import org.ar.common.redis.util.RedisUtils;
import org.ar.job.feign.MerchantInfoClient;
import org.ar.job.feign.SmsThirdApiFeignClient;
import org.ar.job.feign.SysMessageFeignClient;
import org.ar.manager.req.SysMessageSendReq;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author admin
 * @date 2024/5/6 17:22
 */
@Component("SysMessageProcessor")
@Slf4j
@RequiredArgsConstructor
public class SysMessageProcessor implements BasicProcessor {

    @Resource
    private SmsThirdApiFeignClient smsThirdApiFeignClient;

    @Resource
    private MerchantInfoClient merchantInfoClient;

    @Resource
    private SysMessageFeignClient sysMessageFeignClient;

    private final RedisUtils redisUtils;

    String sysMessage = RedisKeys.SYS_MESSAGE;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        smsSysMessage();
        unTransSysMessage();
        return new ProcessResult(true, "return success");
    }

    private void smsSysMessage() {
        String smsKey = sysMessage + ":sms";
        // 获取当前余额
        try {
            RestResult<SmsBalanceWarnDTO> smsBalanceWarnDTORestResult = smsThirdApiFeignClient.checkBalance();
            SmsBalanceWarnDTO data = smsBalanceWarnDTORestResult.getData();
            BigDecimal currentBalance = data.getCurrentBalance();
            BigDecimal thresholdBalance = data.getThreshold();
            String content = getContent(String.valueOf(currentBalance), String.valueOf(thresholdBalance));
            processSend(data.getIsWarn(), smsKey, 1, content);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void unTransSysMessage() {
        String unTransKey = sysMessage + ":unTrans:";
        // 获取未交易信息
        try {
            List<MerchantLastOrderWarnDTO> latestOrderTime = merchantInfoClient.getLatestOrderTime();
            for (MerchantLastOrderWarnDTO merchantLastOrderWarnDTO : latestOrderTime) {
                String merchantName = merchantLastOrderWarnDTO.getMerchantName();
                Integer thresholdTimeLimit = merchantLastOrderWarnDTO.getThreshold();
                String content = getContent(merchantName, String.valueOf(thresholdTimeLimit));
                processSend(merchantLastOrderWarnDTO.isWarn(), unTransKey + merchantName, 2, content);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private String getContent(String merchantName, String threshold) {
        Map<String, String> messageContent = new HashMap<>();
        messageContent.put("value", merchantName);
        messageContent.put("threshold", threshold);
        return JSON.toJSONString(messageContent);
    }

    private void processSend(boolean isWarn, String redisKey, Integer messageType, String content) {
        boolean lock = redisUtils.hasKey(redisKey);
        if (isWarn) {
            // 告警 不存在锁 发送消息 加锁
            if (!lock) {
                // 发送消息
                SysMessageSendReq req = new SysMessageSendReq();
                req.setMessageTo("*");
                req.setMessageFrom("admin");
                req.setMessageType(messageType);
                req.setMessageContent(content);
                sysMessageFeignClient.sendMessage(req);
                redisUtils.set(redisKey, "1");
            }
        } else {
            // 未告警 存在锁 则解锁
            if (lock) {
                redisUtils.del(redisKey);
            }
        }
    }
}
