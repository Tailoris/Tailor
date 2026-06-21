package com.tailoris.ai.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class RocketMQConfig {

    @Bean("aiPatternTaskConsumerExecutor")
    public ExecutorService aiPatternTaskConsumerExecutor(
            @Value("${tailoris.ai.mq.consumer.core-pool-size:4}") int corePoolSize,
            @Value("${tailoris.ai.mq.consumer.max-pool-size:8}") int maxPoolSize,
            @Value("${tailoris.ai.mq.consumer.queue-capacity:200}") int queueCapacity) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy());
        log.info("AI pattern task consumer thread pool initialized: coreSize={}, maxSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);
        return executor;
    }
}