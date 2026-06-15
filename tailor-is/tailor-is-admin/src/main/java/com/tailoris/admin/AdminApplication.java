package com.tailoris.admin;

import com.tailoris.admin.config.MapperBeanNameGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.tailoris.admin",
        "com.tailoris.common",
        "com.tailoris.api"
})
@EnableDiscoveryClient
@MapperScan(basePackages = {"com.tailoris.admin.mapper", "com.tailoris.api.admin.mapper",
                            "com.tailoris.api.community.mapper", "com.tailoris.api.merchant.mapper",
                            "com.tailoris.api.order.mapper", "com.tailoris.api.product.mapper",
                            "com.tailoris.api.user.mapper"},
        nameGenerator = MapperBeanNameGenerator.class)
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
