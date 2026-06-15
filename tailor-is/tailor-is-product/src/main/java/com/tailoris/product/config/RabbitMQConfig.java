package com.tailoris.product.config;

import com.tailoris.product.mq.InventoryReleaseConsumer;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 - 声明队列.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue inventoryReleaseQueue() {
        return new Queue(InventoryReleaseConsumer.INVENTORY_RELEASE_QUEUE, true);
    }
}