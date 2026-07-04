package com.tailoris.payment.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.payment.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("SettlementProcessingConsumer 测试")
@ExtendWith(MockitoExtension.class)
class SettlementProcessingConsumerTest {

    @Mock
    private SettlementService settlementService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SettlementProcessingConsumer settlementProcessingConsumer;

    private static final Long MERCHANT_ID = 123L;
    private static final List<Long> ORDER_IDS = List.of(1001L, 1002L);

    @Test
    @DisplayName("结算处理 - 正常处理")
    void handleSettlementProcessing_ShouldProcess() throws Exception {
        Map<String, Object> message = Map.of("merchantId", MERCHANT_ID, "orderIds", ORDER_IDS);
        String jsonMessage = "{\"merchantId\":123,\"orderIds\":[1001,1002]}";

        when(objectMapper.readValue(eq(jsonMessage), any(TypeReference.class)))
                .thenReturn(message);

        settlementProcessingConsumer.handleSettlementProcessing(jsonMessage);

        verify(settlementService).batchSettle(MERCHANT_ID, ORDER_IDS);
    }

    @Test
    @DisplayName("结算处理 - 参数不完整应跳过")
    void handleSettlementProcessing_MissingParams_ShouldReturn() throws Exception {
        Map<String, Object> message = Map.of("merchantId", MERCHANT_ID);
        String jsonMessage = "{\"merchantId\":123}";

        when(objectMapper.readValue(eq(jsonMessage), any(TypeReference.class)))
                .thenReturn(message);

        settlementProcessingConsumer.handleSettlementProcessing(jsonMessage);

        verify(settlementService, never()).batchSettle(anyLong(), anyList());
    }
}