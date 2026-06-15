package com.tailoris.copyright;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.tailoris.copyright", "com.tailoris.common"})
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.tailoris.copyright.mapper")
public class CopyrightApplication {
    public static void main(String[] args) {
        SpringApplication.run(CopyrightApplication.class, args);
    }
}
