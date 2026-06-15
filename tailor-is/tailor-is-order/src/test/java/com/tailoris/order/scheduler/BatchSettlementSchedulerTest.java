package com.tailoris.order.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.client.SettlementClient;
import com.tailoris.common.result.Result;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.OrderInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BatchSettlementScheduler 测试")
@ExtendWith(MockitoExtension.class)
class BatchSettlementSchedulerTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private SettlementClient settlementClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BatchSettlementScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 500);
        ReflectionTestUtils.setField(scheduler, "platformFeeRate", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(scheduler, "lookbackDays", 7);
        ReflectionTestUtils.setField(scheduler, "settlementExchange", "settlement.batch.exchange");
        ReflectionTestUtils.setField(scheduler, "settlementRoutingKey", "settlement.batch");
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", true);
    }

    private OrderInfo buildOrder(Long orderId, Long merchantId, BigDecimal payAmount) {
        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setOrderNo("ORD" + orderId);
        order.setMerchantId(merchantId);
        order.setShopId(200L);
        order.setPayAmount(payAmount);
        order.setStatus(3);
        order.setPayStatus(1);
        order.setConfirmReceiveTime(LocalDateTime.now().minusDays(1));
        return order;
    }

    @Test
    @DisplayName("执行批量结算-调度器禁用")
    void testExecuteBatchSettlement_Disabled() {
        ReflectionTestUtils.setField(scheduler, "schedulerEnabled", false);

        scheduler.executeBatchSettlement();

        verify(orderInfoMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("执行批量结算-无待结算订单")
    void testExecuteBatchSettlement_NoOrders() {
        when(orderInfoMapper.selectList(any())).thenReturn(Collections.emptyList());

        scheduler.executeBatchSettlement();

        verify(settlementClient, never()).settleOrder(anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("执行批量结算-结算成功")
    void testExecuteBatchSettlement_Success() throws Exception {
        OrderInfo order1 = buildOrder(1L, 100L, new BigDecimal("100.00"));
        OrderInfo order2 = buildOrder(2L, 100L, new BigDecimal("200.00"));

        when(orderInfoMapper.selectList(any())).thenReturn(Arrays.asList(order1, order2));
        when(settlementClient.settleOrder(anyLong(), anyLong(), anyLong(), any(), any()))
                .thenReturn(Result.success(true));
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(orderInfoMapper.selectList(any())).thenReturn(Arrays.asList(order1, order2));
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        scheduler.executeBatchSettlement();

        verify(settlementClient, times(2)).settleOrder(anyLong(), anyLong(), anyLong(), any(), any());
        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("执行批量结算-结算失败")
    void testExecuteBatchSettlement_Failed() {
        OrderInfo order = buildOrder(1L, 100L, new BigDecimal("100.00"));

        when(orderInfoMapper.selectList(any())).thenReturn(Arrays.asList(order));
        when(settlementClient.settleOrder(anyLong(), anyLong(), anyLong(), any(), any()))
                .thenReturn(Result.fail("结算失败"));

        scheduler.executeBatchSettlement();

        verify(settlementClient).settleOrder(anyLong(), anyLong(), anyLong(), any(), any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("执行批量结算-结算服务不可用")
    void testExecuteBatchSettlement_ClientUnavailable() throws Exception {
        ReflectionTestUtils.setField(scheduler, "settlementClient", null);

        OrderInfo order = buildOrder(1L, 100L, new BigDecimal("100.00"));
        when(orderInfoMapper.selectList(any())).thenReturn(Arrays.asList(order));
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(orderInfoMapper.selectList(any())).thenReturn(Arrays.asList(order));
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        scheduler.executeBatchSettlement();

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("手动触发结算")
    void testTriggerManualSettlement() {
        when(orderInfoMapper.selectList(any())).thenReturn(Collections.emptyList());

        scheduler.triggerManualSettlement();

        verify(orderInfoMapper).selectList(any());
    }
}
