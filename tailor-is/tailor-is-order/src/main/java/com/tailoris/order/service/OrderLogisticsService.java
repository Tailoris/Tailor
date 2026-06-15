package com.tailoris.order.service;

import com.tailoris.order.entity.OrderLogistics;

public interface OrderLogisticsService {

    void createLogistics(Long orderId, String orderNo, String logisticsCompany, String logisticsCompanyName, String logisticsNo);

    void updateLogistics(Long orderId, String logisticsCompany, String logisticsCompanyName, String logisticsNo);

    OrderLogistics getLogisticsByOrderId(Long orderId);

    Object trackLogistics(String logisticsNo);
}
