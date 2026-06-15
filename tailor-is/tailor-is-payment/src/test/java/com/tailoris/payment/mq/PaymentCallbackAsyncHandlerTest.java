package com.tailoris.payment.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.client.OrderClient;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PaymentCallbackAsyncHandler 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCallbackAsyncHandlerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private OrderClient orderClient;

    // 使用真实的 ObjectMapper，避免复杂的 mock
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentCallbackAsyncHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        // 手动注入真实的 objectMapper
        try {
            java.lang.reflect.Field field = PaymentCallbackAsyncHandler.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(handler, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 设置 @Value 字段
        ReflectionTestUtils.setField(handler, "maxRetry", 3);
        ReflectionTestUtils.setField(handler, "asyncEnabled", true);
    }

    @Test
    @DisplayName("发送回调消息-异步模式成功")
    void testSendCallbackMessage_AsyncSuccess() {
        handler.sendCallbackMessage("PAY123456", "TXN123", "{}", 100L);

        verify(rabbitTemplate).convertAndSend(eq("payment.callback.async.queue"), anyString());
    }

    @Test
    @DisplayName("发送回调消息-异步禁用时同步处理")
    void testSendCallbackMessage_SyncMode() {
        ReflectionTestUtils.setField(handler, "asyncEnabled", false);

        handler.sendCallbackMessage("PAY123456", "TXN123", "{}", 100L);

        verify(paymentService).payCallback("PAY123456", "TXN123", "{}", 100L);
        verify(rabbitTemplate, never()).convertAndSend(eq("payment.callback.async.queue"), anyString());
    }

    @Test
    @DisplayName("发送回调消息-MQ发送失败降级同步处理")
    void testSendCallbackMessage_FallbackToSync() {
        doThrow(new RuntimeException("MQ连接失败")).when(rabbitTemplate).convertAndSend(anyString(), anyString());

        handler.sendCallbackMessage("PAY123456", "TXN123", "{}", 100L);

        verify(paymentService).payCallback("PAY123456", "TXN123", "{}", 100L);
    }

    @Test
    @DisplayName("发送回调消息-MQ发送失败且降级同步也失败")
    void testSendCallbackMessage_FallbackAlsoFails() {
        doThrow(new RuntimeException("MQ连接失败")).when(rabbitTemplate).convertAndSend(anyString(), anyString());
        doThrow(new BusinessException("回调处理失败")).when(paymentService).payCallback(anyString(), anyString(), anyString(), any());

        assertDoesNotThrow(() -> handler.sendCallbackMessage("PAY123456", "TXN123", "{}", 100L));
    }

    @Test
    @DisplayName("处理异步回调-成功")
    void testHandleAsyncCallback_Success() throws Exception {
        String jsonMessage = "{\"paymentNo\":\"PAY123456\",\"transactionId\":\"TXN123\",\"channelResponse\":\"{}\",\"merchantId\":100,\"retryCount\":0,\"requestId\":\"PAY123456_123\"}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);

        handler.handleAsyncCallback(jsonMessage);

        verify(paymentService).payCallback(eq("PAY123456"), eq("TXN123"), eq("{}"), eq(100L));
        verify(valueOperations).set(anyString(), eq("SUCCESS"), any(Duration.class));
    }

    @Test
    @DisplayName("处理异步回调-幂等冲突")
    void testHandleAsyncCallback_IdempotentConflict() throws Exception {
        String jsonMessage = "{\"paymentNo\":\"PAY123456\",\"transactionId\":\"TXN123\",\"merchantId\":100,\"retryCount\":0}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(false);

        handler.handleAsyncCallback(jsonMessage);

        verify(paymentService, never()).payCallback(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("处理异步回调-业务异常正在处理")
    void testHandleAsyncCallback_BusinessExceptionProcessing() throws Exception {
        String jsonMessage = "{\"paymentNo\":\"PAY123456\",\"transactionId\":\"TXN123\",\"channelResponse\":\"{}\",\"merchantId\":100,\"retryCount\":0}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);
        doThrow(new BusinessException("支付回调正在处理")).when(paymentService).payCallback(anyString(), anyString(), any(), eq(100L));

        handler.handleAsyncCallback(jsonMessage);

        verify(valueOperations, never()).set(anyString(), eq("SUCCESS"), any(Duration.class));
    }

    @Test
    @DisplayName("处理异步回调-异常触发重试")
    void testHandleAsyncCallback_Retry() throws Exception {
        String jsonMessage = "{\"paymentNo\":\"PAY123456\",\"transactionId\":\"TXN123\",\"channelResponse\":\"{}\",\"merchantId\":100,\"retryCount\":0}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);
        doThrow(new RuntimeException("处理失败")).when(paymentService).payCallback(anyString(), anyString(), any(), eq(100L));

        handler.handleAsyncCallback(jsonMessage);

        // 验证重试消息被发送，使用 ArgumentCaptor 避免方法重载歧义
        verify(rabbitTemplate).convertAndSend(
                eq("payment.callback.async.queue"),
                any(Object.class),
                any(MessagePostProcessor.class));
    }

    @Test
    @DisplayName("处理异步回调-超过最大重试次数发送到死信队列")
    void testHandleAsyncCallback_MaxRetryReached() throws Exception {
        String jsonMessage = "{\"paymentNo\":\"PAY123456\",\"transactionId\":\"TXN123\",\"channelResponse\":\"{}\",\"merchantId\":100,\"retryCount\":3}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);
        doThrow(new RuntimeException("处理失败")).when(paymentService).payCallback(anyString(), anyString(), any(), eq(100L));

        handler.handleAsyncCallback(jsonMessage);

        verify(rabbitTemplate).convertAndSend(eq("payment.callback.async.dlq"), any(Object.class));
    }

    @Test
    @DisplayName("通知订单服务-成功")
    void testNotifyOrderService_Success() throws Exception {
        String jsonMessage = "{\"paymentNo\":\"123456\",\"transactionId\":\"TXN123\",\"merchantId\":100,\"retryCount\":0}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);
        when(orderClient.getOrderDetail(anyLong())).thenReturn(null);

        handler.handleAsyncCallback(jsonMessage);

        verify(orderClient).getOrderDetail(123456L);
    }

    @Test
    @DisplayName("通知订单服务-客户端不可用")
    void testNotifyOrderService_ClientUnavailable() throws Exception {
        PaymentCallbackAsyncHandler handlerWithoutClient = new PaymentCallbackAsyncHandler(
                paymentService, rabbitTemplate, stringRedisTemplate, objectMapper, null
        );
        ReflectionTestUtils.setField(handlerWithoutClient, "maxRetry", 3);
        ReflectionTestUtils.setField(handlerWithoutClient, "asyncEnabled", true);

        String jsonMessage = "{\"paymentNo\":\"123456\",\"transactionId\":\"TXN123\",\"merchantId\":100,\"retryCount\":0}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);

        handlerWithoutClient.handleAsyncCallback(jsonMessage);

        verify(orderClient, never()).getOrderDetail(anyLong());
    }

    @Test
    @DisplayName("通知订单服务-通知失败非致命")
    void testNotifyOrderService_NotifyFail() throws Exception {
        String jsonMessage = "{\"paymentNo\":\"123456\",\"transactionId\":\"TXN123\",\"merchantId\":100,\"retryCount\":0}";

        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);
        doThrow(new RuntimeException("通知失败")).when(orderClient).getOrderDetail(anyLong());

        assertDoesNotThrow(() -> handler.handleAsyncCallback(jsonMessage));
    }

    @Test
    @DisplayName("处理异步回调-解析异常时记录日志")
    void testHandleAsyncCallback_ParseException() throws Exception {
        String jsonMessage = "invalid json";

        assertDoesNotThrow(() -> handler.handleAsyncCallback(jsonMessage));
    }
}
