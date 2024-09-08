package org.ar.auth.comm.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ar")
@Data
@RefreshScope
public class ArProperty {

    //接收三方支付回调地址
    private String notifyurl;

    //接收三方代付回调地址
    private String callbackpaymenturl;

    //redis md5加密key
    private String redismd5key;

    //redis-key 短信验证码前缀
    private String smsCodePrefix;

    //redis-key 短信验证码前缀
    private String emailCodePrefix;

    //验证码有效时间 单位:分钟
    private Long validityDuration;

    //发送邮箱验证码的Email账号
    private String emailAccount;

    //钱包项目-前台图片文件大小最大限制 (5MB)
    private Integer maxImageFileSize;

    //钱包项目-前台视频文件大小最大限制 (50MB)
    private Integer maxVideoFileSize;

    //钱包项目 支付页面过期时间(分钟)
    private Long paymentPageExpirationTime;

    //支付页面地址
    private String payUrl;

    //RSA私钥
    private String privateKey;

    //RSA公钥
    private String publicKey;

    //签发支付页面token key
    private String secretKey;

    //钱包地址
    private String walletAccessUrl;

    //短信验证码模板id
    private String smsVerificationTemplateId;

    //确认超时短信模板id
    private String confirmationTimeoutTemplateId;

    //钱包项目 激活钱包页面过期时间(分钟)
    private Long walletActivationPageExpiryTime;

    //钱包项目 钱包激活页面地址
    private String walletActivationPageUrl;

    //当前环境
    private String appEnv;

    //交易ip统计频率时间范围
    private Integer expirationHours;

    //交易ip统计频率时间次数
    private Long tradeLimit;

    //公告链接
    private String announcementLink;

    //是否开启语音通知
    private String voiceNotificationStatus;

    //短信验证码运营商
    private String smsServiceProvider;
}
