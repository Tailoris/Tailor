package com.tailoris.order.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.client.SettlementClient;
import com.tailoris.common.mq.MessageRoutingStrategy;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.mapper.OrderInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批量结算调度器
 *
 * <p>将结算计算从实时模式迁移到定时批量处理模式，降低高峰期数据库压力。
 * 默认在凌晨 2:00-4:00 低峰时段运行，处理已完成订单的佣金和分账计算。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>查询最近 N 天状态为已完成（COMPLETED）且未结算的订单</li>
 *   <li>按商户分组，计算每个商户的佣金和分账金额</li>
 *   <li>通过 MQ 发送批量结算任务到支付服务（优先 RocketMQ，回退 RabbitMQ）</li>
 *   <li>更新订单结算状态，防止重复结算</li>
 * </ol>
 *
 * <h3>MQ 选型说明</h3>
 * <p>架构方案要求批量结算使用 RocketMQ 处理批量异步场景（高吞吐、事务消息、消息回溯），
 * 实时支付回调使用 RabbitMQ（低延迟、简单路由）。参考 MessageRoutingStrategy 路由策略。</p>
 * <p>当前实现: 如果 RocketMQ 依赖可用则优先使用 RocketMQ，否则回退到 RabbitMQ。</p>
 *
 * @author Tailor IS Team
 * @since 2.0.0
 */
@Slf4j
@Component
public class BatchSettlementScheduler {

    private final OrderInfoMapper orderInfoMapper;
    @Nullable
    private final SettlementClient settlementClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * RocketMQ 模板（可选依赖）.
     *
     * <p>架构方案要求批量结算使用 RocketMQ 处理批量异步场景。
     * 当 RocketMQ 依赖可用时优先使用，否则回退到 RabbitMQ。</p>
     */
    @Nullable
    private final RocketMQTemplate rocketMQTemplate;

    /** 是否启用 RocketMQ（由依赖是否可用决定） */
    private final boolean rocketMQAvailable;

    /** RocketMQ 批量结算 Topic */
    private static final String ROCKETMQ_SETTLEMENT_TOPIC = "settlement-batch-topic";
    /** RocketMQ 批量结算 Tag */
    private static final String ROCKETMQ_SETTLEMENT_TAG = "batch-settle";

    /** 批次大小：每批处理的订单数 */
    @Value("${tailoris.order.settlement.batch-size:500}")
    private int batchSize;

    /** 结算平台费率（默认 5%） */
    @Value("${tailoris.order.settlement.platform-fee-rate:0.05}")
    private BigDecimal platformFeeRate;

    /** 查询最近 N 天的已完成订单 */
    @Value("${tailoris.order.settlement.lookback-days:7}")
    private int lookbackDays;

    /** 批量结算任务 MQ Exchange */
    @Value("${tailoris.order.settlement.mq-exchange:settlement.batch.exchange}")
    private String settlementExchange;

    /** 批量结算任务 Routing Key */
    @Value("${tailoris.order.settlement.mq-routing-key:settlement.batch}")
    private String settlementRoutingKey;

    /** 是否启用批量结算调度 */
    @Value("${tailoris.order.settlement.scheduler-enabled:true}")
    private boolean schedulerEnabled;

    public BatchSettlementScheduler(OrderInfoMapper orderInfoMapper,
                                @Nullable SettlementClient settlementClient,
                                RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper,
                                @Nullable RocketMQTemplate rocketMQTemplate) {
    this.orderInfoMapper = orderInfoMapper;
    this.settlementClient = settlementClient;
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
    this.rocketMQTemplate = rocketMQTemplate;
    this.rocketMQAvailable = rocketMQTemplate != null;
    if (rocketMQAvailable) {
        log.info("批量结算 MQ 选型: RocketMQ（批量异步场景）");
    } else {
        log.info("批量结算 MQ 选型: RabbitMQ（RocketMQ 依赖不可用，回退到 RabbitMQ）");
    }
}

