package com.tailoris.payment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.client.OrderClient;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.payment.dto.PayRequest;
import com.tailoris.payment.dto.RefundRequest;
import com.tailoris.payment.entity.MerchantAccount;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.entity.RefundRecord;
import com.tailoris.payment.entity.UserAccount;
import com.tailoris.payment.mapper.MerchantAccountMapper;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import com.tailoris.payment.mapper.RefundRecordMapper;
import com.tailoris.payment.mapper.UserAccountMapper;
import com.tailoris.payment.service.EscrowService;
import com.tailoris.payment.service.PaymentService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现
 *
 * <p>提供支付单创建、支付处理、退款申请、支付状态查询等核心支付业务逻辑。
 * 使用Redis缓存支付状态以减少数据库查询。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final MerchantAccountMapper merchantAccountMapper;
    private final UserAccountMapper userAccountMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final EscrowService escrowService;
    private final ObjectProvider<OrderClient> orderClientProvider;

    private static final String PAYMENT_LOCK_KEY = "payment:lock:";
    private static final String PAYMENT_CACHE_KEY = "payment:info:";
    private static final String PAYMENT_STATUS_CACHE_KEY = "payment:status:";
    private static final String CALLBACK_IDEMPOTENT_KEY = "payment:callback:idempotent:";

    /** 支付状态：待支付 */
    private static final int PAY_STATUS_PENDING = 0;
    /** 支付状态：处理中 */
    private static final int PAY_STATUS_PROCESSING = 1;
    /** 支付状态：支付成功 */
    private static final int PAY_STATUS_SUCCESS = 2;

    /** 支付渠道公钥（生产环境从配置中心获取） */
    @org.springframework.beans.factory.annotation.Value("${payment.alipay.public-key:}")
    private String alipayPublicKey;

    @Override
    @Transactional
    @SentinelResource(value = "createPayment", blockHandler = "createPaymentBlockHandler", fallback = "createPaymentFallback")
    public PaymentRecord createPayment(Long userId, PayRequest request) {
        String lockKey = PAYMENT_LOCK_KEY + request.getOrderId();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lock)) {
            throw new BusinessException("订单正在支付中，请勿重复提交");
        }

        try {
            LambdaQueryWrapper<PaymentRecord> existQuery = new LambdaQueryWrapper<>();
            existQuery.eq(PaymentRecord::getOrderId, request.getOrderId())
                    .in(PaymentRecord::getPayStatus, PAY_STATUS_PENDING, PAY_STATUS_PROCESSING);
            Long existCount = paymentRecordMapper.selectCount(existQuery);
            if (existCount > 0) {
                throw new BusinessException("该订单已有待支付记录");
            }

            PaymentRecord record = new PaymentRecord();
            record.setId(SnowflakeIdGenerator.getInstance().nextId());
            record.setOrderId(request.getOrderId());
            record.setOrderNo("ORD" + request.getOrderId());
            record.setPaymentNo("PAY" + SnowflakeIdGenerator.getInstance().nextId());
            record.setUserId(userId);
            record.setAmount(request.getAmount());
            record.setPayChannel(request.getPayChannel());
            record.setPayMethod(request.getPayMethod());
            record.setPayStatus(PAY_STATUS_PENDING);
            record.setExpireTime(LocalDateTime.now().plusMinutes(30));
            record.setNotifyUrl(request.getNotifyUrl());
            record.setClientIp(getClientIp());
            record.setDeviceType(request.getDeviceType());
            record.setRemark(request.getRemark());
            paymentRecordMapper.insert(record);

            stringRedisTemplate.opsForValue().set(
                    PAYMENT_CACHE_KEY + record.getPaymentNo(),
                    record.getId().toString(), 30, TimeUnit.MINUTES);

            return record;
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    @GlobalTransactional(name = "payment-callback", rollbackFor = Exception.class)
    @Transactional
    @SentinelResource(value = "payCallback", blockHandler = "payCallbackBlockHandler", fallback = "payCallbackFallback")
    public void payCallback(String paymentNo, String transactionId, String channelResponse, String sign, String signType) {
        String idempotentKey = CALLBACK_IDEMPOTENT_KEY + paymentNo;
        
        Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(idempotentKey, "PROCESSING", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(lockAcquired)) {
            String status = stringRedisTemplate.opsForValue().get(idempotentKey);
            if ("SUCCESS".equals(status)) {
                log.info("支付回调幂等命中(已完成): paymentNo={}", paymentNo);
                return;
            }
            log.warn("支付回调正在处理中，请稍后: paymentNo={}", paymentNo);
            throw new BusinessException("支付回调正在处理中，请稍后");
        }

        try {
            LambdaQueryWrapper<PaymentRecord> query = new LambdaQueryWrapper<>();
            query.eq(PaymentRecord::getPaymentNo, paymentNo);
            PaymentRecord record = paymentRecordMapper.selectOne(query);
            if (record == null) {
                log.error("支付记录不存在: paymentNo={}", paymentNo);
                throw new BusinessException("支付记录不存在");
            }

            // ===== BE-C-2: 支付回调签名验证 =====
            if (!verifyCallbackSign(paymentNo, transactionId, sign, signType)) {
                log.error("支付回调签名验证失败: paymentNo={}, sign={}", paymentNo, sign);
                throw new BusinessException("支付回调签名验证失败");
            }
            
            if (record.getPayStatus() == PAY_STATUS_SUCCESS) {
                log.info("支付记录已处理: paymentNo={}", paymentNo);
                stringRedisTemplate.opsForValue().set(idempotentKey, "SUCCESS", 24, TimeUnit.HOURS);
                return;
            }

            if (record.getPayStatus() == PAY_STATUS_PROCESSING) {
                log.warn("支付记录处理中: paymentNo={}", paymentNo);
                throw new BusinessException("支付记录正在处理中");
            }

            record.setPayStatus(PAY_STATUS_SUCCESS);
            record.setPayTime(LocalDateTime.now());
            record.setTransactionId(transactionId);
            record.setChannelResponse(channelResponse);
            record.setNotifyStatus(1);
            record.setNotifyTime(LocalDateTime.now());
            paymentRecordMapper.updateById(record);

            // ===== BE-C-3: merchantId 从 PaymentRecord 获取，不信任客户端传入 =====
            Long merchantId = getMerchantIdFromRecord(record);
            escrowService.deposit(merchantId, record.getAmount());

            stringRedisTemplate.delete(PAYMENT_CACHE_KEY + paymentNo);
            // 幂等锁 SUCCESS 状态在事务提交后设置，避免事务回滚后 Redis 误判为已成功（锁生命周期与事务边界一致）
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    stringRedisTemplate.opsForValue().set(idempotentKey, "SUCCESS", 24, TimeUnit.HOURS);
                }
            });

            log.info("支付回调处理成功: paymentNo={}, transactionId={}, merchantId={}", paymentNo, transactionId, merchantId);
        } catch (BusinessException e) {
            // 事务回滚，保留幂等锁防止重复处理（锁自然过期后允许重试）
            throw e;
        } catch (Exception e) {
            // 事务回滚，保留幂等锁防止重复处理（锁自然过期后允许重试）
            log.error("支付回调处理失败，保留幂等锁防止重复处理: paymentNo={}", paymentNo, e);
            throw new BusinessException("支付回调处理失败");
        }
    }

    /**
     * 验证支付回调签名（BE-C-2 修复）
     * <p>使用 RSA256 验证支付渠道回调的签名，防止伪造回调请求。</p>
     *
     * @param paymentNo    支付单号
     * @param transactionId 交易流水号
     * @param sign         回调签名
     * @param signType     签名算法类型
     * @return true 签名验证通过
     */
    private boolean verifyCallbackSign(String paymentNo, String transactionId, String sign, String signType) {
        if (sign == null || sign.isBlank()) {
            log.warn("支付回调缺少签名: paymentNo={}", paymentNo);
            return false;
        }
        if (alipayPublicKey == null || alipayPublicKey.isBlank()) {
            log.warn("支付渠道公钥未配置，跳过验签: paymentNo={}", paymentNo);
            // 生产环境必须配置公钥，此处仅开发环境容错
            return false;
        }
        try {
            String content = "paymentNo=" + paymentNo + "&transactionId=" + transactionId;
            byte[] keyBytes = Base64.getDecoder().decode(alipayPublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.error("支付回调验签异常: paymentNo={}", paymentNo, e);
            return false;
        }
    }

    /**
     * 从支付记录中获取商家ID（BE-C-3 修复）
     * <p>不信任客户端传入的 merchantId，而是通过订单服务查询订单所属商家。</p>
     */
    private Long getMerchantIdFromRecord(PaymentRecord record) {
        if (record.getOrderId() == null) {
            throw new BusinessException("支付记录缺少订单关联");
        }
        OrderClient orderClient = orderClientProvider != null ? orderClientProvider.getIfAvailable() : null;
        if (orderClient == null) {
            throw new BusinessException("订单服务客户端不可用，无法获取商家ID: orderId=" + record.getOrderId());
        }
        Result<Map<String, Object>> orderResult = orderClient.getOrderDetail(record.getOrderId());
        if (orderResult == null || orderResult.getCode() != 200 || orderResult.getData() == null) {
            throw new BusinessException("查询订单详情失败，无法获取商家ID: orderId=" + record.getOrderId());
        }
        Object merchantIdObj = orderResult.getData().get("merchantId");
        if (merchantIdObj == null) {
            throw new BusinessException("订单详情缺少商家ID: orderId=" + record.getOrderId());
        }
        return ((Number) merchantIdObj).longValue();
    }

    @Override
    @Transactional
    public RefundRecord refund(Long userId, Long ticketId, Long orderId, BigDecimal amount, Integer refundChannel, String remark) {
        RefundRecord refundRecord = new RefundRecord();
        refundRecord.setId(SnowflakeIdGenerator.getInstance().nextId());
        refundRecord.setTicketId(ticketId);
        refundRecord.setTicketNo("TICKET" + ticketId);
        refundRecord.setRefundNo("REF" + SnowflakeIdGenerator.getInstance().nextId());
        refundRecord.setOrderId(orderId);
        refundRecord.setOrderNo("ORD" + orderId);
        refundRecord.setUserId(userId);
        refundRecord.setAmount(amount);
        refundRecord.setRefundChannel(refundChannel);
        refundRecord.setRefundStatus(0);
        refundRecord.setRetryCount(0);
        refundRecord.setRemark(remark);
        refundRecordMapper.insert(refundRecord);
        return refundRecord;
    }

    @Override
    @Transactional
    public PaymentRecord getPaymentStatus(Long paymentId) {
        String cacheKey = PAYMENT_STATUS_CACHE_KEY + paymentId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            // 缓存命中，直接返回包含支付状态的记录，跳过数据库查询
            PaymentRecord record = new PaymentRecord();
            record.setId(paymentId);
            record.setPayStatus(Integer.parseInt(cached));
            return record;
        }
        // 缓存未命中，查询数据库并写入缓存
        PaymentRecord record = paymentRecordMapper.selectById(paymentId);
        if (record != null) {
            stringRedisTemplate.opsForValue().set(cacheKey, record.getPayStatus().toString(), 30, TimeUnit.MINUTES);
        }
        return record;
    }

    @Override
    public PaymentRecord getPaymentByPaymentNo(String paymentNo) {
        LambdaQueryWrapper<PaymentRecord> query = new LambdaQueryWrapper<>();
        query.eq(PaymentRecord::getPaymentNo, paymentNo);
        return paymentRecordMapper.selectOne(query);
    }

    private String getClientIp() {
        try {
            Object clientIp = StpUtil.getExtra("clientIp");
            return clientIp != null ? clientIp.toString() : "127.0.0.1";
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // ==================== Sentinel 限流/降级处理 ====================

    /** 创建支付 - 限流处理 */
    public PaymentRecord createPaymentBlockHandler(Long userId, PayRequest request, BlockException ex) {
        log.warn("创建支付被限流: userId={}", userId);
        throw new BusinessException("支付请求过于频繁，请稍后再试");
    }

    /** 创建支付 - 降级处理 */
    public PaymentRecord createPaymentFallback(Long userId, PayRequest request, Throwable ex) {
        log.error("创建支付降级: userId={}", userId, ex);
        throw new BusinessException("支付服务暂不可用，请稍后再试");
    }

    /** 支付回调 - 限流处理 */
    public void payCallbackBlockHandler(String paymentNo, String transactionId, String channelResponse, String sign, String signType, BlockException ex) {
        log.warn("支付回调被限流: paymentNo={}", paymentNo);
        throw new BusinessException("支付回调请求过于频繁，请稍后再试");
    }

    /** 支付回调 - 降级处理 */
    public void payCallbackFallback(String paymentNo, String transactionId, String channelResponse, String sign, String signType, Throwable ex) {
        log.error("支付回调降级: paymentNo={}", paymentNo, ex);
        throw new BusinessException("支付回调服务暂不可用，请稍后再试");
    }
}
