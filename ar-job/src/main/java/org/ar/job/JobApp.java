package org.ar.job;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

/**
 * @author Admin
 */
@SpringBootApplication
@EnableDiscoveryClient
@Import({})
@RefreshScope
@EnableAsync
@MapperScan("**.mapper")
@EnableAspectJAutoProxy
@EnableFeignClients()
public class JobApp {

    public static void main(String[] args) {

        // 设置默认时区为北京时区
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        SpringApplication.run(JobApp.class, args);
    }


}


