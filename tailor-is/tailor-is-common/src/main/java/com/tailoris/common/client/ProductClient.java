package com.tailoris.common.client;

import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "tailor-is-product", path = "/api/product", fallbackFactory = ProductClientFallbackFactory.class)
public interface ProductClient {

    @GetMapping("/detail/{productId}")
    Result<Map<String, Object>> getProductDetail(@PathVariable Long productId);

    @GetMapping("/stock/check")
    Result<Map<String, Object>> checkStock(@RequestParam Long skuId, @RequestParam Integer quantity);
}