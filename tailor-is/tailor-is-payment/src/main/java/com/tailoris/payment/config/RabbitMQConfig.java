package com.tailoris.payment.config;

import com.tailoris.payment.mq.PaymentCallbackAsyncHandler;
import com.tailoris.payment.mq.PaymentNotificationConsumer;
import com.tailoris.payment.mq.SettlementProcessingConsumer;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 - 声明队列、交换机和绑定.
 */
@Configuration
public class RabbitMQConfig {

    /** 批量结算交换机 */
    public static final String SETTLEMENT_BATCH_EXCHANGE = "settlement.batch.exchange";

    /** 批量结算路由键 */
    public static final String SETTLEMENT_BATCH_ROUTING_KEY = "settlement.batch";

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

    @Bean
    public TopicExchange settlementBatchExchange() {
        return new TopicExchange(SETTLEMENT_BATCH_EXCHANGE, true, false);
    }

    @Bean
    public Binding settlementProcessBinding() {
        return BindingBuilder.bind(settlementProcessQueue())
                .to(settlementBatchExchange())
                .with(SETTLEMENT_BATCH_ROUTING_KEY);
    }

    /** 支付通知交换机 */
    public static final String PAYMENT_NOTIFY_EXCHANGE = "payment.notify.exchange";

    /** 支付通知路由键 */
    public static final String PAYMENT_NOTIFY_ROUTING_KEY = "payment.notify";

    @Bean
    public TopicExchange paymentNotifyExchange() {
        return new TopicExchange(PAYMENT_NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public Binding paymentNotifyBinding() {
        return BindingBuilder.bind(paymentNotifyQueue())
                .to(paymentNotifyExchange())
                .with(PAYMENT_NOTIFY_ROUTING_KEY);
    }
}