package org.ar.common.web.utils;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.ar.common.core.constant.SecurityConstants;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;

public class UserContext {


    public static Long getCurrentUserId() {
        String token = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader(SecurityConstants.JWT_PAYLOAD_KEY);
        if (StringUtil.isEmpty(token)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(URLDecoder.decode(token, "utf-8"));
            if (Objects.isNull(jsonObject)) {
                return null;
            }
            return jsonObject.getLong("userId");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取会员id
     *
     * @return {@link String}
     */
    public static String getCurrentMemberId() {
        String token = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader(SecurityConstants.JWT_PAYLOAD_KEY);
        if (StringUtil.isEmpty(token)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(URLDecoder.decode(token, "utf-8"));
            if (Objects.isNull(jsonObject)) {
                return null;
            }
            return jsonObject.getString("memberId");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getCurrentUserName() {

        String jwtPayloadKey = SecurityConstants.JWT_PAYLOAD_KEY;

        if (StringUtil.isEmpty(jwtPayloadKey)){
            return null;
        }

        //更新数据库的操作可能是 MQ异步操作的 没有请求上下文信息 防止空指针, 加入了以下判断
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            String token = servletRequestAttributes.getRequest().getHeader(jwtPayloadKey);
            // 处理 token
            if (StringUtil.isEmpty(token)) {
                return null;
            }
            try {
                JSONObject jsonObject = JSON.parseObject(URLDecoder.decode(token, "utf-8"));
                if (Objects.isNull(jsonObject)) {
                    return null;
                }
                return jsonObject.getString("username");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            // 请求上下文不可用的处理
            return null;
        }
    }

    /**
     * 获取客戶端id
     *
     * @return {@link String}
     */
    public static String getCurrentClientId() {
        String token = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader(SecurityConstants.JWT_PAYLOAD_KEY);
        if (StringUtil.isEmpty(token)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(URLDecoder.decode(token, "utf-8"));
            if (Objects.isNull(jsonObject)) {
                return null;
            }
            return jsonObject.getString("client_id");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
