package com.tailoris.order.mq;

import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderItem;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderItemMapper;
import com.tailoris.order.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OrderTimeoutConsumer 测试")
@ExtendWith(MockitoExtension.class)
class OrderTimeoutConsumerTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private OrderTimeoutConsumer orderTimeoutConsumer;

    private static final String ORDER_NO = "ORD202606130001";

    private OrderInfo buildOrder(Integer status, LocalDateTime expireTime) {
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        order.setOrderNo(ORDER_NO);
        order.setStatus(status);
        order.setExpireTime(expireTime);
        order.setTotalAmount(new BigDecimal("100.00"));
        return order;
    }

    @Test
    @DisplayName("订单不存在 - 直接返回")
    void handleOrderTimeout_OrderNotFound_ShouldReturn() {
        when(orderInfoMapper.selectOne(any())).thenReturn(null);

        orderTimeoutConsumer.handleOrderTimeout(ORDER_NO);

        verify(orderItemMapper, never()).selectList(any());
        verify(inventoryService, never()).releaseStock(any());
    }

    @Test
    @DisplayName("订单状态已变更(非待支付) - 无需取消")
    void handleOrderTimeout_StatusChanged_ShouldReturn() {
        OrderInfo order = buildOrder(OrderConstants.ORDER_STATUS_SHIPPED, null);
        when(orderInfoMapper.selectOne(any())).thenReturn(order);

        orderTimeoutConsumer.handleOrderTimeout(ORDER_NO);

        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    @Test
    @DisplayName("订单未超时(expireTime未到) - 无需取消")
    void handleOrderTimeout_NotExpired_ShouldReturn() {
        OrderInfo order = buildOrder(OrderConstants.ORDER_STATUS_PENDING_PAY,
                LocalDateTime.now().plusHours(1));
        when(orderInfoMapper.selectOne(any())).thenReturn(order);

        orderTimeoutConsumer.handleOrderTimeout(ORDER_NO);

        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    @Test
    @DisplayName("订单超时且过期 - 正常取消并释放库存")
    void handleOrderTimeout_Expired_ShouldCancel() {
        OrderInfo order = buildOrder(OrderConstants.ORDER_STATUS_PENDING_PAY,
                LocalDateTime.now().minusHours(1));
        when(orderInfoMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(new OrderItem()));

        orderTimeoutConsumer.handleOrderTimeout(ORDER_NO);

        verify(orderInfoMapper).updateById(any(OrderInfo.class));
        verify(inventoryService).releaseStock(anyList());
    }

    @Test
    @DisplayName("订单超时无订单项 - 取消但不释放库存")
    void handleOrderTimeout_ExpiredNoItems_ShouldCancelWithoutRelease() {
        OrderInfo order = buildOrder(OrderConstants.ORDER_STATUS_PENDING_PAY,
                LocalDateTime.now().minusHours(1));
        when(orderInfoMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        orderTimeoutConsumer.handleOrderTimeout(ORDER_NO);

        verify(orderInfoMapper).updateById(any(OrderInfo.class));
        verify(inventoryService, never()).releaseStock(any());
    }

    @Test
    @DisplayName("订单超时 expireTime 为 null - 直接取消")
    void handleOrderTimeout_NullExpireTime_ShouldCancel() {
        OrderInfo order = buildOrder(OrderConstants.ORDER_STATUS_PENDING_PAY, null);
        when(orderInfoMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());

        orderTimeoutConsumer.handleOrderTimeout(ORDER_NO);

        verify(orderInfoMapper).updateById(any(OrderInfo.class));
    }
}