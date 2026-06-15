package com.tailoris.common.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Snowflake ID生成器 - Spring Bean版本 - 修复 B-H24
 *
 * <p>将原单例改造为Spring Bean，支持多实例和配置化管理：</p>
 * <ul>
 *   <li>workerId 和 datacenterId 从配置文件读取</li>
 *   <li>支持Nacos配置中心动态调整</li>
 *   <li>支持不同微服务使用不同的实例</li>
 *   <li>集群中每个节点ID必须唯一</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>{@code
 * tailoris:
 *   snowflake:
 *     worker-id: 1
 *     datacenter-id: 1
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RefreshScope
public class SpringSnowflakeIdGenerator {

    @Value("${tailoris.snowflake.worker-id:1}")
    private int workerId;

    @Value("${tailoris.snowflake.datacenter-id:1}")
    private int datacenterId;

    @Getter
    private SnowflakeIdGenerator generator;

    @PostConstruct
    public void init() {
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException("workerId 必须在 0~31 之间: " + workerId);
        }
        if (datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException("datacenterId 必须在 0~31 之间: " + datacenterId);
        }
        this.generator = new SnowflakeIdGenerator(workerId, datacenterId);
        log.info("SpringSnowflakeIdGenerator 初始化完成, workerId={}, datacenterId={}",
                workerId, datacenterId);
    }

    /**
     * 生成下一个ID
     */
    public long nextId() {
        return generator.nextId();
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }
}
