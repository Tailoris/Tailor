package com.tailoris.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.order.dto.AfterSaleRequest;
import com.tailoris.order.dto.TicketProcessRequest;
import com.tailoris.order.entity.AfterSaleTicket;
import com.tailoris.order.service.AfterSaleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AfterSaleController 测试")
@ExtendWith(MockitoExtension.class)
class AfterSaleControllerTest {

    @Mock
    private AfterSaleService afterSaleService;

    @InjectMocks
    private AfterSaleController afterSaleController;

    @Test
    @DisplayName("创建售后工单成功")
    void testCreateTicket_Success() {
        Long userId = 1L;
        AfterSaleRequest request = new AfterSaleRequest();
        request.setOrderId(100L);
        request.setTicketType(1);
        request.setReason("商品质量问题");
        request.setRefundAmount(new BigDecimal("100.00"));

        String expectedTicketNo = "AS123456789";
        when(afterSaleService.createTicket(eq(userId), any(AfterSaleRequest.class))).thenReturn(expectedTicketNo);

        Result<String> result = afterSaleController.createTicket(userId, request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(expectedTicketNo, result.getData());
        verify(afterSaleService).createTicket(eq(userId), any(AfterSaleRequest.class));
    }

    @Test
    @DisplayName("获取售后工单详情成功")
    void testGetTicketDetail_Success() {
        String ticketNo = "AS123456789";
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setId(1L);
        ticket.setTicketNo(ticketNo);
        ticket.setStatus(0);

        when(afterSaleService.getTicketDetail(ticketNo)).thenReturn(ticket);

        Result<AfterSaleTicket> result = afterSaleController.getTicketDetail(ticketNo);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(ticketNo, result.getData().getTicketNo());
        verify(afterSaleService).getTicketDetail(ticketNo);
    }

    @Test
    @DisplayName("查询用户售后工单列表成功")
    void testListTickets_Success() {
        Long userId = 1L;
        Integer status = 0;
        int pageNum = 1;
        int pageSize = 10;

        Page<AfterSaleTicket> page = new Page<>(pageNum, pageSize);
        AfterSaleTicket ticket = new AfterSaleTicket();
        ticket.setId(1L);
        ticket.setTicketNo("AS123");
        page.setRecords(java.util.Arrays.asList(ticket));

        when(afterSaleService.listTicketsByUser(eq(userId), eq(status), eq(pageNum), eq(pageSize))).thenReturn(page);

        Result<Page<AfterSaleTicket>> result = afterSaleController.listTickets(userId, status, pageNum, pageSize);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getRecords().size());
        verify(afterSaleService).listTicketsByUser(eq(userId), eq(status), eq(pageNum), eq(pageSize));
    }

    @Test
    @DisplayName("处理售后工单成功")
    void testProcessTicket_Success() {
        Long userId = 100L;
        TicketProcessRequest request = new TicketProcessRequest();
        request.setTicketId(1L);
        request.setProcessResult(1);
        request.setProcessRemark("同意退款");

        doNothing().when(afterSaleService).processTicket(eq(userId), any(TicketProcessRequest.class));

        Result<Void> result = afterSaleController.processTicket(userId, request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(afterSaleService).processTicket(eq(userId), any(TicketProcessRequest.class));
    }
}
