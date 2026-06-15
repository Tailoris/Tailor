package com.tailoris.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.dto.AfterSaleRequest;
import com.tailoris.order.dto.TicketProcessRequest;
import com.tailoris.order.entity.AfterSaleTicket;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.AfterSaleTicketMapper;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AfterSaleServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class AfterSaleServiceImplTest {

    @Mock
    private AfterSaleTicketMapper afterSaleTicketMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AfterSaleServiceImpl afterSaleService;

    private OrderInfo buildOrder(Long orderId, Long userId, Integer status) {
        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setOrderNo("ORD" + orderId);
        order.setUserId(userId);
        order.setMerchantId(100L);
        order.setShopId(200L);
        order.setStatus(status);
        return order;
    }

    private AfterSaleTicket buildTicket(Long ticketId, Long merchantId, Integer status) {
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setId(ticketId);
        ticket.setTicketNo("AS" + ticketId);
        ticket.setOrderId(1L);
        ticket.setMerchantId(merchantId);
        ticket.setShopId(200L);
        ticket.setStatus(status);
        ticket.setRefundAmount(new BigDecimal("100.00"));
        return ticket;
    }

    @Test
    @DisplayName("创建售后工单成功")
    void testCreateTicket_Success() {
        Long userId = 1L;
        AfterSaleRequest request = new AfterSaleRequest();
        request.setOrderId(1L);
        request.setTicketType(OrderConstants.TICKET_TYPE_REFUND_ONLY);
        request.setReason("商品质量问题");
        request.setRefundAmount(new BigDecimal("100.00"));
        request.setRefundQuantity(1);

        OrderInfo order = buildOrder(1L, userId, OrderConstants.ORDER_STATUS_COMPLETED);
        when(orderInfoMapper.selectById(1L)).thenReturn(order);
        when(afterSaleTicketMapper.insert(any(AfterSaleTicket.class))).thenReturn(1);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        String ticketNo = afterSaleService.createTicket(userId, request);

        assertNotNull(ticketNo);
        assertTrue(ticketNo.startsWith(OrderConstants.TICKET_NO_PREFIX));
        verify(afterSaleTicketMapper).insert(any(AfterSaleTicket.class));
        verify(orderInfoMapper).updateById(any(OrderInfo.class));
    }

    @Test
    @DisplayName("创建售后工单失败-订单不存在")
    void testCreateTicket_OrderNotFound() {
        Long userId = 1L;
        AfterSaleRequest request = new AfterSaleRequest();
        request.setOrderId(999L);

        when(orderInfoMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.createTicket(userId, request));
        assertEquals("订单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建售后工单失败-无权操作")
    void testCreateTicket_NoPermission() {
        Long orderOwner = 1L;
        Long otherUser = 999L;
        AfterSaleRequest request = new AfterSaleRequest();
        request.setOrderId(1L);
        request.setTicketType(OrderConstants.TICKET_TYPE_REFUND_ONLY);
        request.setReason("商品质量问题");
        request.setRefundAmount(new BigDecimal("100.00"));

        OrderInfo order = buildOrder(1L, orderOwner, OrderConstants.ORDER_STATUS_COMPLETED);
        when(orderInfoMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.createTicket(otherUser, request));
        assertEquals("无权操作该订单", exception.getMessage());
    }

    @Test
    @DisplayName("创建售后工单失败-订单已取消")
    void testCreateTicket_OrderCancelled() {
        Long userId = 1L;
        AfterSaleRequest request = new AfterSaleRequest();
        request.setOrderId(1L);
        request.setTicketType(OrderConstants.TICKET_TYPE_REFUND_ONLY);
        request.setReason("商品质量问题");
        request.setRefundAmount(new BigDecimal("100.00"));

        OrderInfo order = buildOrder(1L, userId, OrderConstants.ORDER_STATUS_CANCELLED);
        when(orderInfoMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.createTicket(userId, request));
        assertEquals("订单已取消，无法申请售后", exception.getMessage());
    }

    @Test
    @DisplayName("处理售后工单-通过")
    void testProcessTicket_Approve() {
        Long userId = 100L;
        TicketProcessRequest request = new TicketProcessRequest();
        request.setTicketId(1L);
        request.setProcessResult(1);
        request.setProcessRemark("审核通过");

        AfterSaleTicket ticket = buildTicket(1L, userId, OrderConstants.TICKET_STATUS_PENDING);
        when(afterSaleTicketMapper.selectById(1L)).thenReturn(ticket);
        when(afterSaleTicketMapper.updateById(any(AfterSaleTicket.class))).thenReturn(1);

        assertDoesNotThrow(() -> afterSaleService.processTicket(userId, request));

        verify(afterSaleTicketMapper).updateById(argThat((AfterSaleTicket t) -> {
            return t.getStatus() == OrderConstants.TICKET_STATUS_COMPLETED
                    && "审核通过".equals(t.getMerchantRemark());
        }));
    }

    @Test
    @DisplayName("处理售后工单-拒绝")
    void testProcessTicket_Reject() {
        Long userId = 100L;
        TicketProcessRequest request = new TicketProcessRequest();
        request.setTicketId(1L);
        request.setProcessResult(0);
        request.setProcessRemark("不符合售后条件");

        AfterSaleTicket ticket = buildTicket(1L, userId, OrderConstants.TICKET_STATUS_PENDING);
        when(afterSaleTicketMapper.selectById(1L)).thenReturn(ticket);
        when(afterSaleTicketMapper.updateById(any(AfterSaleTicket.class))).thenReturn(1);

        assertDoesNotThrow(() -> afterSaleService.processTicket(userId, request));

        verify(afterSaleTicketMapper).updateById(argThat((AfterSaleTicket t) -> {
            return t.getStatus() == OrderConstants.TICKET_STATUS_REJECTED;
        }));
    }

    @Test
    @DisplayName("处理售后工单失败-工单不存在")
    void testProcessTicket_TicketNotFound() {
        TicketProcessRequest request = new TicketProcessRequest();
        request.setTicketId(999L);
        request.setProcessResult(1);

        when(afterSaleTicketMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.processTicket(100L, request));
        assertEquals("售后工单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("审批退款成功")
    void testApproveRefund_Success() {
        Long userId = 100L;
        Long ticketId = 1L;
        String remark = "同意退款";

        AfterSaleTicket ticket = buildTicket(ticketId, userId, OrderConstants.TICKET_STATUS_PENDING);
        OrderInfo order = buildOrder(1L, 1L, OrderConstants.ORDER_STATUS_REFUNDING);

        when(afterSaleTicketMapper.selectById(ticketId)).thenReturn(ticket);
        when(afterSaleTicketMapper.updateById(any(AfterSaleTicket.class))).thenReturn(1);
        when(orderInfoMapper.selectById(1L)).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        assertDoesNotThrow(() -> afterSaleService.approveRefund(userId, ticketId, remark));

        verify(afterSaleTicketMapper).updateById(argThat((AfterSaleTicket t) -> {
            return t.getStatus() == OrderConstants.TICKET_STATUS_COMPLETED
                    && remark.equals(t.getMerchantRemark());
        }));
        verify(orderInfoMapper).updateById(argThat((OrderInfo o) -> {
            return o.getStatus() == OrderConstants.ORDER_STATUS_REFUNDED;
        }));
    }

    @Test
    @DisplayName("审批退款失败-工单不存在")
    void testApproveRefund_TicketNotFound() {
        when(afterSaleTicketMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.approveRefund(100L, 999L, "同意"));
        assertEquals("售后工单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("审批退款失败-无权操作")
    void testApproveRefund_NoPermission() {
        Long ticketOwner = 100L;
        Long otherUser = 999L;
        AfterSaleTicket ticket = buildTicket(1L, ticketOwner, OrderConstants.TICKET_STATUS_PENDING);

        when(afterSaleTicketMapper.selectById(1L)).thenReturn(ticket);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.approveRefund(otherUser, 1L, "同意"));
        assertEquals("无权操作该售后工单", exception.getMessage());
    }

    @Test
    @DisplayName("审批退款失败-工单状态异常")
    void testApproveRefund_InvalidStatus() {
        Long userId = 100L;
        AfterSaleTicket ticket = buildTicket(1L, userId, OrderConstants.TICKET_STATUS_COMPLETED);

        when(afterSaleTicketMapper.selectById(1L)).thenReturn(ticket);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.approveRefund(userId, 1L, "同意"));
        assertEquals("售后工单状态异常", exception.getMessage());
    }

    @Test
    @DisplayName("拒绝工单成功")
    void testRejectTicket_Success() {
        Long userId = 100L;
        Long ticketId = 1L;
        String remark = "不符合售后条件";

        AfterSaleTicket ticket = buildTicket(ticketId, userId, OrderConstants.TICKET_STATUS_PENDING);
        OrderInfo order = buildOrder(1L, 1L, OrderConstants.ORDER_STATUS_REFUNDING);

        when(afterSaleTicketMapper.selectById(ticketId)).thenReturn(ticket);
        when(afterSaleTicketMapper.updateById(any(AfterSaleTicket.class))).thenReturn(1);
        when(orderInfoMapper.selectById(1L)).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        assertDoesNotThrow(() -> afterSaleService.rejectTicket(userId, ticketId, remark));

        verify(afterSaleTicketMapper).updateById(argThat((AfterSaleTicket t) -> {
            return t.getStatus() == OrderConstants.TICKET_STATUS_REJECTED;
        }));
        verify(orderInfoMapper).updateById(argThat((OrderInfo o) -> {
            return o.getStatus() == OrderConstants.ORDER_STATUS_COMPLETED;
        }));
    }

    @Test
    @DisplayName("拒绝工单失败-工单不存在")
    void testRejectTicket_TicketNotFound() {
        when(afterSaleTicketMapper.selectById(999L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.rejectTicket(100L, 999L, "拒绝"));
        assertEquals("售后工单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("拒绝工单失败-无权操作")
    void testRejectTicket_NoPermission() {
        Long ticketOwner = 100L;
        Long otherUser = 999L;
        AfterSaleTicket ticket = buildTicket(1L, ticketOwner, OrderConstants.TICKET_STATUS_PENDING);

        when(afterSaleTicketMapper.selectById(1L)).thenReturn(ticket);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.rejectTicket(otherUser, 1L, "拒绝"));
        assertEquals("无权操作该售后工单", exception.getMessage());
    }

    @Test
    @DisplayName("查询用户售后工单列表")
    void testListTicketsByUser() {
        Long userId = 1L;
        Integer status = OrderConstants.TICKET_STATUS_PENDING;
        int pageNum = 1;
        int pageSize = 10;

        Page<AfterSaleTicket> expectedPage = new Page<>(pageNum, pageSize);
        expectedPage.setRecords(Arrays.asList(buildTicket(1L, 100L, status)));

        when(afterSaleTicketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<AfterSaleTicket> result = afterSaleService.listTicketsByUser(userId, status, pageNum, pageSize);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(afterSaleTicketMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("查询商户售后工单列表")
    void testListTicketsByMerchant() {
        Long merchantId = 100L;
        Integer status = OrderConstants.TICKET_STATUS_PENDING;
        int pageNum = 1;
        int pageSize = 10;

        Page<AfterSaleTicket> expectedPage = new Page<>(pageNum, pageSize);
        expectedPage.setRecords(Arrays.asList(buildTicket(1L, merchantId, status)));

        when(afterSaleTicketMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        Page<AfterSaleTicket> result = afterSaleService.listTicketsByMerchant(merchantId, status, pageNum, pageSize);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(afterSaleTicketMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取工单详情成功")
    void testGetTicketDetail_Success() {
        String ticketNo = "AS123456";
        AfterSaleTicket ticket = buildTicket(1L, 100L, OrderConstants.TICKET_STATUS_PENDING);
        ticket.setTicketNo(ticketNo);

        when(afterSaleTicketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(ticket);

        AfterSaleTicket result = afterSaleService.getTicketDetail(ticketNo);

        assertNotNull(result);
        assertEquals(ticketNo, result.getTicketNo());
    }

    @Test
    @DisplayName("获取工单详情失败-工单不存在")
    void testGetTicketDetail_NotFound() {
        String ticketNo = "AS_NOT_EXIST";

        when(afterSaleTicketMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> afterSaleService.getTicketDetail(ticketNo));
        assertEquals("售后工单不存在", exception.getMessage());
    }
}
