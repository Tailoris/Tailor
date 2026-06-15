package com.tailoris.ai.mq;

import com.tailoris.common.mq.MessageRoutingStrategy;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("RocketMqPatternProducer 单元测试")
@ExtendWith(MockitoExtension.class)
class RocketMqPatternProducerTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @InjectMocks
    private RocketMqPatternProducer rocketMqPatternProducer;

    @Test
    @DisplayName("发送批量生成任务 - 同步")
    void testSendBatchGenerateTask_Sync() {
        Object payload = new Object();
        SendResult sendResult = mock(SendResult.class);
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class))).thenReturn(sendResult);

        assertDoesNotThrow(() -> rocketMqPatternProducer.sendBatchGenerateTask(payload));
        verify(rocketMQTemplate).syncSend(
                eq(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC + ":" +
                   MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE),
                any(Message.class)
        );
    }

    @Test
    @DisplayName("发送批量生成任务 - 同步异常")
    void testSendBatchGenerateTask_SyncException() {
        Object payload = new Object();
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class)))
                .thenThrow(new RuntimeException("发送失败"));

        assertThrows(RuntimeException.class, () -> rocketMqPatternProducer.sendBatchGenerateTask(payload));
    }

    @Test
    @DisplayName("发送迭代任务")
    void testSendPatternIterateTask() {
        Object payload = new Object();
        SendResult sendResult = mock(SendResult.class);
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class))).thenReturn(sendResult);

        assertDoesNotThrow(() -> rocketMqPatternProducer.sendPatternIterateTask(payload));
        verify(rocketMQTemplate).syncSend(
                eq(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC + ":" +
                   MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_ITERATE),
                any(Message.class)
        );
    }

    @Test
    @DisplayName("发送检查任务")
    void testSendPatternCheckTask() {
        Object payload = new Object();
        SendResult sendResult = mock(SendResult.class);
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class))).thenReturn(sendResult);

        assertDoesNotThrow(() -> rocketMqPatternProducer.sendPatternCheckTask(payload));
        verify(rocketMQTemplate).syncSend(
                eq(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC + ":" +
                   MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_CHECK),
                any(Message.class)
        );
    }

    @Test
    @DisplayName("发送离线渲染任务")
    void testSendOfflineRenderTask() {
        Object payload = new Object();
        SendResult sendResult = mock(SendResult.class);
        when(rocketMQTemplate.syncSend(anyString(), any(Message.class))).thenReturn(sendResult);

        assertDoesNotThrow(() -> rocketMqPatternProducer.sendOfflineRenderTask(payload));
        verify(rocketMQTemplate).syncSend(
                eq(MessageRoutingStrategy.ROCKETMQ_AI_RENDER_TOPIC + ":render"),
                any(Message.class)
        );
    }

    @Test
    @DisplayName("发送批量生成任务 - 异步成功")
    void testSendBatchGenerateTaskAsync_Success() {
        Object payload = new Object();
        doNothing().when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        assertDoesNotThrow(() -> rocketMqPatternProducer.sendBatchGenerateTaskAsync(payload));
        verify(rocketMQTemplate).asyncSend(
                eq(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC + ":" +
                   MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE),
                any(Message.class),
                any(SendCallback.class)
        );
    }

    @Test
    @DisplayName("发送批量生成任务 - 异步异常")
    void testSendBatchGenerateTaskAsync_Exception() {
        Object payload = new Object();
        doThrow(new RuntimeException("异步发送失败"))
                .when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        assertThrows(RuntimeException.class, () -> rocketMqPatternProducer.sendBatchGenerateTaskAsync(payload));
    }

    @Test
    @DisplayName("SendCallback - onSuccess")
    void testSendCallback_OnSuccess() {
        Object payload = new Object();
        SendResult sendResult = mock(SendResult.class);

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onSuccess(sendResult);
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        assertDoesNotThrow(() -> rocketMqPatternProducer.sendBatchGenerateTaskAsync(payload));
    }

    @Test
    @DisplayName("SendCallback - onException")
    void testSendCallback_OnException() {
        Object payload = new Object();
        Throwable exception = new RuntimeException("发送异常");

        doAnswer(invocation -> {
            SendCallback callback = invocation.getArgument(2);
            callback.onException(exception);
            return null;
        }).when(rocketMQTemplate).asyncSend(anyString(), any(Message.class), any(SendCallback.class));

        assertDoesNotThrow(() -> rocketMqPatternProducer.sendBatchGenerateTaskAsync(payload));
    }
}
