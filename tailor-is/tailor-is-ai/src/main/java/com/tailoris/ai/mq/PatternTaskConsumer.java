package com.tailoris.ai.mq;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.ai.model.PatternTask;
import com.tailoris.common.mq.MessageRoutingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TOPIC,
        consumerGroup = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_CONSUMER_GROUP,
        selectorExpression = MessageRoutingStrategy.ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE,
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        maxReconsumeTimes = 3,
        consumeThreadNumber = 8
)
@RequiredArgsConstructor
public class PatternTaskConsumer implements RocketMQListener<String> {

    private final PatternRecordMapper patternRecordMapper;

    private final ExecutorService aiPatternTaskConsumerExecutor;

    @Override
    public void onMessage(String message) {
        log.info("PatternTaskConsumer received message: {}", message);
        try {
            PatternTask task = JSON.parseObject(message, PatternTask.class);
            if (task == null || task.getTaskId() == null) {
                log.warn("Invalid pattern task message, skipping");
                return;
            }

            aiPatternTaskConsumerExecutor.submit(() -> processTask(task));

        } catch (Exception e) {
            log.error("Failed to parse pattern task message: {}", message, e);
            throw e;
        }
    }

    private void processTask(PatternTask task) {
        log.info("Processing pattern task: taskId={}, userId={}, patternType={}",
                task.getTaskId(), task.getUserId(), task.getPatternType());
        try {
            task.setStatus("PROCESSING");
            task.setProgress(10);
            task.setUpdateTime(LocalDateTime.now());

            // 模拟AI纸样生成处理
            // 实际生产环境中，这里会调用 AiModelService 进行真实的模型推理
            Thread.sleep(500);

            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setUpdateTime(LocalDateTime.now());
            updateTaskStatus(task);

            log.info("Pattern task completed: taskId={}", task.getTaskId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleTaskFailure(task, "Task interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("Pattern task processing failed: taskId={}", task.getTaskId(), e);
            handleTaskFailure(task, "Processing failed: " + e.getMessage());
        }
    }

    private void handleTaskFailure(PatternTask task, String errorMsg) {
        task.setStatus("FAILED");
        task.setResult(errorMsg);
        task.setUpdateTime(LocalDateTime.now());
        updateTaskStatus(task);
        log.error("Pattern task failed: taskId={}, error={}", task.getTaskId(), errorMsg);
    }

    private void updateTaskStatus(PatternTask task) {
        LambdaUpdateWrapper<com.tailoris.ai.entity.PatternRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(com.tailoris.ai.entity.PatternRecord::getId, task.getTaskId())
               .set(com.tailoris.ai.entity.PatternRecord::getStatus, "COMPLETED".equals(task.getStatus()) ? 1 : 2);
        patternRecordMapper.update(null, wrapper);
    }
}