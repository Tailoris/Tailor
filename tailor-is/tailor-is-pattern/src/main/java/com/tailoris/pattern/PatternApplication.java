package com.tailoris.pattern;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.tailoris.pattern", "com.tailoris.common"})
@EnableDiscoveryClient
@MapperScan("com.tailoris.pattern.mapper")
public class PatternApplication {
    public static void main(String[] args) {
        SpringApplication.run(PatternApplication.class, args);
    }
}