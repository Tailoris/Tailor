package com.tailoris.common.exception;

import lombok.Getter;

/**
 * 数据源连接异常 - 数据库/Redis/中间件连接失败
 *
 * <p>用于区分数据库连接超时、拒绝连接等中间件故障场景，
 * 与业务异常、登录异常分离，返回 HTTP 503 Service Unavailable。</p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>MySQL 连接超时/拒绝连接</li>
 *   <li>Redis 连接失败</li>
 *   <li>Nacos 注册中心不可达</li>
 *   <li>RabbitMQ 消息队列连接失败</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.2.0
 */
@Getter
public class DataSourceConnectException extends RuntimeException {

    /**
     * 数据源类型标识（如 MySQL, Redis, Nacos, RabbitMQ 等）
     */
    private final String sourceType;

    public DataSourceConnectException(String sourceType, String message) {
        super(message);
        this.sourceType = sourceType;
    }

    public DataSourceConnectException(String sourceType, String message, Throwable cause) {
        super(message, cause);
        this.sourceType = sourceType;
    }

    public DataSourceConnectException(String message) {
        super(message);
        this.sourceType = "UNKNOWN";
    }

    public DataSourceConnectException(String message, Throwable cause) {
        super(message, cause);
        this.sourceType = "UNKNOWN";
    }
}