package com.tailoris.payment.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.payment.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 批量结算处理消费者
 *
 * <p>消息格式（JSON）：
 * <pre>
 * {
 *     "merchantId": 123456,
 *     "orderIds": [1001, 1002, 1003]
 * }
 * </pre>
 * </p>
 */
@Slf4j
@Component
public class SettlementProcessingConsumer {

    public static final String SETTLEMENT_PROCESS_QUEUE = "settlement.process.queue";

    private final SettlementService settlementService;
    private final ObjectMapper objectMapper;

    public SettlementProcessingConsumer(SettlementService settlementService,
                                        ObjectMapper objectMapper) {
        this.settlementService = settlementService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = SETTLEMENT_PROCESS_QUEUE)
    public void handleSettlementProcessing(String messageBody) {
        log.info("收到结算处理消息: {}", messageBody);

        try {
            Map<String, Object> message = objectMapper.readValue(messageBody,
                    new TypeReference<Map<String, Object>>() {});

            Object merchantIdObj = message.get("merchantId");
            Long merchantId = merchantIdObj != null
                    ? ((Number) merchantIdObj).longValue() : null;

            @SuppressWarnings("unchecked")
            List<Object> orderIdsRaw = (List<Object>) message.get("orderIds");

            if (merchantId == null || orderIdsRaw == null || orderIdsRaw.isEmpty()) {
                log.warn("结算消息参数不完整: merchantId={}, orderIds={}", merchantId, orderIdsRaw);
                return;
            }

            List<Long> orderIds = orderIdsRaw.stream()
                    .map(obj -> ((Number) obj).longValue())
                    .toList();

            log.info("开始批量结算: merchantId={}, orderCount={}", merchantId, orderIds.size());
            settlementService.batchSettle(merchantId, orderIds);
            log.info("批量结算完成: merchantId={}, orderCount={}", merchantId, orderIds.size());

        } catch (Exception e) {
            log.error("批量结算处理异常: message={}", messageBody, e);
            throw new RuntimeException("批量结算处理失败", e);
        }
    }
}