package com.tailoris.message.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.message.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息推送消费者
 *
 * <p>从 MQ 消费消息推送任务，调用 MessageService 发送系统消息或模板消息。
 * 消息格式（JSON）：
 * <pre>
 * {
 *     "type": "system",
 *     "userId": 789,
 *     "title": "订单通知",
 *     "content": "您的订单已发货",
 *     "msgType": 1,
 *     "relatedType": "order",
 *     "relatedId": 1001,
 *     "businessNo": "ORD202401010001"
 * }
 * </pre>
 * </p>
 *
 * <p>模板消息格式（JSON）：
 * <pre>
 * {
 *     "type": "template",
 *     "userId": 789,
 *     "templateCode": "ORDER_SHIPPED",
 *     "params": {
 *         "orderNo": "ORD202401010001",
 *         "userName": "张三"
 *     }
 * }
 * </pre>
 * </p>
 *
 * <p>消息类型：1-系统通知，2-订单消息，3-营销消息，4-客服消息</p>
 */
@Slf4j
@Component
public class MessagePushConsumer {

    public static final String MESSAGE_PUSH_QUEUE = "message.push.queue";

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public MessagePushConsumer(MessageService messageService,
                               ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public Queue messagePushQueue() {
        return new Queue(MESSAGE_PUSH_QUEUE, true);
    }

    @RabbitListener(queues = MESSAGE_PUSH_QUEUE)
    public void handleMessagePush(String messageBody) {
        log.info("收到消息推送任务: {}", messageBody);

        try {
            Map<String, Object> message = objectMapper.readValue(messageBody,
                    new TypeReference<Map<String, Object>>() {});

            String type = (String) message.get("type");
            Object userIdObj = message.get("userId");
            Long userId = userIdObj != null
                    ? ((Number) userIdObj).longValue() : null;

            if (userId == null) {
                log.warn("消息推送参数不完整，缺少 userId: {}", messageBody);
                return;
            }

            if ("template".equalsIgnoreCase(type)) {
                // 模板消息推送
                String templateCode = (String) message.get("templateCode");
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) message.get("params");

                if (templateCode == null || templateCode.isEmpty()) {
                    log.warn("模板消息缺少 templateCode: {}", messageBody);
                    return;
                }

                messageService.sendTemplateMessage(userId, templateCode,
                        params != null ? params : Map.of());
                log.info("模板消息推送完成: userId={}, templateCode={}", userId, templateCode);

            } else {
                // 系统消息推送（默认）
                String title = (String) message.get("title");
                String content = (String) message.get("content");
                Object msgTypeObj = message.get("msgType");
                Integer msgType = msgTypeObj != null
                        ? ((Number) msgTypeObj).intValue() : 1;
                String relatedType = (String) message.get("relatedType");
                Object relatedIdObj = message.get("relatedId");
                Long relatedId = relatedIdObj != null
                        ? ((Number) relatedIdObj).longValue() : null;
                String businessNo = (String) message.get("businessNo");

                if (title == null || content == null) {
                    log.warn("系统消息缺少 title 或 content: {}", messageBody);
                    return;
                }

                messageService.sendSystemMessage(userId, title, content, msgType,
                        relatedType, relatedId, businessNo);
                log.info("系统消息推送完成: userId={}, title={}, msgType={}", userId, title, msgType);
            }

            // TODO: 待接入真实推送通道（短信/App推送/邮件）
            // 当前仅通过 MessageService 写入系统消息到数据库（站内信），
            // 后续需接入阿里云短信、个推 App Push、邮件服务等外部推送通道

        } catch (Exception e) {
            log.error("消息推送处理异常: message={}", messageBody, e);
            throw new RuntimeException("消息推送处理失败", e);
        }
    }
}