    /**
     * 定时批量结算任务
     *
     * <p>每天凌晨 2:00 执行（cron: 0 0 2 * * ?），在低峰期处理结算。</p>
     */
    @Scheduled(cron = "${tailoris.order.settlement.cron:0 0 2 * * ?}")
    public void executeBatchSettlement() {
        if (!schedulerEnabled) {
            log.info("批量结算调度器已禁用，跳过执行");
            return;
        }

        log.info("========== 开始执行批量结算任务 ==========");
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 1. 查询已完成但未结算的订单（状态 = 3, 最近 N 天）
            List<OrderInfo> completedOrders = queryCompletedOrders();
            if (completedOrders.isEmpty()) {
                log.info("未找到需要结算的订单");
                return;
            }

            log.info("查询到待结算订单数: {}", completedOrders.size());

            // 2. 按商户分组
            Map<Long, List<OrderInfo>> ordersByMerchant = completedOrders.stream()
                    .collect(Collectors.groupingBy(OrderInfo::getMerchantId));

            log.info("涉及商户数: {}", ordersByMerchant.size());

            int totalSettled = 0;
            int totalFailed = 0;

            // 3. 按批次处理每个商户的订单
            for (Map.Entry<Long, List<OrderInfo>> entry : ordersByMerchant.entrySet()) {
                Long merchantId = entry.getKey();
                List<OrderInfo> merchantOrders = entry.getValue();

                // 分批发送结算任务
                for (int i = 0; i < merchantOrders.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, merchantOrders.size());
                    List<OrderInfo> batch = merchantOrders.subList(i, end);

                    try {
                        boolean success = processSettlementBatch(merchantId, batch);
                        if (success) {
                            totalSettled += batch.size();
                        } else {
                            totalFailed += batch.size();
                        }
                    } catch (Exception e) {
                        log.error("商户结算批次处理异常: merchantId={}, batchSize={}",
                                merchantId, batch.size(), e);
                        totalFailed += batch.size();
                    }
                }
            }

