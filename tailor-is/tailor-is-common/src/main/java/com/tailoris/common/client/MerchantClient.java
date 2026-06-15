package com.tailoris.common.client;

import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "tailor-is-merchant", path = "/api/merchant", fallbackFactory = MerchantClientFallbackFactory.class)
public interface MerchantClient {

    @GetMapping("/info/{merchantId}")
    Result<Map<String, Object>> getMerchantInfo(@PathVariable Long merchantId);
}