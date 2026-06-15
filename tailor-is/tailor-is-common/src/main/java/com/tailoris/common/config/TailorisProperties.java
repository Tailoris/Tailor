package com.tailoris.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tailor IS 统一配置属性 - 替代分散的 @Value 注解。
 *
 * <p>将原本散落在 15+ 个类的 @Value 配置收敛到一个 @ConfigurationProperties 类。
 * 支持 Nacos 配置中心动态刷新。</p>
 *
 * <h3>配置结构</h3>
 * <pre>
 * tailoris:
 *   crypto:
 *     aes-key: ...
 *     aes-key-version: 1
 *   snowflake:
 *     worker-id: 1
 *     datacenter-id: 1
 *   cache:
 *     l1:
 *       maximum-size: 10000
 *       expire-minutes: 5
 *     l2:
 *       default-ttl-seconds: 300
 *   copyright:
 *     aes-key: ...
 *     verify-base-url: https://api.example.com
 *     similarity-threshold: 0.85
 *     similarity-engine: default
 *   gateway:
 *     auth:
 *       exclude-paths: /api/auth/**,/api/health/**
 *     rate-limit:
 *       enabled: true
 *       default-permits-per-second: 100
 *       login-permits-per-minute: 10
 *   product:
 *     stock-allow-oversell: false
 *     pattern-max-download: 100
 *     token-ttl-days: 30
 *     view-count-sync-batch-size: 500
 *   user:
 *     password-encryption-algorithm: AES-256-GCM
 * </pre>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "tailoris")
public class TailorisProperties {

    private final Crypto crypto = new Crypto();
    private final Snowflake snowflake = new Snowflake();
    private final Cache cache = new Cache();
    private final Copyright copyright = new Copyright();
    private final Gateway gateway = new Gateway();
    private final Product product = new Product();
    private final User user = new User();

    @Data
    public static class Crypto {
        /** AES-256 加密密钥 */
        private String aesKey = "";
        /** 密钥版本（用于轮换） */
        private int aesKeyVersion = 1;
    }

    @Data
    public static class Snowflake {
        /** 雪花算法工作节点ID (0-31) */
        private int workerId = 1;
        /** 雪花算法数据中心ID (0-31) */
        private int datacenterId = 1;
    }

    @Data
    public static class Cache {
        private final L1 l1 = new L1();
        private final L2 l2 = new L2();

        @Data
        public static class L1 {
            /** L1缓存最大条目数 */
            private long maximumSize = 10000;
            /** L1缓存过期时间（分钟） */
            private long expireMinutes = 5;
        }

        @Data
        public static class L2 {
            /** L2缓存默认TTL（秒） */
            private long defaultTtlSeconds = 300;
        }
    }

    @Data
    public static class Copyright {
        /** 版权数据AES加密密钥 */
        private String aesKey = "";
        /** 版权验证基础URL */
        private String verifyBaseUrl = "https://api.example.com";
        /** 相似度检测阈值 */
        private double similarityThreshold = 0.85;
        /** 相似度检测引擎类型 */
        private String similarityEngine = "default";
    }

    @Data
    public static class Gateway {
        private final Auth auth = new Auth();
        private final RateLimit rateLimit = new RateLimit();

        @Data
        public static class Auth {
            /** 鉴权排除路径（逗号分隔） */
            private String excludePaths = "/api/auth/**,/api/health/**";
        }

        @Data
        public static class RateLimit {
            /** 是否启用限流 */
            private boolean enabled = true;
            /** 默认每秒允许请求数 */
            private int defaultPermitsPerSecond = 100;
            /** 登录接口每分钟允许请求数 */
            private int loginPermitsPerMinute = 10;
        }
    }

    @Data
    public static class Product {
        /** 是否允许超卖 */
        private boolean stockAllowOversell = false;
        /** 图案最大下载次数 */
        private int patternMaxDownload = 100;
        /** Token有效期（天） */
        private int tokenTtlDays = 30;
        /** 浏览量同步批次大小 */
        private int viewCountSyncBatchSize = 500;
    }

    @Data
    public static class User {
        /** 用户密码加密算法 */
        private String passwordEncryptionAlgorithm = "AES-256-GCM";
    }
}
