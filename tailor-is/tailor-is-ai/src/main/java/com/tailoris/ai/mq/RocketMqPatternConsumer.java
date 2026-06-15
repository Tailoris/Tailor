package com.tailoris.ai.mq;

import com.alibaba.fastjson2.JSON;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.service.PatternService;
import com.tailoris.common.mq.MessageRoutingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 纸样批量任务消费者
 *
 * <p>消费批量纸样生成任务，由 {@link RocketMqPatternProducer} 发送。</p>
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li>批量纸样生成：用户提交批量设计请求后，异步处理</li>
 *   <li>纸样迭代计算：复杂版型迭代需要大量计算资源</li>
 *   <li>离线纸样检查：对已有版型进行结构检查</li>
 * </ul>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC,
        consumerGroup = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_CONSUMER_GROUP,
        selectorExpression = "*",
        maxReconsumeTimes = 3
)
@RequiredArgsConstructor
public class RocketMqPatternConsumer implements RocketMQListener<String> {

    private final PatternService patternService;

    @Override
    public void onMessage(String message) {
        log.info("RocketMQ received batch pattern task: {}", message);
        try {
            // 解析消息体为 PatternGenerateRequest
            PatternGenerateRequest request = JSON.parseObject(message, PatternGenerateRequest.class);

            // 根据任务标签执行不同操作
            // 实际生产中可通过 message tag 区分任务类型
            if (request.getBodySizeId() != null && request.getPatternType() != null) {
                Long userId = request.getUserId();
                if (userId == null) {
                    log.warn("UserId is null for pattern task, skipping");
                    return;
                }
                patternService.generatePattern(userId, request);
                log.info("Batch pattern generated successfully for userId={}, bodySizeId={}",
                        userId, request.getBodySizeId());
            } else {
                log.warn("Invalid pattern task payload: missing bodySizeId or patternType");
            }
        } catch (Exception e) {
            log.error("Failed to process batch pattern task: {}", message, e);
            throw e; // 抛出异常触发 RocketMQ 重试机制
        }
    }
}
