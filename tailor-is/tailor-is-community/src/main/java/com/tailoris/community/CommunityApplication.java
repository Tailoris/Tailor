package com.tailoris.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @deprecated 架构方案要求非核心模块采用 SpringBoot 单体 + 插件化架构，后续版本将合并至 tailor-is-lite。
 * 参考: docs/architecture/Tailor IS（裁智云）服装全产业平台架构优化方案.md
 *
 * TODO P3-L-07: 清理计划 — v1.1.0 中合并至 tailor-is-lite 后移除此类
 */
@Deprecated
@SpringBootApplication(scanBasePackages = {"com.tailoris.community", "com.tailoris.common"})
@EnableDiscoveryClient
@MapperScan("com.tailoris.community.mapper")
public class CommunityApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityApplication.class, args);
    }
}
