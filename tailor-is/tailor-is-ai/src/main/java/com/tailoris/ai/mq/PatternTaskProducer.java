package com.tailoris.ai.mq;

import com.alibaba.fastjson2.JSON;
import com.tailoris.ai.model.PatternTask;
import com.tailoris.common.mq.MessageRoutingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = false)
public class PatternTaskProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Autowired(required = false)
    public PatternTaskProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void sendPatternGenerateTask(PatternTask task) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendPatternGenerateTask: taskId={}", task.getTaskId());
            return;
        }
        String destination = buildDestination(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE);
        Message<String> message = buildMessage(task);
        rocketMQTemplate.syncSend(destination, message);
        log.info("Pattern generate task sent: taskId={}, userId={}", task.getTaskId(), task.getUserId());
    }

    public void sendBatchPatternTask(List<PatternTask> tasks) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendBatchPatternTask: count={}", tasks.size());
            return;
        }
        String destination = buildDestination(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE);
        for (PatternTask task : tasks) {
            Message<String> message = buildMessage(task);
            rocketMQTemplate.syncSend(destination, message);
        }
        log.info("Batch pattern tasks sent: count={}", tasks.size());
    }

    public void sendPatternGenerateTaskAsync(PatternTask task, SendCallback callback) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendPatternGenerateTaskAsync: taskId={}", task.getTaskId());
            return;
        }
        String destination = buildDestination(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE);
        Message<String> message = buildMessage(task);
        rocketMQTemplate.asyncSend(destination, message, callback != null ? callback : new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("Pattern task async sent successfully: taskId={}, msgId={}", task.getTaskId(), sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.error("Pattern task async send failed: taskId={}", task.getTaskId(), e);
            }
        });
    }

    public void sendPatternGenerateTaskAsync(PatternTask task) {
        sendPatternGenerateTaskAsync(task, null);
    }

    public void sendDelayPatternTask(PatternTask task, int delayLevel) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendDelayPatternTask: taskId={}", task.getTaskId());
            return;
        }
        String destination = buildDestination(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE);
        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(task))
                .build();
        rocketMQTemplate.syncSend(destination, message, rocketMQTemplate.getProducer().getSendMsgTimeout(), delayLevel);
        log.info("Delay pattern task sent: taskId={}, delayLevel={}", task.getTaskId(), delayLevel);
    }

    public void sendOrderedPatternTask(PatternTask task, String hashKey) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ not available, skipping sendOrderedPatternTask: taskId={}", task.getTaskId());
            return;
        }
        String destination = buildDestination(MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE);
        Message<String> message = buildMessage(task);
        rocketMQTemplate.syncSendOrderly(destination, message, hashKey);
        log.info("Ordered pattern task sent: taskId={}, hashKey={}", task.getTaskId(), hashKey);
    }

    private String buildDestination(String tag) {
        return MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC + ":" + tag;
    }

    private Message<String> buildMessage(PatternTask task) {
        return MessageBuilder
                .withPayload(JSON.toJSONString(task))
                .build();
    }
}
