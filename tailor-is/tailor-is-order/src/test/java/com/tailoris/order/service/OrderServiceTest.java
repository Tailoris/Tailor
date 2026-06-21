package com.tailoris.order.service;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.dto.CreateOrderRequest;
import com.tailoris.order.dto.OrderQueryRequest;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderItem;
import com.tailoris.order.entity.ShoppingCart;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderItemMapper;
import com.tailoris.order.mapper.ShoppingCartMapper;
import com.tailoris.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService 补充单元测试 - TEST-P2-01.
 *
 * <p>覆盖 OrderServiceImplTest 中 TODO 待补充的场景：</p>
 * <ul>
 *   <li>订单超时取消测试</li>
 *   <li>订单发货测试</li>
 *   <li>订单搜索/筛选/分页查询测试</li>
 *   <li>N+1 查询修复验证</li>
 *   <li>分布式锁获取失败降级测试</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService 补充单元测试")
class OrderServiceTest {

    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private ShoppingCartMapper shoppingCartMapper;
    @Mock private ShoppingCartService shoppingCartService;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private InventoryService inventoryService;
    @Mock private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    @Mock private com.tailoris.common.lock.DistributedLock distributedLock;
    @Mock private com.tailoris.common.util.SpringSnowflakeIdGenerator snowflakeIdGenerator;
    @Mock private com.tailoris.common.client.SettlementClient settlementClient;
    @Mock private org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(java.time.Duration.class))).thenReturn(true);
        lenient().when(distributedLock.tryLock(anyString(), anyLong(), any(java.util.concurrent.TimeUnit.class))).thenReturn("token");
        lenient().when(distributedLock.unlock(anyString(), anyString())).thenReturn(true);
        lenient().when(distributedLock.executeWithLock(anyString(), anyLong(), anyLong(), any(java.util.concurrent.TimeUnit.class), any(java.util.function.Supplier.class))).thenAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(4);
            return supplier.get();
        });
        lenient().when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);
        lenient().when(inventoryService.deductStock(anyList())).thenReturn(true);
        lenient().doNothing().when(inventoryService).confirmDeduct(anyList());
        lenient().doNothing().when(inventoryService).releaseStock(anyList());
    }

    // ============================================================
    // 订单超时取消
    // ============================================================

    @Test
    @DisplayName("取消订单 - 成功（模拟超时取消场景）")
    void cancelOrder_Success() {
        Long userId = 1L;
        String orderNo = "ORD001";
        OrderInfo order = buildOrder(1L, orderNo, OrderConstants.ORDER_STATUS_PENDING_PAY);
        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        try (MockedStatic<TransactionSynchronizationManager> mockedTSM = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTSM.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                    .thenAnswer(inv -> null);

            assertDoesNotThrow(() -> orderService.cancelOrder(userId, orderNo, "超时未支付"));
            verify(orderInfoMapper).updateById(any(OrderInfo.class));
        }
    }

    @Test
    @DisplayName("取消订单 - 订单不存在")
    void cancelOrder_NotFound() {
        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1L, "ORD_NOT_EXIST", "测试"));
    }

    @Test
    @DisplayName("取消订单 - 无权操作他人订单")
    void cancelOrder_NotOwner() {
        OrderInfo order = buildOrder(1L, "ORD001", OrderConstants.ORDER_STATUS_PENDING_PAY);
        order.setUserId(2L);
        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(1L, "ORD001", "测试"));
    }

    // ============================================================
    // 订单发货
    // ============================================================

    @Test
    @DisplayName("发货 - 成功")
    void shipOrder_Success() {
        Long merchantId = 100L;
        String orderNo = "ORD001";
        OrderInfo order = buildOrder(1L, orderNo, OrderConstants.ORDER_STATUS_PAID);
        order.setMerchantId(merchantId);
        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        orderService.shipOrder(merchantId, orderNo, "SF1234567890");

        verify(orderInfoMapper).updateById((OrderInfo) argThat(o ->
                o.getLogisticsNo() != null && o.getLogisticsNo().equals("SF1234567890")));
    }

    @Test
    @DisplayName("发货 - 非商家无权发货")
    void shipOrder_NotMerchant() {
        String orderNo = "ORD001";
        OrderInfo order = buildOrder(1L, orderNo, OrderConstants.ORDER_STATUS_PAID);
        order.setMerchantId(100L);
        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> orderService.shipOrder(999L, orderNo, "SF1234567890"));
    }

    // ============================================================
    // 订单搜索/筛选/分页查询
    // ============================================================

    @Test
    @DisplayName("查询订单列表 - 按状态筛选")
    void listOrders_ByStatus() {
        Long userId = 1L;
        OrderInfo order = buildOrder(1L, "ORD001", OrderConstants.ORDER_STATUS_PAID);
        OrderQueryRequest request = new OrderQueryRequest();
        request.setStatus(OrderConstants.ORDER_STATUS_PAID);
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(List.of(order));
        when(mockPage.getTotal()).thenReturn(1L);
        when(orderInfoMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(orderInfoMapper.selectOrderItemsByIds(anyList())).thenReturn(Collections.emptyList());
        when(orderInfoMapper.selectLogisticsByIds(anyList())).thenReturn(Collections.emptyList());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> result = orderService.listOrders(userId, request);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(OrderConstants.ORDER_STATUS_PAID, result.getRecords().get(0).getStatus());
    }

    @Test
    @DisplayName("查询订单列表 - 按关键词搜索")
    void listOrders_ByKeyword() {
        Long userId = 1L;
        OrderInfo order = buildOrder(1L, "ORD12345", OrderConstants.ORDER_STATUS_COMPLETED);
        OrderQueryRequest request = new OrderQueryRequest();
        request.setKeyword("ORD12345");
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(List.of(order));
        when(mockPage.getTotal()).thenReturn(1L);
        when(orderInfoMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(orderInfoMapper.selectOrderItemsByIds(anyList())).thenReturn(Collections.emptyList());
        when(orderInfoMapper.selectLogisticsByIds(anyList())).thenReturn(Collections.emptyList());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> result = orderService.listOrders(userId, request);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
    }

    @Test
    @DisplayName("查询订单列表 - 无结果")
    void listOrders_NoResults() {
        Long userId = 1L;
        OrderQueryRequest request = new OrderQueryRequest();
        request.setKeyword("NONEXISTENT");
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(Collections.emptyList());
        when(mockPage.getTotal()).thenReturn(0L);
        when(orderInfoMapper.selectPage(any(), any())).thenReturn(mockPage);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> result = orderService.listOrders(userId, request);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
    }

    // ============================================================
    // N+1 查询修复验证
    // ============================================================

    @Test
    @DisplayName("批量查询订单列表 - 使用批量填充避免N+1")
    void listOrders_BatchFillOrderItems() {
        Long userId = 1L;
        OrderInfo o1 = buildOrder(1L, "ORD001", OrderConstants.ORDER_STATUS_PAID);
        OrderInfo o2 = buildOrder(2L, "ORD002", OrderConstants.ORDER_STATUS_PAID);
        OrderInfo o3 = buildOrder(3L, "ORD003", OrderConstants.ORDER_STATUS_COMPLETED);
        OrderQueryRequest request = new OrderQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(Arrays.asList(o1, o2, o3));
        when(mockPage.getTotal()).thenReturn(3L);
        when(orderInfoMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(orderInfoMapper.selectOrderItemsByIds(anyList())).thenReturn(Collections.emptyList());
        when(orderInfoMapper.selectLogisticsByIds(anyList())).thenReturn(Collections.emptyList());

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> result = orderService.listOrders(userId, request);

        assertEquals(3, result.getRecords().size());
        // 验证使用批量查询而非逐条查询
        verify(orderInfoMapper, times(1)).selectOrderItemsByIds(anyList());
        verify(orderInfoMapper, times(1)).selectLogisticsByIds(anyList());
    }

    @Test
    @DisplayName("查询订单列表 - 空列表不触发批量查询")
    void listOrders_EmptyTriggersNoBatchQuery() {
        Long userId = 1L;
        OrderQueryRequest request = new OrderQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> mockPage = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
        when(mockPage.getRecords()).thenReturn(Collections.emptyList());
        when(mockPage.getTotal()).thenReturn(0L);
        when(orderInfoMapper.selectPage(any(), any())).thenReturn(mockPage);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OrderInfo> result = orderService.listOrders(userId, request);

        assertTrue(result.getRecords().isEmpty());
        verify(orderInfoMapper, never()).selectOrderItemsByIds(any());
    }

    // ============================================================
    // 分布式锁获取失败降级
    // ============================================================

    @Test
    @DisplayName("创建订单 - 分布式锁获取失败降级")
    void createOrder_LockFailed() {
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(List.of(1L));

        when(distributedLock.tryLock(anyString(), anyLong(), any(java.util.concurrent.TimeUnit.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, request));
        verify(orderInfoMapper, never()).insert(any(OrderInfo.class));
    }

    // ============================================================
    // 订单状态流转完整链路
    // ============================================================

    @Test
    @DisplayName("订单状态流转 - 创建→支付→发货→收货→完成")
    void orderStatusFlow_Complete() {
        assertTrue(OrderConstants.ORDER_STATUS_PENDING_PAY < OrderConstants.ORDER_STATUS_PAID);
        assertTrue(OrderConstants.ORDER_STATUS_PAID < OrderConstants.ORDER_STATUS_SHIPPED);
        assertTrue(OrderConstants.ORDER_STATUS_SHIPPED < OrderConstants.ORDER_STATUS_COMPLETED);
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private OrderInfo buildOrder(Long id, String orderNo, Integer status) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setUserId(1L);
        order.setShopId(200L);
        order.setMerchantId(1L);
        order.setProductType(1);
        order.setStatus(status);
        order.setPayStatus(OrderConstants.PAY_STATUS_UNPAID);
        order.setTotalAmount(new BigDecimal("347.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setCouponAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayAmount(new BigDecimal("347.00"));
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        return order;
    }
}