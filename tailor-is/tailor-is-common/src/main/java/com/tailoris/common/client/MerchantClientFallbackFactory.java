package com.tailoris.common.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MerchantClientFallbackFactory implements FallbackFactory<MerchantClient> {

    @Override
    public MerchantClient create(Throwable cause) {
        return merchantId -> com.tailoris.common.result.Result.fail("商户服务暂时不可用，请稍后重试");
    }
}