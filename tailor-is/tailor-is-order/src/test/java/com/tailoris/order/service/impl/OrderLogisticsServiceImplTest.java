package com.tailoris.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderLogistics;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderLogisticsMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("OrderLogisticsServiceImpl 测试")
@ExtendWith(MockitoExtension.class)
class OrderLogisticsServiceImplTest {

    @Mock
    private OrderLogisticsMapper orderLogisticsMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @InjectMocks
    private OrderLogisticsServiceImpl orderLogisticsService;

    private OrderLogistics buildLogistics(Long orderId, String logisticsNo, Integer status) {
        OrderLogistics logistics = new OrderLogistics();
        logistics.setId(1L);
        logistics.setOrderId(orderId);
        logistics.setOrderNo("ORD" + orderId);
        logistics.setLogisticsCompany("SF");
        logistics.setLogisticsCompanyName("顺丰速运");
        logistics.setLogisticsNo(logisticsNo);
        logistics.setStatus(status);
        logistics.setLogisticsInfo("已发货");
        return logistics;
    }

    private OrderInfo buildOrder(Long orderId, Integer status) {
        OrderInfo order = new OrderInfo();
        order.setId(orderId);
        order.setOrderNo("ORD" + orderId);
        order.setStatus(status);
        return order;
    }

    @Test
    @DisplayName("创建物流信息成功")
    void testCreateLogistics_Success() {
        Long orderId = 1L;
        String orderNo = "ORD1";
        String logisticsCompany = "SF";
        String logisticsCompanyName = "顺丰速运";
        String logisticsNo = "SF123456";

        when(orderLogisticsMapper.insert(any(OrderLogistics.class))).thenReturn(1);

        assertDoesNotThrow(() -> orderLogisticsService.createLogistics(orderId, orderNo, logisticsCompany, logisticsCompanyName, logisticsNo));

        verify(orderLogisticsMapper).insert(argThat((OrderLogistics l) -> {
            return l.getOrderId().equals(orderId)
                    && l.getLogisticsNo().equals(logisticsNo)
                    && l.getStatus() == OrderConstants.LOGISTICS_STATUS_PENDING;
        }));
    }

    @Test
    @DisplayName("更新物流信息成功")
    void testUpdateLogistics_Success() {
        Long orderId = 1L;
        String logisticsCompany = "YTO";
        String logisticsCompanyName = "圆通速递";
        String logisticsNo = "YTO123456";

        OrderLogistics logistics = buildLogistics(orderId, "SF123456", OrderConstants.LOGISTICS_STATUS_PENDING);
        OrderInfo order = buildOrder(orderId, OrderConstants.ORDER_STATUS_PENDING_DELIVERY);

        when(orderLogisticsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(logistics);
        when(orderLogisticsMapper.updateById(any(OrderLogistics.class))).thenReturn(1);
        when(orderInfoMapper.selectById(orderId)).thenReturn(order);
        when(orderInfoMapper.updateById(any(OrderInfo.class))).thenReturn(1);

        assertDoesNotThrow(() -> orderLogisticsService.updateLogistics(orderId, logisticsCompany, logisticsCompanyName, logisticsNo));

        verify(orderLogisticsMapper).updateById(argThat((OrderLogistics l) -> {
            return l.getLogisticsNo().equals(logisticsNo)
                    && l.getStatus() == OrderConstants.LOGISTICS_STATUS_SHIPPED;
        }));
        verify(orderInfoMapper).updateById(argThat((OrderInfo o) -> {
            return o.getStatus() == OrderConstants.ORDER_STATUS_PENDING_RECEIVE;
        }));
    }

    @Test
    @DisplayName("更新物流信息失败-物流不存在")
    void testUpdateLogistics_NotFound() {
        Long orderId = 1L;
        when(orderLogisticsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderLogisticsService.updateLogistics(orderId, "YTO", "圆通", "YTO123"));
        assertEquals("物流信息不存在", exception.getMessage());
    }

    @Test
    @DisplayName("获取物流信息成功")
    void testGetLogisticsByOrderId_Success() {
        Long orderId = 1L;
        OrderLogistics logistics = buildLogistics(orderId, "SF123456", OrderConstants.LOGISTICS_STATUS_SHIPPED);

        when(orderLogisticsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(logistics);

        OrderLogistics result = orderLogisticsService.getLogisticsByOrderId(orderId);

        assertNotNull(result);
        assertEquals("SF123456", result.getLogisticsNo());
    }

    @Test
    @DisplayName("获取物流信息-不存在")
    void testGetLogisticsByOrderId_NotFound() {
        Long orderId = 1L;
        when(orderLogisticsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        OrderLogistics result = orderLogisticsService.getLogisticsByOrderId(orderId);

        assertNull(result);
    }

    @Test
    @DisplayName("物流轨迹查询成功")
    void testTrackLogistics_Success() {
        String logisticsNo = "SF123456";
        OrderLogistics logistics = buildLogistics(1L, logisticsNo, OrderConstants.LOGISTICS_STATUS_SHIPPED);
        logistics.setLogisticsInfo("已发货-运输中");

        when(orderLogisticsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(logistics);

        Object result = orderLogisticsService.trackLogistics(logisticsNo);

        assertNotNull(result);
        assertEquals("已发货-运输中", result);
    }

    @Test
    @DisplayName("物流轨迹查询失败-物流不存在")
    void testTrackLogistics_NotFound() {
        String logisticsNo = "NOT_EXIST";
        when(orderLogisticsMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> orderLogisticsService.trackLogistics(logisticsNo));
        assertEquals("物流信息不存在", exception.getMessage());
    }
}
