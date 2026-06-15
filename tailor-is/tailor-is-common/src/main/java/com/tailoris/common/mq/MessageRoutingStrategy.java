package com.tailoris.common.mq;

/**
 * 消息队列路由策略
 *
 * <p>定义双MQ选型路由规则：</p>
 * <ul>
 *   <li>RabbitMQ: 核心实时场景 — 支付回调、订单处理、即时通知、低延迟要求</li>
 *   <li>RocketMQ: 批量异步场景 — AI纸样生成、离线渲染、大规模批量任务、事务消息</li>
 * </ul>
 *
 * <p>选型原则：</p>
 * <ol>
 *   <li>低延迟(毫秒级) + 简单路由 → RabbitMQ</li>
 *   <li>高吞吐 + 批量 + 异步 + 事务 → RocketMQ</li>
 *   <li>消息回溯 + 定时投递 + 顺序消费 → RocketMQ</li>
 * </ol>
 */
public final class MessageRoutingStrategy {

    private MessageRoutingStrategy() {
        // prevent instantiation
    }

    // ========== RabbitMQ (核心实时场景) ==========

    /** 支付回调队列 */
    public static final String RABBITMQ_PAYMENT_CALLBACK_QUEUE = "payment.callback.queue";
    /** 支付回调交换机 */
    public static final String RABBITMQ_PAYMENT_CALLBACK_EXCHANGE = "payment.callback.exchange";
    /** 支付回调路由键 */
    public static final String RABBITMQ_PAYMENT_CALLBACK_ROUTING_KEY = "payment.callback";

    /** 订单处理队列 */
    public static final String RABBITMQ_ORDER_PROCESS_QUEUE = "order.process.queue";
    /** 订单处理交换机 */
    public static final String RABBITMQ_ORDER_PROCESS_EXCHANGE = "order.process.exchange";
    /** 订单处理路由键 */
    public static final String RABBITMQ_ORDER_PROCESS_ROUTING_KEY = "order.process";

    /** 实时通知队列 */
    public static final String RABBITMQ_REALTIME_NOTIFY_QUEUE = "realtime.notify.queue";
    /** 实时通知交换机 */
    public static final String RABBITMQ_REALTIME_NOTIFY_EXCHANGE = "realtime.notify.exchange";
    /** 实时通知路由键 */
    public static final String RABBITMQ_REALTIME_NOTIFY_ROUTING_KEY = "realtime.notify";

    // ========== RocketMQ (批量异步场景) ==========

    /** AI纸样生成主题 */
    public static final String ROCKETMQ_AI_PATTERN_TOPIC = "ai-pattern-topic";
    /** AI纸样生成消费者组 */
    public static final String ROCKETMQ_AI_PATTERN_CONSUMER_GROUP = "ai-pattern-consumer-group";
    /** AI纸样生成标签 — 批量纸样生成 */
    public static final String ROCKETMQ_AI_PATTERN_TAG_BATCH_GENERATE = "batch-generate";
    /** AI纸样生成标签 — 纸样迭代计算 */
    public static final String ROCKETMQ_AI_PATTERN_TAG_ITERATE = "iterate";
    /** AI纸样生成标签 — 纸样检查 */
    public static final String ROCKETMQ_AI_PATTERN_TAG_CHECK = "check";

    /** AI离线渲染主题 */
    public static final String ROCKETMQ_AI_RENDER_TOPIC = "ai-render-topic";
    /** AI离线渲染消费者组 */
    public static final String ROCKETMQ_AI_RENDER_CONSUMER_GROUP = "ai-render-consumer-group";

    /** 批量数据处理主题 */
    public static final String ROCKETMQ_BATCH_DATA_TOPIC = "batch-data-topic";
    /** 批量数据处理消费者组 */
    public static final String ROCKETMQ_BATCH_DATA_CONSUMER_GROUP = "batch-data-consumer-group";

    // ========== 路由决策 ==========

    /**
     * 判断是否应该使用 RocketMQ（批量异步场景）
     */
    public static boolean useRocketMQ(SceneType scene) {
        return scene == SceneType.AI_BATCH_PATTERN
                || scene == SceneType.AI_OFFLINE_RENDER
                || scene == SceneType.AI_PATTERN_ITERATE
                || scene == SceneType.AI_PATTERN_CHECK
                || scene == SceneType.BATCH_DATA_PROCESS;
    }

    /**
     * 判断是否应该使用 RabbitMQ（核心实时场景）
     */
    public static boolean useRabbitMQ(SceneType scene) {
        return scene == SceneType.PAYMENT_CALLBACK
                || scene == SceneType.ORDER_PROCESS
                || scene == SceneType.REALTIME_NOTIFY;
    }

    /**
     * 消息场景枚举
     */
    public enum SceneType {
        // RabbitMQ 场景
        PAYMENT_CALLBACK,
        ORDER_PROCESS,
        REALTIME_NOTIFY,
        // RocketMQ 场景
        AI_BATCH_PATTERN,
        AI_OFFLINE_RENDER,
        AI_PATTERN_ITERATE,
        AI_PATTERN_CHECK,
        BATCH_DATA_PROCESS
    }
}
