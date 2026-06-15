package com.tailoris.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.tailoris.order", "com.tailoris.common"})
@EnableDiscoveryClient
@EnableFeignClients(clients = {
    com.tailoris.common.client.UserClient.class,
    com.tailoris.common.client.ProductClient.class,
    com.tailoris.common.client.PaymentClient.class,
    com.tailoris.common.client.MerchantClient.class,
    com.tailoris.common.client.SettlementClient.class
})
@EnableScheduling
@MapperScan("com.tailoris.order.mapper")
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
