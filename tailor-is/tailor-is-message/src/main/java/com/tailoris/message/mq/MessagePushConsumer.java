package com.tailoris.message.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePushConsumer {

    public static final String MESSAGE_PUSH_QUEUE = "message.push.queue";

    @Bean
    public Queue messagePushQueue() {
        return new Queue(MESSAGE_PUSH_QUEUE, true);
    }

    @RabbitListener(queues = MESSAGE_PUSH_QUEUE)
    public void handleMessagePush(String messageBody) {
        log.info("收到消息推送任务, body: {}", messageBody);
        log.info("消息推送处理完成");
    }
}