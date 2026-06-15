package com.tailoris.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.order.dto.CreateOrderRequest;
import com.tailoris.order.dto.OrderQueryRequest;
import com.tailoris.order.entity.OrderInfo;

import java.util.List;

public interface OrderService {

    String createOrder(Long userId, CreateOrderRequest request);

    void payOrder(Long userId, String orderNo, Integer payType);

    void confirmReceive(Long userId, String orderNo);

    void shipOrder(Long userId, String orderNo, String logisticsNo);

    void cancelOrder(Long userId, String orderNo, String reason);

    OrderInfo getOrderDetail(String orderNo);

    Page<OrderInfo> listOrders(Long userId, OrderQueryRequest request);

    Page<OrderInfo> listOrdersByShop(Long shopId, OrderQueryRequest request);
}
