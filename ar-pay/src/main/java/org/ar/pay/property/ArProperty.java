package org.ar.pay.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ar")
public class ArProperty {


    public String getNotifyurl() {
        return notifyurl;
    }

    public void setNotifyurl(String notifyurl) {
        this.notifyurl = notifyurl;
    }

    private String notifyurl;

    public String getCallbackpaymenturl() {
        return callbackpaymenturl;
    }

    public void setCallbackpaymenturl(String callbackpaymenturl) {
        this.callbackpaymenturl = callbackpaymenturl;
    }

    private String callbackpaymenturl;

}
