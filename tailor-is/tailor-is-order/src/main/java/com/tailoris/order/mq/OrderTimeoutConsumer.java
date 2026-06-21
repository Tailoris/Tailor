package com.tailoris.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderItem;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderItemMapper;
import com.tailoris.order.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final InventoryService inventoryService;

    @RabbitListener(queues = OrderConstants.ORDER_TIMEOUT_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeout(String orderNo) {
        log.info("收到订单超时消息, orderNo: {}", orderNo);

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo order = orderInfoMapper.selectOne(queryWrapper);

        if (order == null) {
            log.warn("订单不存在, orderNo: {}", orderNo);
            return;
        }

        if (!Integer.valueOf(OrderConstants.ORDER_STATUS_PENDING_PAY).equals(order.getStatus())) {
            log.info("订单状态已变更，无需取消, orderNo: {}, status: {}", orderNo, order.getStatus());
            return;
        }

        if (order.getExpireTime() != null && LocalDateTime.now().isBefore(order.getExpireTime())) {
            log.info("订单未超时，无需取消, orderNo: {}, expireTime: {}", orderNo, order.getExpireTime());
            return;
        }

        order.setStatus(OrderConstants.ORDER_STATUS_CANCELLED);
        order.setCancelReason("订单超时未支付，系统自动取消");
        order.setCancelTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderNo, orderNo);
        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);

        if (!orderItems.isEmpty()) {
            inventoryService.releaseStock(orderItems);
            log.info("订单超时释放库存, orderNo: {}, items: {}", orderNo, orderItems.size());
        }

        log.info("订单超时自动取消, orderNo: {}", orderNo);
    }
}
