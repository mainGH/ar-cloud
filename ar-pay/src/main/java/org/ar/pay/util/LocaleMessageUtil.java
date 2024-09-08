package org.ar.pay.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Locale;

@Component
public class LocaleMessageUtil {

    @Resource
    private MessageSource messageSource;

    public LocaleMessageUtil() {
    }

    public String getMessage(String code) {
        return this.getConnectorMessage(code, null, null);
    }

    public String getMessage(String code, String param) {
        return this.getConnectorMessage(code, param, null);
    }

    public String getMessage(String[] codes) {
        return this.getConnectorMessage(null, null, codes);
    }

    private String getConnectorMessage(String code, String param, String[] codes) {
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotEmpty(param)) {
            stringBuilder.append(param).append(":");
        } else if (StringUtils.isNotEmpty(code)) {
            Locale locale = LocaleContextHolder.getLocale();
            stringBuilder.append(this.messageSource.getMessage(code, null, locale));
        }
        if (null != codes) {
            for (String arr : codes) {
                stringBuilder.append(this.messageSource.getMessage(arr, null, LocaleContextHolder.getLocale())).append(";");
            }
        }
        return stringBuilder.toString();
    }

}
