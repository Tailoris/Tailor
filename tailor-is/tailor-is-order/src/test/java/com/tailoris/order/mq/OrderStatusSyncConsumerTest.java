package com.tailoris.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.OrderInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("OrderStatusSyncConsumer 测试")
@ExtendWith(MockitoExtension.class)
class OrderStatusSyncConsumerTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

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
    void handleOrderStatusSync_OrderNotFound_ShouldReturn() {
        when(orderInfoMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        orderStatusSyncConsumer.handleOrderStatusSync(ORDER_NO);

        verify(orderInfoMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("订单存在 - 同步成功")
    void handleOrderStatusSync_OrderExists_ShouldSync() {
        OrderInfo order = buildOrder(2); // 已支付
        when(orderInfoMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        orderStatusSyncConsumer.handleOrderStatusSync(ORDER_NO);

        verify(orderInfoMapper).selectOne(any(LambdaQueryWrapper.class));
    }
}