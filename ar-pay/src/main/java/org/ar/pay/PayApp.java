package org.ar.pay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@SpringBootApplication
    @EnableDiscoveryClient
    @RefreshScope
    public class PayApp {
        public static void main(String[] args) {
            SpringApplication.run(PayApp.class, args);
        }


    @Bean
    public static PropertySourcesPlaceholderConfigurer palceholderConfigurer(){
        PropertySourcesPlaceholderConfigurer palceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        palceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);
        return palceholderConfigurer;
    }

}


