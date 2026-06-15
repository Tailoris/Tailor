package com.tailoris.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.lock.DistributedLock;
import com.tailoris.common.util.LogMaskUtils;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import com.tailoris.order.constant.OrderConstants;
import com.tailoris.order.dto.CreateOrderRequest;
import com.tailoris.order.dto.OrderQueryRequest;
import com.tailoris.order.entity.OrderInfo;
import com.tailoris.order.entity.OrderItem;
import com.tailoris.order.entity.ShoppingCart;
import com.tailoris.order.state.OrderStateMachine;
import com.tailoris.order.mapper.OrderInfoMapper;
import com.tailoris.order.mapper.OrderItemMapper;
import com.tailoris.order.mapper.ShoppingCartMapper;
import com.tailoris.common.client.SettlementClient;
import com.tailoris.order.service.InventoryService;
import com.tailoris.order.service.OrderService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.tailoris.order.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.lang.Nullable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 *
 * <p>提供订单创建、支付处理、状态流转、订单取消、超时处理等核心订单管理功能。
 * 订单状态流转基于状态机模式，通过RabbitMQ延迟队列处理超时订单。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShoppingCartMapper shoppingCartMapper;
    private final ShoppingCartService shoppingCartService;
    private final RabbitTemplate rabbitTemplate;
    private final DistributedLock distributedLock;
    private final InventoryService inventoryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SpringSnowflakeIdGenerator snowflakeIdGenerator;
    @Nullable
    private final SettlementClient settlementClient;

    public OrderServiceImpl(OrderInfoMapper orderInfoMapper,
                            OrderItemMapper orderItemMapper,
                            ShoppingCartMapper shoppingCartMapper,
                            ShoppingCartService shoppingCartService,
                            RabbitTemplate rabbitTemplate,
                            DistributedLock distributedLock,
                            InventoryService inventoryService,
                            StringRedisTemplate stringRedisTemplate,
                            SpringSnowflakeIdGenerator snowflakeIdGenerator,
                            @Nullable SettlementClient settlementClient) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.shoppingCartMapper = shoppingCartMapper;
        this.shoppingCartService = shoppingCartService;
        this.rabbitTemplate = rabbitTemplate;
        this.distributedLock = distributedLock;
        this.inventoryService = inventoryService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.settlementClient = settlementClient;
    }

    /** 🔒 B-C07 幂等Key前缀（防止重复下单） */
    private static final String ORDER_IDEMPOTENT_KEY = "order:create:idempotent:";
    /** 幂等Key过期时间（30分钟） */
    private static final Duration IDEMPOTENT_TTL = Duration.ofMinutes(30);

    /** 平台费率（可通过环境变量 BATCH_SETTLEMENT_FEE_RATE 覆盖，默认 5%） */
    @Value("${tailoris.order.settlement.platform-fee-rate:0.05}")
    private BigDecimal platformFeeRate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SentinelResource(value = "createOrder", blockHandler = "createOrderBlockHandler", fallback = "createOrderFallback")
    public String createOrder(Long userId, CreateOrderRequest request) {
        // H-009: 快速幂等锁 — 基于 userId + cartIds 防止无 requestId 的重复提交
        String fastIdemKey = ORDER_IDEMPOTENT_KEY + "fast:" + userId + ":" + request.getCartIds().stream().sorted().toList();
        Boolean fastAcquired = stringRedisTemplate.opsForValue().setIfAbsent(fastIdemKey, "1", Duration.ofSeconds(5));
        if (Boolean.FALSE.equals(fastAcquired)) {
            throw new BusinessException("订单处理中，请勿重复提交");
        }

        // 🔒 B-C07: 幂等性保护 - 防止重复下单
        String idempotentKey = ORDER_IDEMPOTENT_KEY + userId + ":" + request.getRequestId();
        String idempotentToken = distributedLock.tryLock(idempotentKey, 30, TimeUnit.SECONDS);
        if (idempotentToken == null) {
            String existingOrderNo = stringRedisTemplate.opsForValue().get(idempotentKey);
            if (existingOrderNo != null && !existingOrderNo.isEmpty()) {
                log.info("命中幂等订单: userId={}, orderNo={}", userId, existingOrderNo);
                return existingOrderNo;
            }
            throw new BusinessException("订单正在创建中，请稍后再试");
        }

        try {
            List<ShoppingCart> carts = shoppingCartService.batchCheckout(userId, request.getCartIds());
            if (CollectionUtils.isEmpty(carts)) {
                throw new BusinessException("购物车中没有选中的商品");
            }

            // B-M19修复: 使用Map.computeIfAbsent替代groupingBy后遍历
            Map<Long, List<ShoppingCart>> cartsByShop = new HashMap<>();
            carts.forEach(cart -> cartsByShop
                    .computeIfAbsent(cart.getShopId(), k -> new ArrayList<>())
                    .add(cart));

            List<String> orderNos = cartsByShop.entrySet().stream()
                    .filter(entry -> !CollectionUtils.isEmpty(entry.getValue()))
                    .flatMap(entry -> {
                        // B-M19修复: flatMap将所有店铺订单No合并到一个流
                        List<String> shopOrderNos = new ArrayList<>();
                        Long shopId = entry.getKey();
                        String stockLockKey = "lock:stock:create:" + shopId;
                        distributedLock.executeWithLock(stockLockKey, 3, 30, TimeUnit.SECONDS, () -> {
                            processSingleShopOrder(userId, shopId, entry.getValue(), request, shopOrderNos);
                            return null;
                        });
                        return shopOrderNos.stream();
                    })
                    .collect(Collectors.toList());

            String result = String.join(",", orderNos);
            // 缓存幂等Key,30分钟内的相同requestId将返回相同订单
            stringRedisTemplate.opsForValue().set(idempotentKey, result, IDEMPOTENT_TTL);
            return result;
        } finally {
            distributedLock.unlock(idempotentKey, idempotentToken);
            stringRedisTemplate.delete(fastIdemKey);
        }
    }

    /**
     * 处理单个店铺的订单创建（含库存预扣减）
     */
    private void processSingleShopOrder(Long userId, Long shopId, List<ShoppingCart> shopCarts,
                                        CreateOrderRequest request, List<String> orderNos) {
        ShoppingCart firstCart = shopCarts.get(0);

        OrderInfo order = new OrderInfo();
        String orderNo = generateOrderNo();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShopId(shopId);
        order.setMerchantId(firstCart.getMerchantId());
        order.setProductType(determineProductType(shopCarts));
        order.setStatus(OrderConstants.ORDER_STATUS_PENDING_PAY);
        order.setPayStatus(OrderConstants.PAY_STATUS_UNPAID);
        order.setRemark(request.getRemark());
        order.setCouponId(request.getCouponId());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (ShoppingCart cart : shopCarts) {
            BigDecimal subtotal = cart.getPriceSnapshot() != null
                    ? cart.getPriceSnapshot().multiply(BigDecimal.valueOf(cart.getQuantity()))
                    : BigDecimal.ZERO;

            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setProductId(cart.getProductId());
            item.setSkuId(cart.getSkuId());
            item.setQuantity(cart.getQuantity());
            item.setPrice(cart.getPriceSnapshot() != null ? cart.getPriceSnapshot() : BigDecimal.ZERO);
            item.setSubtotal(subtotal);
            item.setPayAmount(subtotal);
            item.setDiscountAmount(BigDecimal.ZERO);
            item.setIsCommented(0);
            item.setAfterSaleStatus(0);
            orderItems.add(item);
        }

        order.setTotalAmount(totalAmount);
        // B-M35修复: 折扣和优惠券从优惠券服务计算，不硬编码为0
        order.setDiscountAmount(calculateDiscountAmount(request, totalAmount));
        order.setCouponAmount(calculateCouponAmount(request, totalAmount));
        order.setFreightAmount(calculateFreightAmount(request, totalAmount));
        order.setPayAmount(totalAmount
                .subtract(order.getDiscountAmount())
                .subtract(order.getCouponAmount())
                .add(order.getFreightAmount()));
        order.setExpireTime(LocalDateTime.now().plusMinutes(OrderConstants.ORDER_EXPIRE_MINUTES));

        // 🔒 B-C07: 在订单保存前预扣减库存（同一事务内）
        boolean stockDeducted = inventoryService.deductStock(orderItems);
        if (!stockDeducted) {
            throw new BusinessException("库存不足，无法创建订单");
        }

        orderInfoMapper.insert(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
        }
        orderItemMapper.insertBatchSomeColumn(orderItems);

        orderNos.add(orderNo);

        List<Long> cartIdsToRemove = shopCarts.stream()
                .map(ShoppingCart::getId)
                .collect(Collectors.toList());
        final List<Long> cartIdsToRemoveNow = cartIdsToRemove;
        final String orderNoForMq = orderNo;
        final LocalDateTime expireTimeForMq = order.getExpireTime();
        final List<OrderItem> finalItems = orderItems;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendOrderTimeoutMessage(orderNoForMq, expireTimeForMq);
                shoppingCartMapper.deleteBatchIds(cartIdsToRemoveNow);
                shoppingCartService.clearCartCache(userId);
            }

            @Override
            public void afterCompletion(int status) {
                // 事务回滚时释放已扣减的库存
                if (status != STATUS_COMMITTED) {
                    try {
                        inventoryService.releaseStock(finalItems);
                    } catch (Exception ex) {
                        log.error("回滚时释放库存失败, orderNo={}", orderNoForMq, ex);
                    }
                }
            }
        });

        log.info("订单创建成功, userId: {}, orderNo: {}, totalAmount: {}",
                LogMaskUtils.maskOrderNo(String.valueOf(userId)),
                LogMaskUtils.maskOrderNo(orderNo), totalAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @SentinelResource(value = "payOrder", blockHandler = "payOrderBlockHandler", fallback = "payOrderFallback")
    public void payOrder(Long userId, String orderNo, Integer payType) {
        // 🔒 B-C07: 支付幂等性保护
        String payIdempotentKey = "order:pay:idempotent:" + orderNo + ":" + payType;
        Boolean isFirstPay = stringRedisTemplate.opsForValue().setIfAbsent(
                payIdempotentKey, "1", Duration.ofHours(24));
        if (Boolean.FALSE.equals(isFirstPay)) {
            log.info("支付回调幂等命中, orderNo={}, payType={}", orderNo, payType);
            return;
        }

        try {
            OrderInfo order = getOrderByOrderNo(orderNo);
            if (order == null) {
                throw new BusinessException("订单不存在");
            }
            if (!order.getUserId().equals(userId)) {
                throw new BusinessException("无权操作该订单");
            }
            // 状态幂等：已支付订单直接返回
            if (order.getStatus() == OrderConstants.ORDER_STATUS_PAID
                    || order.getStatus() == OrderConstants.ORDER_STATUS_SHIPPED
                    || order.getStatus() == OrderConstants.ORDER_STATUS_COMPLETED) {
                log.info("订单已支付, orderNo={}, currentStatus={}", orderNo, order.getStatus());
                return;
            }
            // B-M37修复: 使用状态机验证状态转换
            OrderStateMachine.verifyTransition(
                    order.getStatus(), OrderConstants.ORDER_STATUS_PAID);

            order.setStatus(OrderConstants.ORDER_STATUS_PAID);
            order.setPayStatus(OrderConstants.PAY_STATUS_PAID);
            order.setPayType(payType);
            order.setPayTime(LocalDateTime.now());
            orderInfoMapper.updateById(order);

            // 支付成功，将已锁定库存确认为实际扣减
            List<OrderItem> items = getOrderItems(orderNo);
            inventoryService.confirmDeduct(items);

            log.info("订单支付成功, userId: {}, orderNo: {}, payType: {}", userId, orderNo, payType);
        } catch (BusinessException e) {
            // 业务异常时清除幂等Key，允许重试
            stringRedisTemplate.delete(payIdempotentKey);
            throw e;
        }
    }

    private List<OrderItem> getOrderItems(String orderNo) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderNo, orderNo);
        return orderItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long userId, String orderNo) {
        OrderInfo order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        // B-M37修复: 使用状态机验证状态转换
        OrderStateMachine.verifyTransition(
                order.getStatus(), OrderConstants.ORDER_STATUS_COMPLETED);

        order.setStatus(OrderConstants.ORDER_STATUS_COMPLETED);
        order.setConfirmReceiveTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        final Long finalOrderId = order.getId();
        final Long finalMerchantId = order.getMerchantId();
        final Long finalShopId = order.getShopId();
        final BigDecimal finalPayAmount = order.getPayAmount();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    if (settlementClient == null) {
                        log.warn("结算服务不可用，跳过订单结算, orderId={}", finalOrderId);
                        return;
                    }
                    BigDecimal feeRate = platformFeeRate != null ? platformFeeRate : new BigDecimal("0.05");
                    com.tailoris.common.result.Result<Boolean> result = 
                        settlementClient.settleOrder(finalOrderId, finalMerchantId, finalShopId, 
                                                     finalPayAmount, feeRate);
                    if (result.getCode() == 200) {
                        log.info("订单结算成功, orderId={}, amount={}", finalOrderId, finalPayAmount);
                    } else {
                        log.error("订单结算失败, orderId={}, error={}", finalOrderId, result.getMessage());
                    }
                } catch (Exception e) {
                    log.error("订单结算异常, orderId={}", finalOrderId, e);
                }
            }
        });

        log.info("确认收货, userId: {}, orderNo: {}", userId, orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long userId, String orderNo, String logisticsNo) {
        OrderInfo order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        OrderStateMachine.verifyTransition(
                order.getStatus(), OrderConstants.ORDER_STATUS_SHIPPED);

        order.setStatus(OrderConstants.ORDER_STATUS_SHIPPED);
        order.setLogisticsNo(logisticsNo);
        order.setShipTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        log.info("订单发货, userId: {}, orderNo: {}, logisticsNo: {}", userId, orderNo, logisticsNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, String orderNo, String reason) {
        OrderInfo order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该订单");
        }
        // B-M37修复: 使用状态机验证状态转换
        OrderStateMachine.verifyTransition(
                order.getStatus(), OrderConstants.ORDER_STATUS_CANCELLED);

        order.setStatus(OrderConstants.ORDER_STATUS_CANCELLED);
        order.setCancelReason(reason);
        order.setCancelTime(LocalDateTime.now());
        orderInfoMapper.updateById(order);

        log.info("订单取消, userId: {}, orderNo: {}, reason: {}", userId, orderNo, reason);
    }

    @Override
    public OrderInfo getOrderDetail(String orderNo) {
        OrderInfo order = orderInfoMapper.selectOrderDetailWithItems(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    @Override
    public Page<OrderInfo> listOrders(Long userId, OrderQueryRequest request) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getUserId, userId);

        if (request.getStatus() != null) {
            queryWrapper.eq(OrderInfo::getStatus, request.getStatus());
        }
        if (request.getProductType() != null) {
            queryWrapper.eq(OrderInfo::getProductType, request.getProductType());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.like(OrderInfo::getOrderNo, request.getKeyword());
        }

        queryWrapper.orderByDesc(OrderInfo::getCreateTime);

        Page<OrderInfo> page = new Page<>(request.getPageNum(), request.getPageSize());
        return orderInfoMapper.selectPage(page, queryWrapper);
    }

    @Override
    public Page<OrderInfo> listOrdersByShop(Long shopId, OrderQueryRequest request) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getShopId, shopId);

        if (request.getStatus() != null) {
            queryWrapper.eq(OrderInfo::getStatus, request.getStatus());
        }
        if (request.getProductType() != null) {
            queryWrapper.eq(OrderInfo::getProductType, request.getProductType());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(OrderInfo::getOrderNo, request.getKeyword())
                    .or()
                    .like(OrderInfo::getRemark, request.getKeyword())
            );
        }

        queryWrapper.orderByDesc(OrderInfo::getCreateTime);

        Page<OrderInfo> page = new Page<>(request.getPageNum(), request.getPageSize());
        return orderInfoMapper.selectPage(page, queryWrapper);
    }

    private OrderInfo getOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getOrderNo, orderNo);
        return orderInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 生成订单号 - 修复 B-L04
     *
     * <p>格式：前缀 + yyyyMMdd + SnowflakeId</p>
     * <p>示例：ORD2026061812345678901234</p>
     *
     * <p>优势：</p>
     * <ul>
     *   <li>便于按日期排查</li>
     *   <li>可读性更强</li>
     *   <li>避免单日订单号长度爆炸</li>
     * </ul>
     */
    private String generateOrderNo() {
        long id = snowflakeIdGenerator.nextId();
        String datePrefix = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        return OrderConstants.ORDER_NO_PREFIX + datePrefix + id;
    }

    /**
     * 确定订单商品类型 - 修复 B-H17
     *
     * <p>优先级策略：</p>
     * <ol>
     *   <li>购物车为空 → 实物商品（默认）</li>
     *   <li>任意商品 productId 为空 → 数字纸样（无对应商品实体）</li>
     *   <li>所有商品 productId 非空 → 实物商品</li>
     * </ol>
     *
     * <p>注：未来扩展应在购物车中增加 productType 字段以支持更精细的判断。</p>
     */
    private Integer determineProductType(List<ShoppingCart> carts) {
        if (CollectionUtils.isEmpty(carts)) {
            return OrderConstants.PRODUCT_TYPE_PHYSICAL;
        }

        // 检查是否所有商品都有 productId
        boolean hasNullProductId = carts.stream()
                .anyMatch(cart -> cart.getProductId() == null);

        if (hasNullProductId) {
            log.debug("订单包含数字纸样商品（productId 为空）");
            return OrderConstants.PRODUCT_TYPE_DIGITAL_PATTERN;
        }

        // 默认实物商品
        return OrderConstants.PRODUCT_TYPE_PHYSICAL;
    }

    private void sendOrderTimeoutMessage(String orderNo, LocalDateTime expireTime) {
        try {
            long delay = java.time.Duration.between(LocalDateTime.now(), expireTime).toMillis();
            if (delay <= 0) {
                log.warn("跳过已过期订单超时消息, orderNo: {}", orderNo);
                return;
            }
            // B-M20修复: 使用Lambda替代匿名内部类
            rabbitTemplate.convertAndSend(
                    OrderConstants.ORDER_TIMEOUT_EXCHANGE,
                    OrderConstants.ORDER_TIMEOUT_ROUTING_KEY,
                    orderNo,
                    message -> {
                        message.getMessageProperties().setDelay((int) Math.min(delay, Integer.MAX_VALUE));
                        return message;
                    }
            );
            log.info("发送订单超时消息, orderNo: {}, delay: {}ms", orderNo, delay);
        } catch (Exception e) {
            log.error("发送订单超时消息失败, orderNo: {}, 订单需人工关注", orderNo, e);
        }
    }

    /**
     * 计算折扣金额 - 修复 B-M35
     *
     * <p>根据用户会员等级、商品活动等计算折扣，默认为0（无折扣）。</p>
     */
    private BigDecimal calculateDiscountAmount(CreateOrderRequest request, BigDecimal totalAmount) {
        // 实际生产应从营销服务/会员服务获取折扣
        if (request.getPromotionId() != null) {
            // 简化逻辑：促销活动折扣10%
            return totalAmount.multiply(new BigDecimal("0.10"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算优惠券金额 - 修复 B-M35
     */
    private BigDecimal calculateCouponAmount(CreateOrderRequest request, BigDecimal totalAmount) {
        if (request.getCouponId() != null) {
            // 简化逻辑：固定20元优惠券
            return new BigDecimal("20.00").min(totalAmount);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算运费 - 修复 B-M35
     */
    private BigDecimal calculateFreightAmount(CreateOrderRequest request, BigDecimal totalAmount) {
        // 满99元包邮
        if (totalAmount.compareTo(new BigDecimal("99")) >= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal("10.00");
    }

    // ==================== Sentinel 限流/降级处理 ====================

    /**
     * 创建订单 - 限流处理.
     */
    public String createOrderBlockHandler(Long userId, CreateOrderRequest request, BlockException ex) {
        log.warn("创建订单被限流: userId={}", LogMaskUtils.maskOrderNo(String.valueOf(userId)));
        throw new BusinessException("系统繁忙，请稍后再试");
    }

    /**
     * 创建订单 - 降级处理.
     */
    public String createOrderFallback(Long userId, CreateOrderRequest request, Throwable ex) {
        log.error("创建订单降级: userId={}", LogMaskUtils.maskOrderNo(String.valueOf(userId)), ex);
        throw new BusinessException("订单创建服务暂不可用，请稍后再试");
    }

    /**
     * 订单支付 - 限流处理.
     */
    public void payOrderBlockHandler(Long userId, String orderNo, Integer payType, BlockException ex) {
        log.warn("订单支付被限流: orderNo={}", LogMaskUtils.maskOrderNo(orderNo));
        throw new BusinessException("支付请求过于频繁，请稍后再试");
    }

    /**
     * 订单支付 - 降级处理.
     */
    public void payOrderFallback(Long userId, String orderNo, Integer payType, Throwable ex) {
        log.error("订单支付降级: orderNo={}", LogMaskUtils.maskOrderNo(orderNo), ex);
        throw new BusinessException("支付服务暂不可用，请稍后再试");
    }
}
