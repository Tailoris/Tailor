package com.tailoris.ai.mq;

import com.alibaba.fastjson2.JSON;
import com.tailoris.common.mq.MessageRoutingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 纸样批量任务生产者
 *
 * <p>用于 AI 批量异步场景：</p>
 * <ul>
 *   <li>批量纸样生成任务投递</li>
 *   <li>纸样迭代计算任务投递</li>
 *   <li>纸样结构检查任务投递</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RocketMQTemplate.class)
public class RocketMqPatternProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送批量纸样生成任务
     *
     * @param payload 任务负载对象
     */
    public void sendBatchGenerateTask(Object payload) {
        String topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC;
        String tag = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE;
        String destination = topic + ":" + tag;

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ batch pattern generate task sent to {}", destination);
    }

    /**
     * 发送纸样迭代任务
     *
     * @param payload 任务负载对象
     */
    public void sendPatternIterateTask(Object payload) {
        String topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC;
        String tag = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_ITERATE;
        String destination = topic + ":" + tag;

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ pattern iterate task sent to {}", destination);
    }

    /**
     * 发送纸样检查任务
     *
     * @param payload 任务负载对象
     */
    public void sendPatternCheckTask(Object payload) {
        String topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC;
        String tag = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_CHECK;
        String destination = topic + ":" + tag;

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ pattern check task sent to {}", destination);
    }

    /**
     * 发送离线渲染任务
     *
     * @param payload 任务负载对象
     */
    public void sendOfflineRenderTask(Object payload) {
        String destination = MessageRoutingStrategy.ROCKETMQ_AI_RENDER_TOPIC + ":render";

        Message<String> message = MessageBuilder
                .withPayload(JSON.toJSONString(payload))
                .build();

        rocketMQTemplate.syncSend(destination, message);
        log.info("RocketMQ offline render task sent to {}", destination);
    }

    /**
     * 异步发送批量纸样生成任务（不阻塞调用线程）
     *
     * @param payload 任务负载对象
     */
    public void sendBatchGenerateTaskAsync(Object payload) {
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
