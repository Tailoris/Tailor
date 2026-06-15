package com.tailoris.common.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {

    @Override
    public PaymentClient create(Throwable cause) {
        return new PaymentClient() {
            @Override
            public com.tailoris.common.result.Result<Map<String, Object>> getPaymentStatus(Long paymentId) {
                return com.tailoris.common.result.Result.fail("支付服务暂时不可用，请稍后重试");
            }

            @Override
            public com.tailoris.common.result.Result<Map<String, Object>> createPayment(Long orderId, BigDecimal amount, String channel) {
                return com.tailoris.common.result.Result.fail("支付服务暂时不可用，请稍后重试");
            }
        };
    }
}