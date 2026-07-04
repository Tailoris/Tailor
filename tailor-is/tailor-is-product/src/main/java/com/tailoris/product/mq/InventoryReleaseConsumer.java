package com.tailoris.product.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.product.service.ProductSkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 库存释放消费者
 *
 * <p>当订单取消或超时未支付时，释放已预扣的库存。
 * 消息格式（JSON）：
 * <pre>
 * {
 *     "skuId": 456,
 *     "quantity": 10
 * }
 * </pre>
 * </p>
 */
@Slf4j
@Component
public class InventoryReleaseConsumer {

    public static final String INVENTORY_RELEASE_QUEUE = "inventory.release.queue";

    private final ProductSkuService productSkuService;
    private final ObjectMapper objectMapper;

    public InventoryReleaseConsumer(ProductSkuService productSkuService,
                                    ObjectMapper objectMapper) {
        this.productSkuService = productSkuService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = INVENTORY_RELEASE_QUEUE)
    public void handleInventoryRelease(String messageBody) {
        log.info("收到库存释放消息: {}", messageBody);

        try {
            Map<String, Object> message = objectMapper.readValue(messageBody,
                    new TypeReference<Map<String, Object>>() {});

            Object skuIdObj = message.get("skuId");
            Long skuId = skuIdObj != null
                    ? ((Number) skuIdObj).longValue() : null;

            Object quantityObj = message.get("quantity");
            Integer quantity = quantityObj != null
                    ? ((Number) quantityObj).intValue() : null;

            if (skuId == null || quantity == null || quantity <= 0) {
                log.warn("库存释放消息参数不完整: skuId={}, quantity={}", skuId, quantity);
                return;
            }

            log.info("开始释放库存: skuId={}, quantity={}", skuId, quantity);
            // increase=true 表示增加库存（释放）
            productSkuService.updateStock(skuId, quantity, true);
            log.info("库存释放完成: skuId={}, quantity={}", skuId, quantity);

        } catch (Exception e) {
            log.error("库存释放处理异常: message={}", messageBody, e);
            throw new RuntimeException("库存释放处理失败", e);
        }
    }
}