package com.tailoris.common.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {

    @Override
    public OrderClient create(Throwable cause) {
        return orderId -> com.tailoris.common.result.Result.fail("订单服务暂时不可用，请稍后重试");
    }
}