package org.ar.wallet.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.nimbusds.jose.JWSObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.ar.common.core.constant.SecurityConstants;
import org.ar.common.core.excption.WebSocketAuthException;
import org.ar.wallet.util.SpringContextUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class CustomConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        String userId = null;

        String clientIp = null;

        if (request != null) {

            // 尝试从HTTP请求头中获取IP地址
            List<String> forwardFor = request.getHeaders().get("X-Forwarded-For");
            if (forwardFor != null && !forwardFor.isEmpty()) {
                // X-Forwarded-For: <client>, <proxy1>, <proxy2>
                clientIp = forwardFor.get(0).split(",")[0].trim(); // 获取第一个IP地址
            }

            List<String> strings = request.getParameterMap().get("userId");

            log.info("=====>CustomConfigurator modifyHandshake()...userIds="+strings);
            if (strings != null && !strings.isEmpty()) {
                userId = strings.get(0);
            }
            log.info("webSocket CustomConfigurator modifyHandshake : 会员id: {}, 客户端ip: {}", userId, clientIp);

            List<String> headerParam = request.getHeaders().get(request.SEC_WEBSOCKET_PROTOCOL);

            // 获取子协议中的token
            List<String> secWebsocketProtocol = request.getHeaders().get("sec-websocket-protocol");

            String token = null;
            if (secWebsocketProtocol != null && !secWebsocketProtocol.isEmpty()) {
                token = secWebsocketProtocol.get(0);
            }

            log.info("webSocket CustomConfigurator modifyHandshake : token: {}", token);
            //鉴权处理
            if (validateToken(token, userId)) {
                //鉴权成功
                log.info("webSocket CustomConfigurator modifyHandshake 鉴权成功: 会员id: {}, 客户端ip: {}", userId, clientIp);

                //响应前端必须带上这个token
                response.getHeaders().put("sec-websocket-protocol", Collections.singletonList(token));

                return;
            }
        }

        log.error("webSocket CustomConfigurator modifyHandshake 鉴权失败: 会员id: {}, 客户端ip: {}", userId, clientIp);
        //鉴权失败
        throw new WebSocketAuthException("webSocket鉴权失败");
    }

    /**
     * webSocket token鉴权
     *
     * @param token
     * @param userId
     * @return {@link Boolean}
     */
    public Boolean validateToken(String token, String userId) {
        log.info("=====>CustomConfigurator validateToken()...userId="+userId+",  token="+token);

        if (StringUtils.isEmpty(token)) return Boolean.FALSE;

        try {
            // 解析JWT获取jti，以jti为key判断redis的黑名单列表是否存在，存在则拦截访问
            token = StrUtil.replaceIgnoreCase(token, SecurityConstants.JWT_PREFIX, Strings.EMPTY);
            String payload = StrUtil.toString(JWSObject.parse(token).getPayload());
            JSONObject jsonObject = JSONObject.parseObject(payload);

            //校验前端传的UserID和Token中的userId是否一致
            if (jsonObject.get("userId") != null && jsonObject.get("userId").toString().equals(userId)) {

                String jti = jsonObject.getString("jti"); // JWT唯一标识
                String username = jsonObject.getString("username"); // JWT唯一标识
                long exp = jsonObject.getLong("exp"); // JWT过期时间戳(单位:秒)

                log.info("webSocket CustomConfigurator validateToken : jti:"+jti+",username="+username+",exp="+exp);

                RedisTemplate redisTemplate = SpringContextUtil.getBean("redisTemplate");

                Boolean isBlack = redisTemplate.hasKey(SecurityConstants.BLACKLIST_TOKEN_PREFIX + jti);

                if (isBlack) return Boolean.FALSE;

                String gjti = (String) redisTemplate.opsForValue().get(SecurityConstants.LOGIN_USER_ID + username);

                log.info("webSocket CustomConfigurator validateToken : gjti:"+gjti+",jti="+jti);
                if (!jti.equals(gjti)) return Boolean.FALSE;

                return Boolean.TRUE;
            }
        } catch (Exception e) {
        }
        return Boolean.FALSE;
    }
}
