package com.tailoris.payment.mq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("SettlementProcessingConsumer 测试")
@ExtendWith(MockitoExtension.class)
class SettlementProcessingConsumerTest {

    @InjectMocks
    private SettlementProcessingConsumer settlementProcessingConsumer;

    private static final String SETTLEMENT_ID = "STL202606130001";

    @Test
    @DisplayName("结算处理 - 正常处理")
    void handleSettlementProcessing_ShouldProcess() {
        settlementProcessingConsumer.handleSettlementProcessing(SETTLEMENT_ID);
    }
}