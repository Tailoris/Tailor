package com.tailoris.common.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.config.DataSyncStrategyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RealTimeDataSync 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RealTimeDataSync 单元测试")
class RealTimeDataSyncTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;
    private RealTimeDataSync realTimeDataSync;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        realTimeDataSync = new RealTimeDataSync(rabbitTemplate, objectMapper);
    }

    @Test
    @DisplayName("SyncEvent-of应创建正确的事件")
    void syncEvent_of_shouldCreateCorrectEvent() {
        Map<String, Object> payload = Map.of("key", "value");

        RealTimeDataSync.SyncEvent event = RealTimeDataSync.SyncEvent.of("order", "create", payload);

        assertThat(event.dataType()).isEqualTo("order");
        assertThat(event.action()).isEqualTo("create");
        assertThat(event.payload()).isEqualTo(payload);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.timestamp()).isPositive();
    }

    @Test
    @DisplayName("SyncEvent-of-null payload应使用空Map")
    void syncEvent_of_nullPayload_shouldUseEmptyMap() {
        RealTimeDataSync.SyncEvent event = RealTimeDataSync.SyncEvent.of("order", "create", null);

        assertThat(event.payload()).isEmpty();
    }

    @Test
    @DisplayName("publishImmediately-核心数据应发送到RabbitMQ")
    void publishImmediately_coreData_shouldSendToRabbit() {
        Map<String, Object> payload = Map.of("orderId", "123");

        realTimeDataSync.publishImmediately("order", "create", payload);

        verify(rabbitTemplate).convertAndSend(
                eq(RealTimeDataSync.CORE_DATA_SYNC_EXCHANGE),
                eq("order.create"),
                anyString());
    }

    @Test
    @DisplayName("publishImmediately-非核心数据不应发送到RabbitMQ")
    void publishImmediately_nonCoreData_shouldNotSend() {
        Map<String, Object> payload = Map.of("postId", "123");

        realTimeDataSync.publishImmediately("community_post", "create", payload);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("publishImmediately-rabbitTemplate为null应跳过")
    void publishImmediately_nullRabbitTemplate_shouldSkip() {
        RealTimeDataSync syncWithoutRabbit = new RealTimeDataSync(null, objectMapper);

        syncWithoutRabbit.publishImmediately("order", "create", Map.of());
    }

    @Test
    @DisplayName("publishAfterCommit-不在事务中应直接发送")
    void publishAfterCommit_notInTransaction_shouldSendDirectly() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }

        realTimeDataSync.publishAfterCommit("order", "create", Map.of("id", "123"));

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getExchange应返回正确的Exchange名称")
    void getExchange_shouldReturnCorrectName() {
        String exchange = realTimeDataSync.getExchange();

        assertThat(exchange).isEqualTo("tailoris.core-data.sync");
    }

    @Test
    @DisplayName("payload-应正确构建Map")
    void payload_shouldBuildMapCorrectly() {
        Map<String, Object> result = RealTimeDataSync.payload("key1", "value1", "key2", 123);

        assertThat(result).hasSize(2);
        assertThat(result.get("key1")).isEqualTo("value1");
        assertThat(result.get("key2")).isEqualTo(123);
    }

    @Test
    @DisplayName("payload-奇数个参数应忽略最后一个")
    void payload_oddArguments_shouldIgnoreLast() {
        Map<String, Object> result = RealTimeDataSync.payload("key1", "value1", "orphan");

        assertThat(result).hasSize(1);
        assertThat(result.get("key1")).isEqualTo("value1");
    }

    @Test
    @DisplayName("payload-空参数应返回空Map")
    void payload_noArguments_shouldReturnEmptyMap() {
        Map<String, Object> result = RealTimeDataSync.payload();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("序列化失败应记录错误")
    void publishEvent_serializationFails_shouldLogError() throws Exception {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) {
                throw new RuntimeException("Serialization error");
            }
        };
        RealTimeDataSync syncWithFailingMapper = new RealTimeDataSync(rabbitTemplate, failingMapper);

        syncWithFailingMapper.publishImmediately("order", "create", Map.of());

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("RabbitMQ发送失败应记录错误")
    void publishEvent_sendFails_shouldLogError() {
        lenient().doThrow(new RuntimeException("RabbitMQ error"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());

        realTimeDataSync.publishImmediately("order", "create", Map.of());
    }
}
