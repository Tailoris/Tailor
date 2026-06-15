package com.tailoris.payment.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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

    private static final String PAYMENT_LOCK_KEY = "payment:lock:";
    private static final String PAYMENT_CACHE_KEY = "payment:info:";
    private static final String CALLBACK_IDEMPOTENT_KEY = "payment:callback:idempotent:";

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
                    .in(PaymentRecord::getPayStatus, 0, 1);
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
            record.setPayStatus(0);
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
    public void payCallback(String paymentNo, String transactionId, String channelResponse, Long merchantId) {
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
            
            if (record.getPayStatus() == 2) {
                log.info("支付记录已处理: paymentNo={}", paymentNo);
                stringRedisTemplate.opsForValue().set(idempotentKey, "SUCCESS", 24, TimeUnit.HOURS);
                return;
            }

            if (record.getPayStatus() == 1) {
                log.warn("支付记录处理中: paymentNo={}", paymentNo);
                throw new BusinessException("支付记录正在处理中");
            }

            record.setPayStatus(2);
            record.setPayTime(LocalDateTime.now());
            record.setTransactionId(transactionId);
            record.setChannelResponse(channelResponse);
            record.setNotifyStatus(1);
            record.setNotifyTime(LocalDateTime.now());
            paymentRecordMapper.updateById(record);

            escrowService.deposit(merchantId, record.getAmount());

            stringRedisTemplate.delete(PAYMENT_CACHE_KEY + paymentNo);
            stringRedisTemplate.opsForValue().set(idempotentKey, "SUCCESS", 24, TimeUnit.HOURS);
            
            log.info("支付回调处理成功: paymentNo={}, transactionId={}", paymentNo, transactionId);
        } catch (BusinessException e) {
            stringRedisTemplate.delete(idempotentKey);
            throw e;
        } catch (Exception e) {
            stringRedisTemplate.delete(idempotentKey);
            log.error("支付回调处理失败: paymentNo={}", paymentNo, e);
            throw new BusinessException("支付回调处理失败");
        }
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
    public PaymentRecord getPaymentStatus(Long paymentId) {
        String cacheKey = PAYMENT_CACHE_KEY + paymentId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return paymentRecordMapper.selectById(paymentId);
        }
        return paymentRecordMapper.selectById(paymentId);
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
    public void payCallbackBlockHandler(String paymentNo, String transactionId, String channelResponse, Long merchantId, BlockException ex) {
        log.warn("支付回调被限流: paymentNo={}", paymentNo);
        throw new BusinessException("支付回调请求过于频繁，请稍后再试");
    }

    /** 支付回调 - 降级处理 */
    public void payCallbackFallback(String paymentNo, String transactionId, String channelResponse, Long merchantId, Throwable ex) {
        log.error("支付回调降级: paymentNo={}", paymentNo, ex);
        throw new BusinessException("支付回调服务暂不可用，请稍后再试");
    }
}
