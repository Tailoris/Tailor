package com.tailoris.order.controller;

import com.tailoris.common.result.Result;
import com.tailoris.order.dto.LogisticsUpdateRequest;
import com.tailoris.order.entity.OrderLogistics;
import com.tailoris.order.service.OrderLogisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("LogisticsController 测试")
@ExtendWith(MockitoExtension.class)
class LogisticsControllerTest {

    @Mock
    private OrderLogisticsService orderLogisticsService;

    @InjectMocks
    private LogisticsController logisticsController;

    @Test
    @DisplayName("更新物流信息成功")
    void testUpdateLogistics_Success() {
        Long orderId = 1L;
        LogisticsUpdateRequest request = new LogisticsUpdateRequest();
        request.setLogisticsCompany("SF");
        request.setLogisticsCompanyName("顺丰速运");
        request.setLogisticsNo("SF1234567890");

        doNothing().when(orderLogisticsService).updateLogistics(
            eq(orderId),
            eq(request.getLogisticsCompany()),
            eq(request.getLogisticsCompanyName()),
            eq(request.getLogisticsNo())
        );

        Result<Void> result = logisticsController.updateLogistics(orderId, request);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        verify(orderLogisticsService).updateLogistics(
            eq(orderId),
            eq(request.getLogisticsCompany()),
            eq(request.getLogisticsCompanyName()),
            eq(request.getLogisticsNo())
        );
    }

    @Test
    @DisplayName("获取物流信息成功")
    void testGetLogistics_Success() {
        Long orderId = 1L;

        OrderLogistics logistics = new OrderLogistics();
        logistics.setId(1L);
        logistics.setOrderId(orderId);
        logistics.setLogisticsCompany("SF");
        logistics.setLogisticsNo("SF1234567890");

        when(orderLogisticsService.getLogisticsByOrderId(orderId)).thenReturn(logistics);

        Result<OrderLogistics> result = logisticsController.getLogistics(orderId);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(orderId, result.getData().getOrderId());
        verify(orderLogisticsService).getLogisticsByOrderId(orderId);
    }

    @Test
    @DisplayName("物流轨迹查询成功")
    void testTrackLogistics_Success() {
        String logisticsNo = "SF1234567890";
        String trackInfo = "已发货，正在运输中";

        when(orderLogisticsService.trackLogistics(logisticsNo)).thenReturn(trackInfo);

        Result<Object> result = logisticsController.trackLogistics(logisticsNo);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(trackInfo, result.getData());
        verify(orderLogisticsService).trackLogistics(logisticsNo);
    }
}
