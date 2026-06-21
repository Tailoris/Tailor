package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.entity.EscrowAccount;
import com.tailoris.payment.mapper.EscrowAccountMapper;
import com.tailoris.payment.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private final EscrowAccountMapper escrowAccountMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ESCROW_LOCK_KEY = "escrow:lock:";

    @Override
    public BigDecimal getBalance(Long merchantId) {
        EscrowAccount account = getOrCreateAccount(merchantId);
        return account.getBalance();
    }

    @Override
    @Transactional
    public void deposit(Long merchantId, BigDecimal amount) {
        // 分布式锁串行化同一商家的入金操作，避免高并发下两次 depositAtomic 之间创建重复账户
        String lockKey = ESCROW_LOCK_KEY + merchantId;
        Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lockAcquired)) {
            throw new BusinessException("担保账户操作并发冲突，请稍后重试: merchantId=" + merchantId);
        }
        // 锁生命周期与事务边界一致：事务完成后（提交或回滚）才释放锁，避免提交前被其他线程穿透
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                stringRedisTemplate.delete(lockKey);
            }
        });
        // BE-H-11: 使用原子 UPDATE 避免竞态条件
        int rows = escrowAccountMapper.depositAtomic(merchantId, amount);
        if (rows == 0) {
            // 账户不存在，创建后重试
            getOrCreateAccount(merchantId);
            escrowAccountMapper.depositAtomic(merchantId, amount);
        }
        log.info("担保账户入金: merchantId={}, amount={}", merchantId, amount);
    }

    @Override
    @Transactional
    public void release(Long merchantId, BigDecimal amount) {
        // BE-H-11: 使用原子 UPDATE 避免竞态条件
        int rows = escrowAccountMapper.releaseAtomic(merchantId, amount);
        if (rows == 0) {
            throw new BusinessException("担保账户可用余额不足");
        }
        log.info("担保账户出金: merchantId={}, amount={}", merchantId, amount);
    }

    @Override
    @Transactional
    public void freeze(Long merchantId, BigDecimal amount) {
        // BE-H-11: 使用原子 UPDATE 避免竞态条件
        int rows = escrowAccountMapper.freezeAtomic(merchantId, amount);
        if (rows == 0) {
            throw new BusinessException("担保账户可用余额不足，无法冻结");
        }
        log.info("担保账户冻结: merchantId={}, amount={}", merchantId, amount);
    }

    @Override
    @Transactional
    public void unfreeze(Long merchantId, BigDecimal amount) {
        // BE-H-11: 使用原子 UPDATE 避免竞态条件
        int rows = escrowAccountMapper.unfreezeAtomic(merchantId, amount);
        if (rows == 0) {
            throw new BusinessException("担保账户冻结金额不足，无法解冻");
        }
        log.info("担保账户解冻: merchantId={}, amount={}", merchantId, amount);
    }

    private EscrowAccount getOrCreateAccount(Long merchantId) {
        EscrowAccount account = escrowAccountMapper.selectByMerchantId(merchantId);
        if (account == null) {
            account = new EscrowAccount();
            account.setId(com.tailoris.common.util.SnowflakeIdGenerator.getInstance().nextId());
            account.setMerchantId(merchantId);
            account.setBalance(BigDecimal.ZERO);
            account.setFrozenAmount(BigDecimal.ZERO);
            account.setStatus(1);
            try {
                escrowAccountMapper.insert(account);
            } catch (DuplicateKeyException e) {
                // 高并发下其他线程已创建账户，复用已存在账户避免重复创建
                log.warn("担保账户创建冲突，复用已存在账户: merchantId={}", merchantId);
                account = escrowAccountMapper.selectByMerchantId(merchantId);
            }
        }
        return account;
    }
}