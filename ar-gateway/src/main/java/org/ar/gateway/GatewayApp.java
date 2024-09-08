package org.ar.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.TimeZone;


@EnableDiscoveryClient
    @SpringBootApplication
    public class GatewayApp {
    public static void main(String[] args) {
        System.setProperty("csp.sentinel.app.type", "1");
        //System.setProperty("csp.sentinel.app.type", "1");
        //System.setProperty("csp.sentinel.dashboard.server", "localhost:8080");


        // 设置默认时区为北京时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(GatewayApp.class, args);
    }
}

