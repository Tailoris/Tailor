package com.tailoris;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tailor IS 用户服务启动类
 *  - 启用定时调度（@EnableScheduling）用于定时刷新业务指标 Gauge
 *  - 通过 Actuator + Micrometer Prometheus 在 /actuator/prometheus 暴露指标
 */
@SpringBootApplication
@EnableScheduling
public class TailorIsUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(TailorIsUserApplication.class, args);
    }
}
