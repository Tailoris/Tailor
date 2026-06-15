package com.tailoris.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusSyncConsumer {

    private final OrderInfoMapper orderInfoMapper;

    public static final String ORDER_STATUS_SYNC_QUEUE = "order.status.sync.queue";

    @RabbitListener(queues = ORDER_STATUS_SYNC_QUEUE)
    public void handleOrderStatusSync(String orderNo) {
        log.info("收到订单状态同步消息, orderNo: {}", orderNo);

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo order = orderInfoMapper.selectOne(queryWrapper);

        if (order == null) {
            log.warn("订单不存在, orderNo: {}", orderNo);
            return;
        }

        log.info("订单状态同步完成, orderNo: {}, status: {}", orderNo, order.getStatus());
    }
}