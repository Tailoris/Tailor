package com.tailoris.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务启动类.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = {"com.tailoris.user", "com.tailoris.common"})
@EnableDiscoveryClient
@MapperScan("com.tailoris.user.mapper")
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
