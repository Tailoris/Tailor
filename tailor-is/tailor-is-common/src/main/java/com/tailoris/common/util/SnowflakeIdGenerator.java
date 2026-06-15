package com.tailoris.common.util;

import java.lang.management.ManagementFactory;
import java.security.SecureRandom;

/**
 * Snowflake ID 生成器 — 支持静态单例 + Spring Bean 两种模式
 *
 * <p>修复 C-013: 原静态单例的 workerId 基于 MAC 地址生成，
 * 在容器化环境中多个实例可能共享相同 MAC，导致 ID 冲突。
 * 现改为从环境变量读取或使用进程ID+随机数生成唯一 workerId。</p>
 *
 * <p>推荐方式: 使用 SpringSnowflakeIdGenerator 作为 Spring Bean，
 * workerId/datacenterId 通过配置文件指定。</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
public final class SnowflakeIdGenerator {

    private static final long START_STAMP = 1609459200000L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    // ── 静态单例（仅用于向后兼容）──────────────────────────────────────────

    private static volatile SnowflakeIdGenerator instance;

    /**
     * 获取单例实例，workerId 从环境变量或进程ID自动计算
     */
    public static SnowflakeIdGenerator getInstance() {
        if (instance == null) {
            synchronized (SnowflakeIdGenerator.class) {
                if (instance == null) {
                    long workerId = computeWorkerId();
                    long datacenterId = computeDatacenterId();
                    instance = new SnowflakeIdGenerator(workerId, datacenterId);
                }
            }
        }
        return instance;
    }

    /**
     * 使用指定参数创建新实例（供 SpringSnowflakeIdGenerator 使用）
     */
    public static SnowflakeIdGenerator getInstance(long workerId, long datacenterId) {
        return new SnowflakeIdGenerator(workerId, datacenterId);
    }

    /**
     * 从环境变量或进程ID计算唯一的 workerId
     * 优先级: SNOWFLAKE_WORKER_ID 环境变量 > 进程ID哈希 > 随机值
     */
    private static long computeWorkerId() {
        // 1. 显式配置的环境变量
        String envWorkerId = System.getenv("SNOWFLAKE_WORKER_ID");
        if (envWorkerId != null && !envWorkerId.isBlank()) {
            try {
                long id = Long.parseLong(envWorkerId.trim());
                if (id >= 0 && id <= MAX_WORKER_ID) {
                    return id;
                }
            } catch (NumberFormatException e) {
                // 环境变量格式非法，降级到进程ID方案
            }
        }

        // 2. JVM 进程ID 的哈希
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            String pid = jvmName.split("@")[0];
            return Math.abs(pid.hashCode()) % (MAX_WORKER_ID + 1);
        } catch (Exception e) {
            // 无法获取进程ID，降级到随机方案
        }

        // 3. 安全随机数（最后手段）
        return new SecureRandom().nextInt((int) (MAX_WORKER_ID + 1));
    }

    /**
     * 从环境变量计算 datacenterId
     * 优先级: SNOWFLAKE_DATACENTER_ID 环境变量 > 默认值 0
     */
    private static long computeDatacenterId() {
        String envDcId = System.getenv("SNOWFLAKE_DATACENTER_ID");
        if (envDcId != null && !envDcId.isBlank()) {
            try {
                long id = Long.parseLong(envDcId.trim());
                if (id >= 0 && id <= MAX_DATACENTER_ID) {
                    return id;
                }
            } catch (NumberFormatException e) {
                // 环境变量格式非法，使用默认值
            }
        }
        return 0;
    }

    // ── 实例方法 ──────────────────────────────────────────────────────────

    /**
     * @param workerId     工作机器ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range: 0 ~ " + MAX_WORKER_ID);
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range: 0 ~ " + MAX_DATACENTER_ID);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            long diff = lastTimestamp - timestamp;
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for " + diff + " milliseconds");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - START_STAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }
}
