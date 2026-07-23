package com.tailoris.admin.service.impl;

import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.ArbitrateRequest;
import com.tailoris.api.admin.dto.OrderQueryRequest;
import com.tailoris.admin.feign.OrderFeignClient;
import com.tailoris.admin.service.AdminOrderService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.api.order.entity.AfterSaleTicket;
import com.tailoris.api.order.entity.OrderInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderFeignClient orderFeignClient;

    @Override
    public PageResponse<OrderInfo> listAllOrders(OrderQueryRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageNum", request.getPageNum());
        params.put("pageSize", request.getPageSize());

        if (request.getStatus() != null) {
            params.put("status", request.getStatus());
        }
        if (request.getProductType() != null) {
            params.put("productType", request.getProductType());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            params.put("keyword", request.getKeyword());
        }

        Result<PageResponse<OrderInfo>> result = orderFeignClient.listAllOrders(params);
        if (!result.isSuccess()) {
            throw new BusinessException(result.getMessage());
        }
        return result.getData();
    }

    @Override
    public OrderInfo getOrderDetail(String orderNo) {
        Result<OrderInfo> result = orderFeignClient.getOrderDetail(orderNo);
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("订单不存在");
        }
        return result.getData();
    }

    @Override
    public void arbitrateDispute(ArbitrateRequest request, Long adminId) {
        Result<AfterSaleTicket> result = orderFeignClient.getAfterSaleTicket(request.getTicketId().toString());
        if (!result.isSuccess() || result.getData() == null) {
            throw new BusinessException("售后工单不存在");
        }

        AfterSaleTicket ticket = result.getData();

        Map<String, Object> processRequest = new HashMap<>();
        processRequest.put("result", request.getResult());
        processRequest.put("remark", request.getRemark());

        Result<Void> processResult = orderFeignClient.processAfterSaleTicket(request.getTicketId().toString(), processRequest);
        if (!processResult.isSuccess()) {
            throw new BusinessException(processResult.getMessage());
        }

        log.info("平台仲裁完成, ticketId: {}, result: {}", request.getTicketId(), request.getResult());
    }
}