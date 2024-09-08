package org.ar.wallet.webSocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ar.wallet.entity.NotifyOrderStatusChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单交易状态改变通知 推送消息给前端
 *
 * @author Simon
 * @date 2023/11/08
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotifyOrderStatusChangeSend {

    @Autowired
    private NotifyOrderStatusChangeWebSocketService notifyOrderStatusChangeWebSocketService;

    /**
     * 订单交易状态改变通知 推送消息给前端
     */
    public void send(NotifyOrderStatusChangeMessage notifyOrderStatusChangeMessage) {

        try {
            boolean send = notifyOrderStatusChangeWebSocketService.AppointSending(JSON.toJSONString(notifyOrderStatusChangeMessage, SerializerFeature.WriteMapNullValue));
            if (send) {
//                log.info("[订单交易状态改变通知]webSocket推送前端成功: 用户id: {}, data: {}", notifyOrderStatusChangeMessage.getMemberId(), JSON.toJSONString(notifyOrderStatusChangeMessage, SerializerFeature.WriteMapNullValue));
            } else {
                log.error("[订单交易状态改变通知]webSocket推送前端失败: 用户id: {}, data: {}", notifyOrderStatusChangeMessage.getMemberId(), JSON.toJSONString(notifyOrderStatusChangeMessage, SerializerFeature.WriteMapNullValue));
            }
        } catch (Exception e) {
            log.error("[订单交易状态改变通知]webSocket推送前端失败, e: {}", e);
        }

    }
}
