package org.ar.pay.config;


import org.ar.pay.interceptor.MessageLocaleResolver;
import org.ar.pay.interceptor.WhiteListInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//@Configuration
public class PayWebConfig implements WebMvcConfigurer {
//    public void addInterceptors(InterceptorRegistry registry){
//        registry.addInterceptor(new WhiteListInterceptor()).addPathPatterns("/**");
//    }


}
