package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.ArbitrateRequest;
import com.tailoris.api.admin.dto.OrderQueryRequest;
import com.tailoris.admin.service.AdminOrderService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.order.entity.AfterSaleTicket;
import com.tailoris.api.order.entity.OrderInfo;
import com.tailoris.api.order.mapper.AfterSaleTicketMapper;
import com.tailoris.api.order.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final AfterSaleTicketMapper afterSaleTicketMapper;

    @Override
    public PageResponse<OrderInfo> listAllOrders(OrderQueryRequest request) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();

        if (request.getStatus() != null) {
            queryWrapper.eq(OrderInfo::getStatus, request.getStatus());
        }
        if (request.getProductType() != null) {
            queryWrapper.eq(OrderInfo::getProductType, request.getProductType());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.like(OrderInfo::getOrderNo, request.getKeyword());
        }

        queryWrapper.orderByDesc(OrderInfo::getCreateTime);

        Page<OrderInfo> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<OrderInfo> result = orderInfoMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public OrderInfo getOrderDetail(String orderNo) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);
        if (orderInfo == null) {
            throw new BusinessException("订单不存在");
        }
        return orderInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void arbitrateDispute(ArbitrateRequest request, Long adminId) {
        AfterSaleTicket ticket = afterSaleTicketMapper.selectById(request.getTicketId());
        if (ticket == null) {
            throw new BusinessException("售后工单不存在");
        }

        if (request.getResult() == 1) {
            ticket.setStatus(AdminConstants.AFTER_SALE_STATUS_COMPLETED);
        } else {
            ticket.setStatus(AdminConstants.AFTER_SALE_STATUS_REJECTED);
        }
        ticket.setPlatformIntervene(1);
        ticket.setPlatformResult(request.getRemark());
        ticket.setPlatformHandler(adminId);
        ticket.setPlatformHandleTime(LocalDateTime.now());
        ticket.setProcessedAt(LocalDateTime.now());

        afterSaleTicketMapper.updateById(ticket);

        log.info("平台仲裁完成, ticketId: {}, result: {}", request.getTicketId(), request.getResult());
    }
}
