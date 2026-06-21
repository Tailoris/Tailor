package com.tailoris.order.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.dto.CreateOrderRequest;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.ShoppingCart;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderItemMapper;
import com.tailoris.order.mapper.ShoppingCartMapper;
import com.tailoris.order.service.ShoppingCartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
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
 * OrderServiceImpl 单元测试.
 *
 * <p>TODO 待补充测试场景：</p>
 * <ul>
 *   <li>T-M02: 订单超时自动取消测试</li>
 *   <li>T-M02: 售后申请/审核/退款流程测试</li>
 *   <li>T-M02: 批量订单操作测试</li>
 *   <li>T-M02: 订单搜索/筛选/分页查询测试</li>
 *   <li>T-M02: 分布式锁获取失败降级测试</li>
 *   <li>T-M02: 库存扣减回滚测试</li>
 * </ul>
 */
@DisplayName("OrderServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ShoppingCartMapper shoppingCartMapper;

    @Mock
    private ShoppingCartService shoppingCartService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private com.tailoris.order.service.InventoryService inventoryService;

    @Mock
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Mock
    private com.tailoris.common.lock.DistributedLock distributedLock;

    @Mock
    private com.tailoris.common.util.SpringSnowflakeIdGenerator snowflakeIdGenerator;

    @Mock
    private com.tailoris.common.client.SettlementClient settlementClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;

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
        lenient().when(stringRedisTemplate.delete(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("创建订单成功")
    void testCreateOrder_Success() {
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(Arrays.asList(1L, 2L));
        request.setRemark("请尽快发货");

        ShoppingCart cart1 = buildShoppingCart(1L, 100L, 200L, 1L, new BigDecimal("99.00"), 2);
        ShoppingCart cart2 = buildShoppingCart(2L, 101L, 200L, 1L, new BigDecimal("149.00"), 1);

        when(shoppingCartService.batchCheckout(eq(userId), eq(request.getCartIds())))
                .thenReturn(Arrays.asList(cart1, cart2));
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            OrderInfo o = invocation.getArgument(0);
            o.setId(1L);
            return 1;
        });
        when(orderItemMapper.insertBatchSomeColumn(anyList())).thenReturn(1);
        lenient().when(shoppingCartMapper.deleteById(anyLong())).thenReturn(1);

        try (MockedStatic<TransactionSynchronizationManager> mockedTSM = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTSM.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(inv -> null);

            String result = orderService.createOrder(userId, request);

            assertNotNull(result);
            assertTrue(result.startsWith("ORD"));
            String today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertTrue(result.contains(today),
                    "订单号应包含日期前缀 " + today + ", 实际: " + result);
            verify(orderInfoMapper).insert(any(OrderInfo.class));
            verify(orderItemMapper).insertBatchSomeColumn(anyList());
        }
    }

    @Test
    @DisplayName("🔒 B-L04修复验证: 订单号格式应为 ORD+yyyyMMdd+ID")
    void testOrderNoFormat_HasDatePrefix() {
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(Arrays.asList(1L));
        request.setRemark("测试");

        ShoppingCart cart1 = buildShoppingCart(1L, 100L, 200L, 1L, new BigDecimal("99.00"), 1);
        when(shoppingCartService.batchCheckout(eq(userId), eq(request.getCartIds())))
                .thenReturn(Collections.singletonList(cart1));
        when(orderInfoMapper.insert(any(OrderInfo.class))).thenAnswer(invocation -> {
            OrderInfo o = invocation.getArgument(0);
            o.setId(1L);
            return 1;
        });
        lenient().when(orderItemMapper.insertBatchSomeColumn(anyList())).thenReturn(1);
        lenient().when(shoppingCartMapper.deleteById(anyLong())).thenReturn(1);

        try (MockedStatic<TransactionSynchronizationManager> mockedTSM = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTSM.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(inv -> null);

            String orderNo = orderService.createOrder(userId, request);
            // 格式: ORD + 8位日期 + 雪花ID
            assertTrue(orderNo.matches("^ORD\\d{8}\\d+$"),
                    "订单号应匹配 ORD + 8位日期 + 数字, 实际: " + orderNo);
        }
    }

    @Test
    @DisplayName("支付订单成功")
    void testPayOrder_Success() {
        Long userId = 1L;
        String orderNo = "ORD123456789";
        Integer payType = 1;

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_PENDING_PAY);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        assertDoesNotThrow(() -> orderService.payOrder(userId, orderNo, payType));

        verify(orderInfoMapper).updateById((OrderInfo) argThat(o -> {
                OrderInfo oi = (OrderInfo) o;
                return oi.getStatus() == OrderConstants.ORDER_STATUS_PAID
                        && oi.getPayStatus() == OrderConstants.PAY_STATUS_PAID
                        && oi.getPayType() == payType;
        }));
    }

    @Test
    @DisplayName("取消订单成功")
    void testCancelOrder_Success() {
        Long userId = 1L;
        String orderNo = "ORD123456789";
        String reason = "不想要了";

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_PENDING_PAY);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        try (MockedStatic<TransactionSynchronizationManager> mockedTSM = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTSM.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(inv -> null);

            assertDoesNotThrow(() -> orderService.cancelOrder(userId, orderNo, reason));

            verify(orderInfoMapper).updateById((OrderInfo) argThat(o -> {
                    OrderInfo oi = (OrderInfo) o;
                    return oi.getStatus() == OrderConstants.ORDER_STATUS_CANCELLED
                            && reason.equals(oi.getCancelReason());
            }));
        }
    }

    @Test
    @DisplayName("发货成功")
    void testShipOrder_Success() {
        Long userId = 1L;
        String orderNo = "ORD123456789";
        String logisticsNo = "SF1234567890";

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_PAID);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        assertDoesNotThrow(() -> orderService.shipOrder(userId, orderNo, logisticsNo));

        verify(orderInfoMapper).updateById((OrderInfo) argThat(o -> {
                OrderInfo oi = (OrderInfo) o;
                return oi.getStatus() == OrderConstants.ORDER_STATUS_SHIPPED
                        && logisticsNo.equals(oi.getLogisticsNo());
        }));
    }

    @Test
    @DisplayName("确认收货成功")
    void testConfirmReceive_Success() {
        Long userId = 1L;
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_SHIPPED);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);
        lenient().when(settlementClient.settleOrder(anyLong(), anyLong(), anyLong(), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(com.tailoris.common.result.Result.success(true));

        try (MockedStatic<TransactionSynchronizationManager> mockedTSM = mockStatic(TransactionSynchronizationManager.class)) {
            mockedTSM.when(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)))
                    .thenAnswer(inv -> null);

            assertDoesNotThrow(() -> orderService.confirmReceive(userId, orderNo));

            verify(orderInfoMapper).updateById((OrderInfo) argThat(o -> {
                    OrderInfo oi = (OrderInfo) o;
                    return oi.getStatus() == OrderConstants.ORDER_STATUS_COMPLETED;
            }));
        }
    }

    @Test
    @DisplayName("获取订单详情成功")
    void testGetOrderDetail_Success() {
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(1L, orderNo, OrderConstants.ORDER_STATUS_COMPLETED);

        when(orderInfoMapper.selectOrderDetailWithItems(orderNo)).thenReturn(order);

        OrderInfo result = orderService.getOrderDetail(orderNo);

        assertNotNull(result);
        assertEquals(orderNo, result.getOrderNo());
    }

    @Test
    @DisplayName("获取订单详情失败-订单不存在")
    void testGetOrderDetail_NotFound() {
        String orderNo = "ORD_NOT_EXIST";

        when(orderInfoMapper.selectOrderDetailWithItems(orderNo)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.getOrderDetail(orderNo));
        assertEquals("订单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("取消订单失败-订单状态不允许")
    void testCancelOrder_StatusNotAllowed() {
        Long userId = 1L;
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_COMPLETED);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(userId, orderNo, "不想要了"));
        assertTrue(exception.getMessage().contains("状态"));
    }

    @Test
    @DisplayName("支付订单失败-订单不存在")
    void testPayOrder_OrderNotFound() {
        Long userId = 1L;
        String orderNo = "ORD_NOT_EXIST";

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.payOrder(userId, orderNo, 1));
        assertEquals("订单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建订单失败-购物车为空")
    void testCreateOrder_EmptyCart() {
        Long userId = 1L;
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartIds(Collections.emptyList());

        when(shoppingCartService.batchCheckout(eq(userId), eq(request.getCartIds())))
                .thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.createOrder(userId, request));
        assertTrue(exception.getMessage().contains("购物车"));
    }

    @Test
    @DisplayName("支付订单失败-重复支付")
    void testPayOrder_DuplicatePayment() {
        Long userId = 1L;
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_PAID);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        assertDoesNotThrow(() -> orderService.payOrder(userId, orderNo, 1));

        verify(orderInfoMapper, never()).updateById(any(OrderInfo.class));
    }

    @Test
    @DisplayName("支付订单失败-无权操作")
    void testPayOrder_NoPermission() {
        Long orderOwner = 1L;
        Long otherUser = 999L;
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(orderOwner, orderNo, OrderConstants.ORDER_STATUS_PENDING_PAY);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.payOrder(otherUser, orderNo, 1));
        assertTrue(exception.getMessage().contains("无权"));
    }

    @Test
    @DisplayName("取消订单失败-无权操作")
    void testCancelOrder_NoPermission() {
        Long orderOwner = 1L;
        Long otherUser = 999L;
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(orderOwner, orderNo, OrderConstants.ORDER_STATUS_PENDING_PAY);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(otherUser, orderNo, "不想要了"));
        assertTrue(exception.getMessage().contains("无权"));
    }

    @Test
    @DisplayName("确认收货失败-状态不允许")
    void testConfirmReceive_StatusNotAllowed() {
        Long userId = 1L;
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_PENDING_PAY);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.confirmReceive(userId, orderNo));
        assertTrue(exception.getMessage().contains("状态"));
    }

    @Test
    @DisplayName("发货失败-状态不允许")
    void testShipOrder_StatusNotAllowed() {
        Long userId = 1L;
        String orderNo = "ORD123456789";

        OrderInfo order = buildOrder(userId, orderNo, OrderConstants.ORDER_STATUS_PENDING_PAY);

        when(orderInfoMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class))).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderService.shipOrder(userId, orderNo, "SF12345"));
        assertTrue(exception.getMessage().contains("状态"));
    }

    private ShoppingCart buildShoppingCart(Long cartId, Long productId, Long shopId, Long merchantId,
                                           BigDecimal priceSnapshot, Integer quantity) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(cartId);
        cart.setProductId(productId);
        cart.setShopId(shopId);
        cart.setMerchantId(merchantId);
        cart.setPriceSnapshot(priceSnapshot);
        cart.setQuantity(quantity);
        cart.setUserId(1L);
        return cart;
    }

    private OrderInfo buildOrder(Long userId, String orderNo, Integer status) {
        OrderInfo order = new OrderInfo();
        order.setId(1L);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
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