            LocalDateTime endTime = LocalDateTime.now();
            log.info("========== 批量结算任务完成 ==========");
            log.info("耗时: {}ms, 结算成功: {}, 结算失败: {}",
                    java.time.Duration.between(startTime, endTime).toMillis(),
                    totalSettled, totalFailed);

        } catch (Exception e) {
            log.error("批量结算任务执行异常", e);
        }
    }

    /**
     * 处理一批商户订单的结算
     *
     * @param merchantId 商户 ID
     * @param orders     订单列表
     * @return true 如果全部成功
     */
    private boolean processSettlementBatch(Long merchantId, List<OrderInfo> orders) {
        List<Long> orderIds = new ArrayList<>();
        boolean allSuccess = true;

        for (OrderInfo order : orders) {
            try {
                if (settlementClient != null) {
                    // 调用结算服务进行结算
                    com.tailoris.common.result.Result<Boolean> result =
                            settlementClient.settleOrder(
                                    order.getId(),
                                    merchantId,
                                    order.getShopId(),
                                    order.getPayAmount(),
                                    platformFeeRate
                            );

                    if (result.getCode() == 200 && Boolean.TRUE.equals(result.getData())) {
                        // 发送 MQ 消息进行异步结算确认
                        sendSettlementMessage(order);
                        orderIds.add(order.getId());
                    } else {
                        log.error("订单结算失败: orderId={}, error={}", order.getId(), result.getMessage());
                        allSuccess = false;
                    }
                } else {
                    // 结算服务不可用时发送 MQ 消息
                    sendSettlementMessage(order);
                    orderIds.add(order.getId());
                    log.warn("结算服务不可用，已通过 MQ 发送结算任务: orderId={}", order.getId());
                }
            } catch (Exception e) {
                log.error("订单结算异常: orderId={}", order.getId(), e);
                allSuccess = false;
            }
        }

        // 更新已结算订单状态（标记为已结算）
        if (!orderIds.isEmpty()) {
            updateSettlementStatus(orderIds);
        }

        return allSuccess;
    }

    /**
     * 通过 MQ 发送结算任务消息.
     *
     * <p>MQ 选型: 优先使用 RocketMQ（批量异步场景），不可用时回退到 RabbitMQ。</p>
     */
    private void sendSettlementMessage(OrderInfo order) {
        try {
            String taskId = "BATCH_" + System.currentTimeMillis() + "_" + order.getId();

            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", order.getId());
            payload.put("orderNo", order.getOrderNo());
            payload.put("merchantId", order.getMerchantId());
            payload.put("shopId", order.getShopId());
            payload.put("orderAmount", order.getPayAmount());
            payload.put("platformFeeRate", platformFeeRate);
            payload.put("settlementTime", LocalDateTime.now().toString());
            payload.put("taskId", taskId);

            if (rocketMQAvailable && rocketMQTemplate != null) {
                sendViaRocketMQ(payload, taskId, order.getId());
            } else {
                sendViaRabbitMQ(payload, taskId, order.getId());
            }
        } catch (Exception e) {
            log.error("发送结算 MQ 消息失败: orderId={}", order.getId(), e);
        }
    }

    /**
     * 通过 RocketMQ 发送批量结算任务.
     *
     * <p>使用 syncSend 确保消息可靠投递，topic:tag 格式路由。</p>
     */
    private void sendViaRocketMQ(Map<String, Object> payload, String taskId, Long orderId) {
        try {
            String destination = ROCKETMQ_SETTLEMENT_TOPIC + ":" + ROCKETMQ_SETTLEMENT_TAG;
            String jsonPayload = objectMapper.writeValueAsString(payload);
            Message<String> message = MessageBuilder.withPayload(jsonPayload).build();
            rocketMQTemplate.syncSend(destination, message);
            log.debug("RocketMQ 发送结算消息: orderId={}, taskId={}, destination={}",
                    orderId, taskId, destination);
        } catch (Exception e) {
            log.error("RocketMQ 发送结算消息失败，回退到 RabbitMQ: orderId={}", orderId, e);
            // RocketMQ 发送失败时回退到 RabbitMQ
            sendViaRabbitMQ(payload, taskId, orderId);
        }
    }

    /**
     * 通过 RabbitMQ 发送批量结算任务（兜底方案）.
     */
    private void sendViaRabbitMQ(Map<String, Object> payload, String taskId, Long orderId) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(settlementExchange, settlementRoutingKey, jsonPayload);
            log.debug("RabbitMQ 发送结算消息: orderId={}, taskId={}", orderId, taskId);
        } catch (Exception e) {
            log.error("RabbitMQ 发送结算消息失败: orderId={}", orderId, e);
        }
    }

    /**
     * 更新订单结算状态
     */
    private void updateSettlementStatus(List<Long> orderIds) {
        try {
            LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(OrderInfo::getId, orderIds);
            List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
            for (OrderInfo order : orders) {
                // 使用 payStatus 字段标记结算状态（实际项目中建议增加 settlement_status 字段）
                order.setPayStatus(2); // 2 = 已结算
                orderInfoMapper.updateById(order);
            }
            log.info("更新结算状态: orderCount={}", orders.size());
        } catch (Exception e) {
            log.error("更新订单结算状态失败: orderIds={}", orderIds, e);
        }
    }

    /**
     * 查询已完成但未结算的订单
     *
     * <p>状态 = 3 (COMPLETED) 且 payStatus = 1 (已支付但未结算)，
     * 时间范围在最近 lookbackDays 天内。</p>
     */
    private List<OrderInfo> queryCompletedOrders() {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        // 状态 = COMPLETED (3)
        wrapper.eq(OrderInfo::getStatus, 3);
        // 支付状态 = PAID (1)，即已支付但未结算
        wrapper.eq(OrderInfo::getPayStatus, 1);
        // 时间范围
        wrapper.ge(OrderInfo::getConfirmReceiveTime,
                LocalDateTime.now().minusDays(lookbackDays));
        // 限制批次大小
        wrapper.last("LIMIT " + (batchSize * 10));

        return orderInfoMapper.selectList(wrapper);
    }

    /**
     * 手动触发批量结算（用于管理后台手动执行）
     */
    public void triggerManualSettlement() {
        log.info("手动触发批量结算");
        executeBatchSettlement();
    }
}
