package com.tailoris.payment.config;

import com.tailoris.payment.mq.PaymentCallbackAsyncHandler;
import com.tailoris.payment.mq.PaymentNotificationConsumer;
import com.tailoris.payment.mq.SettlementProcessingConsumer;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 - 声明队列.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue paymentCallbackQueue() {
        return new Queue(PaymentCallbackAsyncHandler.PAYMENT_CALLBACK_QUEUE, true);
    }

    @Bean
    public Queue paymentCallbackDlq() {
        return new Queue(PaymentCallbackAsyncHandler.PAYMENT_CALLBACK_DLQ, true);
    }

    @Bean
    public Queue settlementProcessQueue() {
        return new Queue(SettlementProcessingConsumer.SETTLEMENT_PROCESS_QUEUE, true);
    }

    @Bean
    public Queue paymentNotifyQueue() {
        return new Queue(PaymentNotificationConsumer.PAYMENT_NOTIFY_QUEUE, true);
    }
}