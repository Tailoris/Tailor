package com.tailoris.admin.feign;

import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.order.entity.AfterSaleTicket;
import com.tailoris.api.order.entity.OrderInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "tailor-is-order")
public interface OrderFeignClient {

    @GetMapping("/api/v1/order/admin/list")
    Result<PageResponse<OrderInfo>> listAllOrders(@RequestParam Map<String, Object> params);

    @GetMapping("/api/v1/order/{orderNo}")
    Result<OrderInfo> getOrderDetail(@PathVariable String orderNo);

    @GetMapping("/api/v1/order/after-sale/list")
    Result<PageResponse<AfterSaleTicket>> listAfterSaleTickets(@RequestParam Map<String, Object> params);

    @GetMapping("/api/v1/order/after-sale/{ticketNo}")
    Result<AfterSaleTicket> getAfterSaleTicket(@PathVariable String ticketNo);

    @PutMapping("/api/v1/order/after-sale/{ticketNo}/process")
    Result<Void> processAfterSaleTicket(@PathVariable String ticketNo, @RequestBody Map<String, Object> request);
}