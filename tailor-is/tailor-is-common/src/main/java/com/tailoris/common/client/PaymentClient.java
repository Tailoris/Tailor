package com.tailoris.common.client;

import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "tailor-is-payment", path = "/api/payment", fallbackFactory = PaymentClientFallbackFactory.class)
public interface PaymentClient {

    @GetMapping("/status")
    Result<Map<String, Object>> getPaymentStatus(@RequestParam Long paymentId);

    @PostMapping("/create")
    Result<Map<String, Object>> createPayment(@RequestParam Long orderId,
                                              @RequestParam BigDecimal amount,
                                              @RequestParam String channel);
}