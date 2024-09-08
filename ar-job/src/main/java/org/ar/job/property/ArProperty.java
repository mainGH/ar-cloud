package org.ar.job.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ar")
public class ArProperty {


    private String notifyurl;
    private String callbackpaymenturl;
    private String redismd5key;
    private String smsCodePrefix;

    public String getNotifyurl() {
        return notifyurl;
    }

    public void setNotifyurl(String notifyurl) {
        this.notifyurl = notifyurl;
    }

    public String getCallbackpaymenturl() {
        return callbackpaymenturl;
    }

    public void setCallbackpaymenturl(String callbackpaymenturl) {
        this.callbackpaymenturl = callbackpaymenturl;
    }

    public String getRedismd5key() {
        return redismd5key;
    }

    public void setRedismd5key(String redismd5key) {
        this.redismd5key = redismd5key;
    }

    public String getSmsCodePrefix() {
        return smsCodePrefix;
    }

    public void setSmsCodePrefix(String smsCodePrefix) {
        this.smsCodePrefix = smsCodePrefix;
    }

}
