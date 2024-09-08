package org.ar.wallet.sms;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Slf4j
@RefreshScope
public class SmsService {

    //不卡短信
    @Value("${sms.baseUrl}")
    private String baseUrl;

    @Value("${sms.appId}")
    private String appId;

    @Value("${sms.apiKey}")
    private String apiKey;

    @Value("${sms.apiSecret}")
    private String apiSecret;


    //颂量短信
    @Value("${smsSl.baseUrl}")
    private String slBaseUrl;

    @Value("${smsSl.appId}")
    private String slAppId;

    @Value("${smsSl.apiKey}")
    private String slApiKey;

    @Value("${smsSl.apiSecret}")
    private String slApiSecret;

    /**
     * 不卡发送短信验证码
     *
     * @param numbers 手机号 如有多个已英文逗号分割
     * @param code 验证码
     * @return {@link Boolean}
     */
    public Boolean sendBkSms(String numbers, String code) {

        String content = "[AR-Wallet] Your verification code is " + code + ". please do not share this code with anyone.";

        final String senderId = "";

        final String url = baseUrl.concat("/sendSms");

        HttpRequest request = HttpRequest.post(url);

        final String datetime = String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond());
        final String sign = SecureUtil.md5(apiKey.concat(apiSecret).concat(datetime));

        request.header(Header.CONNECTION, "Keep-Alive")
                .header(Header.CONTENT_TYPE, "application/json;charset=UTF-8")
                .header("Sign", sign)
                .header("Timestamp", datetime)
                .header("Api-Key", apiKey);


        final String params = JSONUtil.createObj()
                .set("appId", appId)
                .set("numbers", numbers)
                .set("content", content)
                .set("senderId", senderId)
                .toString();

        HttpResponse response = request.body(params).execute();
        if (response.isOk()) {

            JSONObject resJson = JSON.parseObject(response.body());

            if (resJson.get("status").toString().equals("0")){
                log.info("不卡发送短信验证码成功: 手机号: {}, 验证码: {}, 请求地址: {}, 请求参数: {}, response: {}", numbers, code, url, params, resJson);
                return Boolean.TRUE;
            }
        }

        log.error("不卡发送短信验证码失败: 手机号: {}, 验证码: {}, 请求地址: {}, 请求参数: {}, response: {}", numbers, code, url, params, response.body());
        return Boolean.FALSE;
    }


    /**
     * 颂量发送短信验证码
     *
     * @param numbers 手机号 如有多个已英文逗号分割
     * @param code    验证码
     * @return {@link Boolean}
     */
    public Boolean sendSlSms(String numbers, String code) {

        String content = "[AR-Wallet] Your verification code is " + code + ". please do not share this code with anyone.";

        final String senderId = "";

        final String url = slBaseUrl.concat("/sms/sendSms");

        HttpRequest request = HttpRequest.post(url);

        final String datetime = String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond());
        final String sign = SecureUtil.md5(slApiKey.concat(slApiSecret).concat(datetime));

        request.header(Header.CONNECTION, "Keep-Alive")
                .header(Header.CONTENT_TYPE, "application/json;charset=UTF-8")
                .header("Sign", sign)
                .header("Timestamp", datetime)
                .header("Api-Key", slApiKey);


        final String params = JSONUtil.createObj()
                .set("appId", slAppId)
                .set("numbers", numbers)
                .set("content", content)
                .set("senderId", senderId)
                .toString();

        HttpResponse response = request.body(params).execute();
        if (response.isOk()) {

            JSONObject resJson = JSON.parseObject(response.body());

            if (resJson.get("status").toString().equals("0")){
                log.info("颂量发送短信验证码成功: 手机号: {}, 验证码: {}, 请求地址: {}, 请求参数: {}, response: {}", numbers, code, url, params, resJson);
                return Boolean.TRUE;
            }
        }

        log.error("颂量发送短信验证码失败: 手机号: {}, 验证码: {}, 请求地址: {}, 请求参数: {}, response: {}", numbers, code, url, params, response.body());
        return Boolean.FALSE;
    }

}
