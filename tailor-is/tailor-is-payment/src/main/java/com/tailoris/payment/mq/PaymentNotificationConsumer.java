package com.tailoris.payment.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付通知消费者
 *
 * <p>接收支付渠道的异步通知，更新支付记录状态并触发后续业务流程。
 * 消息格式（JSON）：
 * <pre>
 * {
 *     "paymentNo": "PAY202401010001",
 *     "payStatus": 1,
 *     "transactionId": "TXN123456789",
 *     "notifyTime": "2024-01-01T12:00:00"
 * }
 * </pre>
 * </p>
 *
 * <p>支付状态：0-待支付，1-支付成功，2-支付失败，3-已退款</p>
 */
@Slf4j
@Component
public class PaymentNotificationConsumer {

    public static final String PAYMENT_NOTIFY_QUEUE = "payment.notify.queue";

    private final PaymentRecordMapper paymentRecordMapper;
    private final ObjectMapper objectMapper;

    public PaymentNotificationConsumer(PaymentRecordMapper paymentRecordMapper,
                                       ObjectMapper objectMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = PAYMENT_NOTIFY_QUEUE)
    public void handlePaymentNotification(String messageBody) {
        log.info("收到支付通知消息: {}", messageBody);

        try {
            Map<String, Object> message = objectMapper.readValue(messageBody,
                    new TypeReference<Map<String, Object>>() {});

            String paymentNo = (String) message.get("paymentNo");
            Object payStatusObj = message.get("payStatus");
            Integer payStatus = payStatusObj != null
                    ? ((Number) payStatusObj).intValue() : null;
            String transactionId = (String) message.get("transactionId");
            String notifyTimeStr = (String) message.get("notifyTime");

            if (paymentNo == null || payStatus == null) {
                log.warn("支付通知消息参数不完整: paymentNo={}, payStatus={}", paymentNo, payStatus);
                return;
            }

            // 查询支付记录（paymentNo 通常也是主键ID或业务编号）
            PaymentRecord payment = paymentRecordMapper.selectById(paymentNo);
            if (payment == null) {
                // 尝试按 payment_no 字段查询
                payment = paymentRecordMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentRecord>()
                                .eq(PaymentRecord::getPaymentNo, paymentNo));
            }

            if (payment == null) {
                log.warn("支付记录不存在: paymentNo={}", paymentNo);
                return;
            }

            Integer oldStatus = payment.getPayStatus();
            payment.setPayStatus(payStatus);

            if (transactionId != null && !transactionId.isEmpty()) {
                payment.setTransactionId(transactionId);
            }

            // 支付成功时记录支付时间
            if (payStatus == 1 && payment.getPayTime() == null) {
                payment.setPayTime(LocalDateTime.now());
            }

            // 更新通知状态
            payment.setNotifyStatus(1);
            if (notifyTimeStr != null && !notifyTimeStr.isEmpty()) {
                try {
                    payment.setNotifyTime(LocalDateTime.parse(notifyTimeStr));
                } catch (Exception e) {
                    payment.setNotifyTime(LocalDateTime.now());
                }
            } else {
                payment.setNotifyTime(LocalDateTime.now());
            }

            paymentRecordMapper.updateById(payment);

            log.info("支付通知处理完成: paymentNo={}, oldStatus={}, newStatus={}, transactionId={}",
                    paymentNo, oldStatus, payStatus, transactionId);

            // TODO: 支付状态变更后，需要触发后续业务流程（如通知订单服务更新订单状态）
            // 当前通过 payment.notify.queue 收到通知后仅更新本地支付记录，
            // 后续需通过 RabbitMQ 发送 order.status.sync 消息或调用 OrderClient Feign 接口同步订单状态

        } catch (Exception e) {
            log.error("支付通知处理异常: message={}", messageBody, e);
            throw new RuntimeException("支付通知处理失败", e);
        }
    }
}