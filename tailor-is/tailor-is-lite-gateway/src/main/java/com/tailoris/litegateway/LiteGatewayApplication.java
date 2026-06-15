package com.tailoris.litegateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LiteGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiteGatewayApplication.class, args);
    }
}
