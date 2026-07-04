package com.tailoris.product.config;

import com.tailoris.product.mq.InventoryReleaseConsumer;
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

    /** 商品事件交换机 */
    public static final String PRODUCT_EVENT_EXCHANGE = "product.event.exchange";

    /** 商品事件队列 */
    public static final String PRODUCT_EVENT_QUEUE = "product.event.queue";

    @Bean
    public Queue inventoryReleaseQueue() {
        return new Queue(InventoryReleaseConsumer.INVENTORY_RELEASE_QUEUE, true);
    }

    @Bean
    public Queue productEventQueue() {
        return new Queue(PRODUCT_EVENT_QUEUE, true);
    }

    @Bean
    public TopicExchange productEventExchange() {
        return new TopicExchange(PRODUCT_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Binding productEventBinding() {
        return BindingBuilder.bind(productEventQueue())
                .to(productEventExchange())
                .with("product.#");
    }
}