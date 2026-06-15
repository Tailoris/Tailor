package com.tailoris.supply;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.tailoris.supply", "com.tailoris.common"})
@EnableDiscoveryClient
@MapperScan("com.tailoris.supply.mapper")
public class SupplyApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupplyApplication.class, args);
    }
}
