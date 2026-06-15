package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.ArbitrateRequest;
import com.tailoris.api.admin.dto.OrderQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.api.order.entity.AfterSaleTicket;
import com.tailoris.api.order.entity.OrderInfo;

public interface AdminOrderService {

    PageResponse<OrderInfo> listAllOrders(OrderQueryRequest request);

    OrderInfo getOrderDetail(String orderNo);

    void arbitrateDispute(ArbitrateRequest request, Long adminId);
}
