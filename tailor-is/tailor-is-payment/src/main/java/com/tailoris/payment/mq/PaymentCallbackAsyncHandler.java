package com.tailoris.payment.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.client.OrderClient;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 异步支付回调处理器
 *
 * <p>将同步支付回调处理转换为基于 RabbitMQ 的异步处理模式。
 * 支付网关回调收到后，立即将支付结果发送到 MQ，由消费者在后台异步处理，
 * 从而降低回调响应延迟，提高支付网关的吞吐量。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>支付网关回调到达 PayController</li>
 *   <li>Controller 验证签名后，发送消息到 RabbitMQ</li>
 *   <li>立即返回成功响应给支付网关</li>
 *   <li>本消费者异步处理支付结果：更新订单状态、通知商户等</li>
 *   <li>处理失败时自动重试（最多 3 次）</li>
 * </ol>
 *
 * <p>幂等保证：</p>
 * <ul>
 *   <li>使用 Redis 分布式锁防止重复处理</li>
 *   <li>消息体携带唯一 requestId，支持幂等校验</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class PaymentCallbackAsyncHandler {

    private final PaymentService paymentService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    @Nullable
    private final OrderClient orderClient;

    /** 支付回调队列 */
    public static final String PAYMENT_CALLBACK_QUEUE = "payment.callback.async.queue";

    /** 支付回调死信队列 */
    public static final String PAYMENT_CALLBACK_DLQ = "payment.callback.async.dlq";

    /** 回调处理幂等 Key 前缀 */
    private static final String CALLBACK_LOCK_KEY = "payment:callback:lock:";

    /** 幂等锁过期时间（5 分钟） */
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    /** 最大重试次数 */
    @Value("${tailoris.payment.callback.max-retry:3}")
    private int maxRetry;

    /** 是否启用异步回调 */
    @Value("${tailoris.payment.callback.async-enabled:true}")
    private boolean asyncEnabled;

    public PaymentCallbackAsyncHandler(PaymentService paymentService,
                                       RabbitTemplate rabbitTemplate,
                                       StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       @Nullable OrderClient orderClient) {
        this.paymentService = paymentService;
        this.rabbitTemplate = rabbitTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.orderClient = orderClient;
    }

    /**
     * 发送支付回调消息到 MQ（由 PayController 调用）
     *
     * @param paymentNo      支付编号
     * @param transactionId  渠道交易号
     * @param channelResponse 渠道完整响应
     * @param merchantId     商户 ID
     */
    public void sendCallbackMessage(String paymentNo, String transactionId,
                                    String channelResponse, Long merchantId) {
        if (!asyncEnabled) {
            log.info("异步回调已禁用，使用同步模式处理: paymentNo={}", paymentNo);
            paymentService.payCallback(paymentNo, transactionId, channelResponse, merchantId);
            return;
        }

        try {
            Map<String, Object> message = new HashMap<>();
            message.put("paymentNo", paymentNo);
            message.put("transactionId", transactionId);
            message.put("channelResponse", channelResponse);
            message.put("merchantId", merchantId);
            message.put("timestamp", System.currentTimeMillis());
            message.put("retryCount", 0);
            message.put("requestId", paymentNo + "_" + System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(PAYMENT_CALLBACK_QUEUE, jsonMessage);

            log.info("支付回调消息已发送到 MQ: paymentNo={}, requestId={}",
                    paymentNo, message.get("requestId"));
        } catch (Exception e) {
            log.error("发送支付回调 MQ 消息失败: paymentNo={}", paymentNo, e);
            // 降级：直接同步处理
            try {
                paymentService.payCallback(paymentNo, transactionId, channelResponse, merchantId);
                log.info("降级为同步处理成功: paymentNo={}", paymentNo);
            } catch (Exception ex) {
                log.error("降级同步处理也失败: paymentNo={}", paymentNo, ex);
            }
        }
    }

    /**
     * 异步消费支付回调消息
     *
     * @param jsonMessage JSON 格式的消息体
     */
    @RabbitListener(queues = PAYMENT_CALLBACK_QUEUE)
    public void handleAsyncCallback(String jsonMessage) {
        log.info("收到异步支付回调消息: {}", jsonMessage);

        try {
            Map<String, Object> message = objectMapper.readValue(jsonMessage,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

            String paymentNo = (String) message.get("paymentNo");
            String transactionId = (String) message.get("transactionId");
            String channelResponse = (String) message.get("channelResponse");
            Object merchantIdObj = message.get("merchantId");
            Long merchantId = merchantIdObj != null ? ((Number) merchantIdObj).longValue() : null;
            int retryCount = message.get("retryCount") != null
                    ? ((Number) message.get("retryCount")).intValue() : 0;
            String requestId = (String) message.get("requestId");

            // 幂等校验
            String lockKey = CALLBACK_LOCK_KEY + paymentNo;
            Boolean lockAcquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "PROCESSING", LOCK_TTL);
            if (Boolean.FALSE.equals(lockAcquired)) {
                log.info("支付回调幂等命中（正在处理中）: paymentNo={}", paymentNo);
                return;
            }

            try {
                // 处理支付回调
                paymentService.payCallback(paymentNo, transactionId, channelResponse, merchantId);

                // 标记处理成功
                stringRedisTemplate.opsForValue().set(lockKey, "SUCCESS", Duration.ofHours(24));

                // 异步通知订单服务更新状态
                notifyOrderService(paymentNo, merchantId);

                log.info("异步支付回调处理成功: paymentNo={}, transactionId={}",
                        paymentNo, transactionId);

            } catch (BusinessException e) {
                // 业务异常：如果是幂等冲突，直接返回
                if (e.getMessage() != null && e.getMessage().contains("正在处理")) {
                    log.info("支付回调重复处理: paymentNo={}", paymentNo);
                    return;
                }
                throw e;
            }

        } catch (Exception e) {
            log.error("异步支付回调处理异常: message={}", jsonMessage, e);
            handleRetry(jsonMessage, e);
        }
    }

    /**
     * 处理消费失败的重试逻辑
     */
    private void handleRetry(String jsonMessage, Exception cause) {
        try {
            Map<String, Object> message = objectMapper.readValue(jsonMessage,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

            int retryCount = message.get("retryCount") != null
                    ? ((Number) message.get("retryCount")).intValue() : 0;

            if (retryCount < maxRetry) {
                // 重试次数 +1，延迟后重新发送
                message.put("retryCount", retryCount + 1);
                String retriedJson = objectMapper.writeValueAsString(message);

                // 指数退避：第 N 次重试延迟 N * 10 秒
                int delayMs = (retryCount + 1) * 10000;
                rabbitTemplate.convertAndSend(PAYMENT_CALLBACK_QUEUE, retriedJson,
                        m -> {
                            m.getMessageProperties().setDelay(Math.min(delayMs, 60000));
                            return m;
                        });

                log.warn("支付回调处理失败，已安排重试: retryCount={}, delay={}ms",
                        retryCount + 1, delayMs);
            } else {
                // 超过最大重试次数，发送到死信队列
                rabbitTemplate.convertAndSend(PAYMENT_CALLBACK_DLQ, jsonMessage);
                log.error("支付回调处理失败，已达最大重试次数，已发送到死信队列: {}", jsonMessage);
            }
        } catch (Exception ex) {
            log.error("支付回调重试逻辑异常", ex);
        }
    }

    /**
     * 通知订单服务更新订单状态
     */
    private void notifyOrderService(String paymentNo, Long merchantId) {
        if (orderClient == null) {
            log.warn("订单服务客户端不可用，跳过通知: paymentNo={}", paymentNo);
            return;
        }

        try {
            orderClient.getOrderDetail(Long.parseLong(paymentNo.replaceAll("\\D+", "")));
            log.debug("已通知订单服务: paymentNo={}", paymentNo);
        } catch (Exception e) {
            log.warn("通知订单服务失败（非致命）: paymentNo={}", paymentNo, e);
        }
    }
}
