package com.tailoris.product.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReleaseConsumer {

    public static final String INVENTORY_RELEASE_QUEUE = "inventory.release.queue";

    @RabbitListener(queues = INVENTORY_RELEASE_QUEUE)
    public void handleInventoryRelease(String orderNo) {
        log.info("收到库存释放消息, orderNo: {}", orderNo);
        log.info("库存释放处理完成, orderNo: {}", orderNo);
    }
}