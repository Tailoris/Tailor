package com.tailoris.payment.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementProcessingConsumer {

    public static final String SETTLEMENT_PROCESS_QUEUE = "settlement.process.queue";

    @RabbitListener(queues = SETTLEMENT_PROCESS_QUEUE)
    public void handleSettlementProcessing(String settlementId) {
        log.info("收到结算处理消息, settlementId: {}", settlementId);
        log.info("结算处理完成, settlementId: {}", settlementId);
    }
}