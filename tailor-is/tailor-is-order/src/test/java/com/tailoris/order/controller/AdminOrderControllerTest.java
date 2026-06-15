package com.tailoris.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.order.entity.AfterSaleTicket;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.AfterSaleTicketMapper;
import com.tailoris.order.mapper.OrderInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AdminOrderController 测试")
@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private AfterSaleTicketMapper afterSaleTicketMapper;

    @InjectMocks
    private AdminOrderController adminOrderController;

    private OrderInfo buildOrder(Long orderId, Integer status) {
        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setOrderNo("ORD" + orderId);
        order.setUserId(1L);
        order.setMerchantId(100L);
        order.setShopId(200L);
        order.setStatus(status);
        order.setPayAmount(new BigDecimal("100.00"));
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private AfterSaleTicket buildTicket(Long ticketId, Integer status) {
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setId(ticketId);
        ticket.setTicketNo("AS" + ticketId);
        ticket.setOrderId(1L);
        ticket.setStatus(status);
        ticket.setCreateTime(LocalDateTime.now());
        return ticket;
    }

    @Test
    @DisplayName("查询订单列表")
    void testListOrders() {
        Page<OrderInfo> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(buildOrder(1L, 1)));
        page.setTotal(1);

        when(orderInfoMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Result<Page<OrderInfo>> result = adminOrderController.listOrders(null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getRecords().size());
        verify(orderInfoMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询订单列表-带筛选条件")
    void testListOrders_WithFilters() {
        Page<OrderInfo> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(buildOrder(1L, 1)));

        when(orderInfoMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Result<Page<OrderInfo>> result = adminOrderController.listOrders(1, 1, "ORD", 100L, 1, 10);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(orderInfoMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取订单统计")
    void testGetOrderStats() {
        when(orderInfoMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

        Result<Map<String, Object>> result = adminOrderController.getOrderStats();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(10L, result.getData().get("totalOrders"));
        verify(orderInfoMapper, times(5)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询待处理售后工单")
    void testListPendingTickets() {
        Page<AfterSaleTicket> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(buildTicket(1L, 0)));
        page.setTotal(1);

        when(afterSaleTicketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        Result<Page<AfterSaleTicket>> result = adminOrderController.listPendingTickets(1, 10);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getRecords().size());
        verify(afterSaleTicketMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("仲裁售后工单-通过")
    void testArbitrateTicket_Approve() {
        AfterSaleTicket ticket = buildTicket(1L, 0);
        when(afterSaleTicketMapper.selectById(1L)).thenReturn(ticket);
        when(afterSaleTicketMapper.updateById(any(AfterSaleTicket.class))).thenReturn(1);

        Result<Void> result = adminOrderController.arbitrateTicket(1L, 1, "支持买家");

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(afterSaleTicketMapper).updateById(argThat((AfterSaleTicket t) -> {
            return t.getStatus() == 2 && t.getPlatformIntervene() == 1;
        }));
    }

    @Test
    @DisplayName("仲裁售后工单-拒绝")
    void testArbitrateTicket_Reject() {
        AfterSaleTicket ticket = buildTicket(1L, 0);
        when(afterSaleTicketMapper.selectById(1L)).thenReturn(ticket);
        when(afterSaleTicketMapper.updateById(any(AfterSaleTicket.class))).thenReturn(1);

        Result<Void> result = adminOrderController.arbitrateTicket(1L, 0, "支持卖家");

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(afterSaleTicketMapper).updateById(argThat((AfterSaleTicket t) -> {
            return t.getStatus() == 3 && t.getPlatformIntervene() == 1;
        }));
    }

    @Test
    @DisplayName("仲裁售后工单-工单不存在")
    void testArbitrateTicket_NotFound() {
        when(afterSaleTicketMapper.selectById(999L)).thenReturn(null);

        Result<Void> result = adminOrderController.arbitrateTicket(999L, 1, null);

        assertNotNull(result);
        assertEquals(500, result.getCode());
        verify(afterSaleTicketMapper, never()).updateById(any(AfterSaleTicket.class));
    }
}
