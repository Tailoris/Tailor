package com.tailoris.common.client;

import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class SettlementClientFallbackFactory implements FallbackFactory<SettlementClient> {

    @Override
    public SettlementClient create(Throwable cause) {
        log.error("结算服务调用失败: {}", cause.getMessage(), cause);
        return new SettlementClient() {
            @Override
            public Result<Boolean> settleOrder(Long orderId, Long merchantId, Long shopId, 
                                               BigDecimal orderAmount, BigDecimal platformFeeRate) {
                log.error("结算订单失败, orderId={}", orderId);
                return Result.fail("结算服务不可用，请稍后重试");
            }
        };
    }
}