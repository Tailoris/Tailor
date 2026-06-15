package com.tailoris.common.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.config.DataSyncStrategyConfig;
import com.tailoris.common.config.DataSyncStrategyConfig.SyncLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 实时数据同步组件 —— 面向核心数据（订单、商品、资金、用户）的秒级同步。
 *
 * <h3>架构概述</h3>
 * <pre>
 *  写操作 (事务内)
 *      │
 *      ├─ TransactionSynchronization.afterCommit()
 *      │     └─ publishSyncEvent() → RabbitMQ → 目标服务消费者
 *      │
 *      └─ Seata AT / XA 分布式事务（跨库强一致性场景）
 * </pre>
 *
 * <h3>适用场景</h3>
 * <ul>
 *   <li>订单状态变更 → 同步到 admin / analytics / message 服务</li>
 *   <li>支付结果通知 → 同步到 order / settlement 服务</li>
 *   <li>库存扣减/释放 → 同步到 order / product 服务</li>
 *   <li>用户信息变更 → 同步到各下游服务</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * @Service
 * public class OrderService {
 *     private final RealTimeDataSync realTimeDataSync;
 *
 *     @Transactional
 *     public void payOrder(String orderNo) {
 *         // ... 业务逻辑
 *         orderMapper.updateById(order);
 *
 *         // 事务提交后发送实时同步消息
 *         realTimeDataSync.publishAfterCommit("order", "status_change",
 *             Map.of("orderNo", orderNo, "status", "PAID"));
 *     }
 * }
 * }</pre>
 *
 * @author Tailor IS Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealTimeDataSync {

    @Nullable
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /** 核心数据同步 Exchange */
    public static final String CORE_DATA_SYNC_EXCHANGE = "tailoris.core-data.sync";

    /**
     * 同步事件包装。
     */
    public record SyncEvent(
            String eventId,
            String dataType,
            String action,
            Map<String, Object> payload,
            long timestamp,
            String sourceService
    ) {
        public static SyncEvent of(String dataType, String action, Map<String, Object> payload) {
            return new SyncEvent(
                    UUID.randomUUID().toString(),
                    dataType,
                    action,
                    payload != null ? payload : Map.of(),
                    Instant.now().toEpochMilli(),
                    resolveSourceService()
            );
        }
    }

    /**
     * 发布实时同步事件 —— 在事务提交后通过 RabbitMQ 广播。
     *
     * <p>注册为 TransactionSynchronization，保证仅在事务成功提交后才发送 MQ 消息，
     * 避免事务回滚后消息已经发出的不一致问题。</p>
     *
     * @param dataType 数据类型（如 "order", "product"）
     * @param action   操作类型（如 "create", "update", "delete", "status_change"）
     * @param payload  业务载荷（会被序列化为 JSON）
     */
    public void publishAfterCommit(String dataType, String action, @Nullable Map<String, Object> payload) {
        SyncEvent event = SyncEvent.of(dataType, action, payload);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 不在事务上下文中，直接发送
            publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishEvent(event);
                    }
                });
    }

    /**
     * 立即发布实时同步事件（不等待事务提交）。
     *
     * @param dataType 数据类型
     * @param action   操作类型
     * @param payload  业务载荷
     */
    public void publishImmediately(String dataType, String action, @Nullable Map<String, Object> payload) {
        SyncEvent event = SyncEvent.of(dataType, action, payload);
        publishEvent(event);
    }

    /**
     * 发送同步事件到 RabbitMQ。
     *
     * <p>routingKey 格式：{dataType}.{action}，消费者按此路由。</p>
     */
    private void publishEvent(SyncEvent event) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate 不可用，跳过实时同步事件: dataType={}, action={}",
                    event.dataType(), event.action());
            return;
        }

        // 仅对核心数据走实时通道
        SyncLevel level = DataSyncStrategyConfig.getSyncLevel(event.dataType());
        if (level != SyncLevel.REAL_TIME) {
            log.debug("非核心数据，跳过实时同步: dataType={}", event.dataType());
            return;
        }

        String routingKey = event.dataType() + "." + event.action();

        try {
            String body = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(CORE_DATA_SYNC_EXCHANGE, routingKey, body);
            log.info("实时同步事件已发送: eventId={}, dataType={}, action={}, routingKey={}",
                    event.eventId(), event.dataType(), event.action(), routingKey);
        } catch (JsonProcessingException e) {
            log.error("实时同步事件序列化失败: eventId={}", event.eventId(), e);
        } catch (Exception e) {
            log.error("实时同步事件发送失败: eventId={}, 需人工关注", event.eventId(), e);
        }
    }

    /**
     * 获取当前 Exchange 名称（供消费者配置引用）。
     *
     * @return exchange 名称
     */
    public String getExchange() {
        return CORE_DATA_SYNC_EXCHANGE;
    }

    private static String resolveSourceService() {
        String appName = System.getenv("spring.application.name");
        if (appName == null) {
            appName = System.getProperty("spring.application.name", "unknown");
        }
        return appName;
    }

    /**
     * 构建事件 payload 的便捷方法。
     *
     * @param entries 键值对（必须为偶数个）
     * @return Map 载荷
     */
    public static Map<String, Object> payload(Object... entries) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < entries.length - 1; i += 2) {
            String key = entries[i].toString();
            Object value = entries[i + 1];
            map.put(key, value);
        }
        return map;
    }
}
