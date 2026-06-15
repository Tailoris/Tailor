package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.entity.EscrowAccount;
import com.tailoris.payment.mapper.EscrowAccountMapper;
import com.tailoris.payment.service.EscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private final EscrowAccountMapper escrowAccountMapper;

    @Override
    public BigDecimal getBalance(Long merchantId) {
        EscrowAccount account = getOrCreateAccount(merchantId);
        return account.getBalance();
    }

    @Override
    @Transactional
    public void deposit(Long merchantId, BigDecimal amount) {
        EscrowAccount account = getOrCreateAccount(merchantId);
        account.setBalance(account.getBalance().add(amount));
        escrowAccountMapper.updateBalance(account.getId(), account.getBalance(), account.getFrozenAmount());
        log.info("担保账户入金: merchantId={}, amount={}, balance={}", merchantId, amount, account.getBalance());
    }

    @Override
    @Transactional
    public void release(Long merchantId, BigDecimal amount) {
        EscrowAccount account = getOrCreateAccount(merchantId);
        BigDecimal available = account.getBalance().subtract(account.getFrozenAmount());
        if (available.compareTo(amount) < 0) {
            throw new BusinessException("担保账户可用余额不足");
        }
        account.setBalance(account.getBalance().subtract(amount));
        escrowAccountMapper.updateBalance(account.getId(), account.getBalance(), account.getFrozenAmount());
        log.info("担保账户出金: merchantId={}, amount={}, balance={}", merchantId, amount, account.getBalance());
    }

    @Override
    @Transactional
    public void freeze(Long merchantId, BigDecimal amount) {
        EscrowAccount account = getOrCreateAccount(merchantId);
        BigDecimal available = account.getBalance().subtract(account.getFrozenAmount());
        if (available.compareTo(amount) < 0) {
            throw new BusinessException("担保账户可用余额不足，无法冻结");
        }
        account.setFrozenAmount(account.getFrozenAmount().add(amount));
        escrowAccountMapper.updateBalance(account.getId(), account.getBalance(), account.getFrozenAmount());
        log.info("担保账户冻结: merchantId={}, amount={}, frozen={}", merchantId, amount, account.getFrozenAmount());
    }

    @Override
    @Transactional
    public void unfreeze(Long merchantId, BigDecimal amount) {
        EscrowAccount account = getOrCreateAccount(merchantId);
        if (account.getFrozenAmount().compareTo(amount) < 0) {
            throw new BusinessException("担保账户冻结金额不足，无法解冻");
        }
        account.setFrozenAmount(account.getFrozenAmount().subtract(amount));
        escrowAccountMapper.updateBalance(account.getId(), account.getBalance(), account.getFrozenAmount());
        log.info("担保账户解冻: merchantId={}, amount={}, frozen={}", merchantId, amount, account.getFrozenAmount());
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
            escrowAccountMapper.insert(account);
        }
        return account;
    }
}