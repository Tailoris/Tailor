package com.tailoris.ai.mq;

import com.alibaba.fastjson2.JSON;
import com.tailoris.common.mq.MessageRoutingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RocketMqPatternProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Autowired(required = false)
    public RocketMqPatternProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void sendBatchGenerateTask(Object payload) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendBatchGenerateTask");
            return;
        }
        String topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC;
        String tag = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE;
        String destination = topic + ":" + tag;

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ batch pattern generate task sent to {}", destination);
    }

    public void sendPatternIterateTask(Object payload) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendPatternIterateTask");
            return;
        }
        String topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC;
        String tag = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_ITERATE;
        String destination = topic + ":" + tag;

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ pattern iterate task sent to {}", destination);
    }

    public void sendPatternCheckTask(Object payload) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendPatternCheckTask");
            return;
        }
        String topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC;
        String tag = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_CHECK;
        String destination = topic + ":" + tag;

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ pattern check task sent to {}", destination);
    }

    public void sendOfflineRenderTask(Object payload) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendOfflineRenderTask");
            return;
        }
        String destination = MessageRoutingStrategy.ROCKETMQ_AI_RENDER_TOPIC + ":render";

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ offline render task sent to {}", destination);
    }

    public void sendBatchGenerateTaskAsync(Object payload) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendBatchGenerateTaskAsync");
            return;
        }
        String destination = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC
                + ":" + MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE;

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("RocketMQ async batch pattern task sent successfully");
            }

            @Override
            public void onException(Throwable e) {
                log.error("RocketMQ async batch pattern task send failed", e);
            }
        });
    }
}
