package com.tailoris.admin.feign;

import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.product.entity.Product;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "tailor-is-product")
public interface ProductFeignClient {

    @GetMapping("/api/v1/product/list")
    Result<PageResponse<Product>> listProducts(@RequestParam Map<String, Object> params);

    @GetMapping("/api/v1/product/{id}")
    Result<Product> getProductDetail(@PathVariable Long id);

    @PutMapping("/api/v1/product/{id}/status")
    Result<Void> updateProductStatus(@PathVariable Long id, @RequestParam Integer status);
}