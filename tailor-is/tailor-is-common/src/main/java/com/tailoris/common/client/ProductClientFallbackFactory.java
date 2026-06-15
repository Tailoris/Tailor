package com.tailoris.common.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public com.tailoris.common.result.Result<Map<String, Object>> getProductDetail(Long productId) {
                return com.tailoris.common.result.Result.fail("商品服务暂时不可用，请稍后重试");
            }

            @Override
            public com.tailoris.common.result.Result<Map<String, Object>> checkStock(Long skuId, Integer quantity) {
                return com.tailoris.common.result.Result.fail("商品服务暂时不可用，请稍后重试");
            }
        };
    }
}