package com.tailoris.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.OrderInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OrderStatusSyncConsumer 测试")
@ExtendWith(MockitoExtension.class)
class OrderStatusSyncConsumerTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderStatusSyncConsumer orderStatusSyncConsumer;

    private static final String ORDER_NO = "ORD202606130001";

    private OrderInfo buildOrder(Integer status) {
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        order.setOrderNo(ORDER_NO);
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("100.00"));
        return order;
    }

    @Test
    @DisplayName("订单不存在 - 记录警告并返回")
    void handleOrderStatusSync_OrderNotFound_ShouldReturn() throws Exception {
        Map<String, Object> message = Map.of("orderNo", ORDER_NO, "newStatus", 2);
        String jsonMessage = "{\"orderNo\":\"ORD202606130001\",\"newStatus\":2}";

        when(objectMapper.readValue(eq(jsonMessage), any(TypeReference.class)))
                .thenReturn(message);
        when(orderInfoMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        orderStatusSyncConsumer.handleOrderStatusSync(jsonMessage);

        verify(orderInfoMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    @Test
    @DisplayName("订单存在 - 同步成功并更新状态")
    void handleOrderStatusSync_OrderExists_ShouldSync() throws Exception {
        OrderInfo order = buildOrder(1); // 待发货
        Map<String, Object> message = Map.of("orderNo", ORDER_NO, "newStatus", 2);
        String jsonMessage = "{\"orderNo\":\"ORD202606130001\",\"newStatus\":2}";

        when(objectMapper.readValue(eq(jsonMessage), any(TypeReference.class)))
                .thenReturn(message);
        when(orderInfoMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        orderStatusSyncConsumer.handleOrderStatusSync(jsonMessage);

        verify(orderInfoMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(orderInfoMapper).updateById(any(OrderInfo.class));
    }
}