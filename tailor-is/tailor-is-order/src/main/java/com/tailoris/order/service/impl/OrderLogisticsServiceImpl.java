package com.tailoris.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderLogistics;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderLogisticsMapper;
import com.tailoris.order.service.OrderLogisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLogisticsServiceImpl implements OrderLogisticsService {

    private final OrderLogisticsMapper orderLogisticsMapper;
    private final OrderInfoMapper orderInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createLogistics(Long orderId, String orderNo, String logisticsCompany, String logisticsCompanyName, String logisticsNo) {
        OrderLogistics logistics = new OrderLogistics();
        logistics.setOrderId(orderId);
        logistics.setOrderNo(orderNo);
        logistics.setLogisticsCompany(logisticsCompany);
        logistics.setLogisticsCompanyName(logisticsCompanyName);
        logistics.setLogisticsNo(logisticsNo);
        logistics.setStatus(OrderConstants.LOGISTICS_STATUS_PENDING);
        orderLogisticsMapper.insert(logistics);

        log.info("创建物流信息, orderId: {}, logisticsNo: {}", orderId, logisticsNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLogistics(Long orderId, String logisticsCompany, String logisticsCompanyName, String logisticsNo) {
        LambdaQueryWrapper<OrderLogistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderLogistics::getOrderId, orderId);
        OrderLogistics logistics = orderLogisticsMapper.selectOne(queryWrapper);

        if (logistics == null) {
            throw new BusinessException("物流信息不存在");
        }

        logistics.setLogisticsCompany(logisticsCompany);
        logistics.setLogisticsCompanyName(logisticsCompanyName);
        logistics.setLogisticsNo(logisticsNo);
        logistics.setStatus(OrderConstants.LOGISTICS_STATUS_SHIPPED);
        logistics.setShippedAt(LocalDateTime.now());
        orderLogisticsMapper.updateById(logistics);

        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order != null && order.getStatus() == OrderConstants.ORDER_STATUS_PENDING_DELIVERY) {
            order.setStatus(OrderConstants.ORDER_STATUS_PENDING_RECEIVE);
            orderInfoMapper.updateById(order);
        }

        log.info("更新物流信息, orderId: {}, logisticsNo: {}", orderId, logisticsNo);
    }

    @Override
    public OrderLogistics getLogisticsByOrderId(Long orderId) {
        LambdaQueryWrapper<OrderLogistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderLogistics::getOrderId, orderId)
                .orderByDesc(OrderLogistics::getCreateTime)
                .last("LIMIT 1");
        return orderLogisticsMapper.selectOne(queryWrapper);
    }

    @Override
    public Object trackLogistics(String logisticsNo) {
        LambdaQueryWrapper<OrderLogistics> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderLogistics::getLogisticsNo, logisticsNo);
        OrderLogistics logistics = orderLogisticsMapper.selectOne(queryWrapper);

        if (logistics == null) {
            throw new BusinessException("物流信息不存在");
        }

        return logistics.getLogisticsInfo();
    }
}
