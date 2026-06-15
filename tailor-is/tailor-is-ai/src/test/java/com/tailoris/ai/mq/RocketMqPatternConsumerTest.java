package com.tailoris.ai.mq;

import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.service.PatternService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RocketMqPatternConsumer 单元测试")
@ExtendWith(MockitoExtension.class)
class RocketMqPatternConsumerTest {

    @Mock
    private PatternService patternService;

    @InjectMocks
    private RocketMqPatternConsumer rocketMqPatternConsumer;

    @Test
    @DisplayName("消费消息 - 成功处理")
    void testOnMessage_Success() {
        String message = "{\"bodySizeId\":1,\"patternType\":1,\"userId\":100,\"patternName\":\"测试\"}";
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        when(patternService.generatePattern(anyLong(), any(PatternGenerateRequest.class)))
                .thenReturn(record);

        assertDoesNotThrow(() -> rocketMqPatternConsumer.onMessage(message));
        verify(patternService).generatePattern(eq(100L), any(PatternGenerateRequest.class));
    }

    @Test
    @DisplayName("消费消息 - userId为null")
    void testOnMessage_NullUserId() {
        String message = "{\"bodySizeId\":1,\"patternType\":1,\"userId\":null}";

        assertDoesNotThrow(() -> rocketMqPatternConsumer.onMessage(message));
        verify(patternService, never()).generatePattern(anyLong(), any(PatternGenerateRequest.class));
    }

    @Test
    @DisplayName("消费消息 - bodySizeId为null")
    void testOnMessage_NullBodySizeId() {
        String message = "{\"bodySizeId\":null,\"patternType\":1,\"userId\":100}";

        assertDoesNotThrow(() -> rocketMqPatternConsumer.onMessage(message));
        verify(patternService, never()).generatePattern(anyLong(), any(PatternGenerateRequest.class));
    }

    @Test
    @DisplayName("消费消息 - patternType为null")
    void testOnMessage_NullPatternType() {
        String message = "{\"bodySizeId\":1,\"patternType\":null,\"userId\":100}";

        assertDoesNotThrow(() -> rocketMqPatternConsumer.onMessage(message));
        verify(patternService, never()).generatePattern(anyLong(), any(PatternGenerateRequest.class));
    }

    @Test
    @DisplayName("消费消息 - 服务层异常")
    void testOnMessage_ServiceException() {
        String message = "{\"bodySizeId\":1,\"patternType\":1,\"userId\":100}";
        when(patternService.generatePattern(anyLong(), any(PatternGenerateRequest.class)))
                .thenThrow(new RuntimeException("生成失败"));

        assertThrows(RuntimeException.class, () -> rocketMqPatternConsumer.onMessage(message));
    }

    @Test
    @DisplayName("消费消息 - JSON解析异常")
    void testOnMessage_InvalidJson() {
        String message = "invalid json";

        assertThrows(Exception.class, () -> rocketMqPatternConsumer.onMessage(message));
    }

    @Test
    @DisplayName("消费消息 - 空消息")
    void testOnMessage_EmptyMessage() {
        String message = "";

        assertThrows(Exception.class, () -> rocketMqPatternConsumer.onMessage(message));
    }

    @Test
    @DisplayName("消费消息 - null消息")
    void testOnMessage_NullMessage() {
        assertThrows(Exception.class, () -> rocketMqPatternConsumer.onMessage(null));
    }
}
