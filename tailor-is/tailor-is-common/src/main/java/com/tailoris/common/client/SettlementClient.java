package com.tailoris.common.client;

import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "tailor-is-payment", path = "/api/settlement", fallbackFactory = SettlementClientFallbackFactory.class)
public interface SettlementClient {

    @PostMapping("/order")
    Result<Boolean> settleOrder(@RequestParam Long orderId,
                                @RequestParam Long merchantId,
                                @RequestParam Long shopId,
                                @RequestParam BigDecimal orderAmount,
                                @RequestParam BigDecimal platformFeeRate);
}