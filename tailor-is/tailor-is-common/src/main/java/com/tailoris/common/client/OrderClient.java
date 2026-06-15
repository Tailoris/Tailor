package com.tailoris.common.client;

import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "tailor-is-order", path = "/api/order", fallbackFactory = OrderClientFallbackFactory.class)
public interface OrderClient {

    @GetMapping("/detail/{orderId}")
    Result<Map<String, Object>> getOrderDetail(@PathVariable Long orderId);
}