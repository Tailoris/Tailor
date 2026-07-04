package com.tailoris.order.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.OrderInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 订单状态同步消费者
 *
 * <p>当订单状态变更时，同步到数据库并通知下游服务。
 * 消息格式（JSON）：
 * <pre>
 * {
 *     "orderNo": "ORD202401010001",
 *     "newStatus": 2
 * }
 * </pre>
 * </p>
 *
 * <p>状态值参考：0-待支付，1-待发货，2-已发货，3-已完成，4-已取消，5-退款中</p>
 */
@Slf4j
@Component
public class OrderStatusSyncConsumer {

    public static final String ORDER_STATUS_SYNC_QUEUE = "order.status.sync.queue";

    private final OrderInfoMapper orderInfoMapper;
    private final ObjectMapper objectMapper;

    public OrderStatusSyncConsumer(OrderInfoMapper orderInfoMapper,
                                   ObjectMapper objectMapper) {
        this.orderInfoMapper = orderInfoMapper;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = ORDER_STATUS_SYNC_QUEUE)
    public void handleOrderStatusSync(String messageBody) {
        log.info("收到订单状态同步消息: {}", messageBody);

        try {
            Map<String, Object> message = objectMapper.readValue(messageBody,
                    new TypeReference<Map<String, Object>>() {});

            String orderNo = (String) message.get("orderNo");
            Object newStatusObj = message.get("newStatus");
            Integer newStatus = newStatusObj != null
                    ? ((Number) newStatusObj).intValue() : null;

            if (orderNo == null || newStatus == null) {
                log.warn("订单状态同步消息参数不完整: orderNo={}, newStatus={}", orderNo, newStatus);
                return;
            }

            // 更新订单状态到数据库
            OrderInfo order = orderInfoMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderInfo>()
                            .eq(OrderInfo::getOrderNo, orderNo));

            if (order == null) {
                log.warn("订单不存在，无法同步状态: orderNo={}", orderNo);
                return;
            }

            Integer oldStatus = order.getStatus();
            order.setStatus(newStatus);
            order.setUpdateTime(LocalDateTime.now());
            orderInfoMapper.updateById(order);

            log.info("订单状态同步完成: orderNo={}, oldStatus={}, newStatus={}", orderNo, oldStatus, newStatus);

            // TODO: 待接入下游 admin/analytics 服务，通过 Feign 客户端同步订单状态变更
            // 目前 admin 和 analytics 服务尚未实现，状态变更仅更新本地数据库
            // 下游服务接入后，在此处调用相应的 Feign 客户端方法

        } catch (Exception e) {
            log.error("订单状态同步处理异常: message={}", messageBody, e);
            throw new RuntimeException("订单状态同步处理失败", e);
        }
    }
}