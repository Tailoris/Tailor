package com.tailoris.order.config;

import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.mq.OrderStatusSyncConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置.
 *
 * <p>🔒 B-L10修复: 启用 RabbitTemplate 发布确认（Publisher Confirms）和返回回调（Returns），
 *    保证消息可靠投递：</p>
 * <ul>
 *   <li>{@code ConfirmCallback}：消息成功到达 Broker 时回调（CorrelationData 关联）</li>
 *   <li>{@code ReturnsCallback}：消息被 Broker 拒收（路由失败）时回调</li>
 *   <li>失败消息可写入数据库兜底表，配合定时任务重试</li>
 * </ul>
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    /**
     * 声明订单超时队列（始终创建，即使延迟交换机不可用）.
     */
    @Bean
    public Queue orderTimeoutQueue() {
        return new Queue(OrderConstants.ORDER_TIMEOUT_QUEUE, true);
    }

    /**
     * 声明订单状态同步队列.
     */
    @Bean
    public Queue orderStatusSyncQueue() {
        return new Queue(OrderStatusSyncConsumer.ORDER_STATUS_SYNC_QUEUE, true);
    }

    /**
     * 延迟交换机（需要 rabbitmq_delayed_message_exchange 插件）.
     * 仅在启用了插件时创建，可通过 rabbitmq.delayed-exchange.enabled=true 控制.
     */
    @Bean
    @ConditionalOnProperty(name = "rabbitmq.delayed-exchange.enabled", havingValue = "true")
    public CustomExchange orderTimeoutExchange() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-delayed-type", "direct");
        return new CustomExchange(
                OrderConstants.ORDER_TIMEOUT_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                arguments
        );
    }

    @Bean
    @ConditionalOnProperty(name = "rabbitmq.delayed-exchange.enabled", havingValue = "true")
    public Binding orderTimeoutBinding() {
        return BindingBuilder.bind(orderTimeoutQueue())
                .to(orderTimeoutExchange())
                .with(OrderConstants.ORDER_TIMEOUT_ROUTING_KEY)
                .noargs();
    }

    /**
     * 配置 RabbitTemplate 发布确认与返回回调.
     *
     * <p>🔒 B-L10修复: 必须在 application.yml 中开启 {@code spring.rabbitmq.publisher-confirm-type=correlated}
     *    与 {@code spring.rabbitmq.publisher-returns=true}，否则回调不会触发。</p>
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 1. Confirm 回调: 消息成功到达 Broker
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("🔴 [B-L10] RabbitMQ 消息发送失败, correlationData={}, cause={}",
                        correlationData, cause);
                // Phase3: 实现可靠投递机制 — 写入 msg_send_failed 表 + 定时补偿重试
            } else {
                log.debug("✅ [B-L10] RabbitMQ 消息已确认, correlationData={}", correlationData);
            }
        });

        // 2. Returns 回调: 消息被 Broker 拒收（路由不到队列）
        template.setReturnsCallback(returned -> {
            log.error("🔴 [B-L10] RabbitMQ 消息被退回, exchange={}, routingKey={}, replyText={}, message={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyText(),
                    returned.getMessage());
            // Phase3: 实现可靠投递机制 — 写入 msg_send_failed 表 + 定时补偿重试
        });

        // 3. 启用强制消息路由（mandatory=true），无路由时触发 ReturnCallback
        template.setMandatory(true);

        return template;
    }
}
