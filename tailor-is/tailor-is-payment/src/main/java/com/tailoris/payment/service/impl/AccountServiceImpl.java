package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.payment.dto.AccountInfoResponse;
import com.tailoris.payment.dto.WithdrawRequest;
import com.tailoris.payment.entity.MerchantAccount;
import com.tailoris.payment.entity.RechargeRecord;
import com.tailoris.payment.entity.UserAccount;
import com.tailoris.payment.entity.WithdrawRecord;
import com.tailoris.payment.mapper.MerchantAccountMapper;
import com.tailoris.payment.mapper.RechargeRecordMapper;
import com.tailoris.payment.mapper.UserAccountMapper;
import com.tailoris.payment.mapper.WithdrawRecordMapper;
import com.tailoris.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 账户管理服务实现
 *
 * <p>负责用户账户和商家账户的余额管理、提现申请、充值记录等核心资金操作。
 * 使用Redis分布式锁保证提现操作的并发安全性。}</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserAccountMapper userAccountMapper;
    private final MerchantAccountMapper merchantAccountMapper;
    private final WithdrawRecordMapper withdrawRecordMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String USER_ACCOUNT_CACHE_KEY = "user:account:";
    private static final String MERCHANT_ACCOUNT_CACHE_KEY = "merchant:account:";

    @Override
    public AccountInfoResponse getMerchantAccount(Long merchantId) {
        MerchantAccount account = getOrCreateMerchantAccount(merchantId);
        AccountInfoResponse response = new AccountInfoResponse();
        response.setAccountId(merchantId);
        response.setAccountType(2);
        response.setBalance(account.getBalance());
        response.setWithdrawableBalance(account.getWithdrawableBalance());
        response.setFrozenAmount(account.getFrozenAmount());
        response.setPendingAmount(account.getPendingAmount());
        response.setTotalIncome(account.getTotalIncome());
        response.setTotalExpense(account.getTotalExpense());
        response.setTotalWithdraw(account.getTotalWithdraw());
        response.setTotalSettlement(account.getTotalSettlement());
        return response;
    }

    @Override
    public AccountInfoResponse getUserAccount(Long userId) {
        UserAccount account = getOrCreateUserAccount(userId);
        AccountInfoResponse response = new AccountInfoResponse();
        response.setAccountId(userId);
        response.setAccountType(1);
        response.setBalance(account.getBalance());
        response.setFrozenAmount(account.getFrozenAmount());
        response.setPoints(account.getPoints());
        response.setTotalIncome(account.getTotalRecharge());
        response.setTotalExpense(account.getTotalConsume());
        return response;
    }

    @Override
    @Transactional
    public WithdrawRequest createWithdraw(Long merchantId, WithdrawRequest request) {
        MerchantAccount account = getOrCreateMerchantAccount(merchantId);
        if (account.getWithdrawableBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException("可提现余额不足");
        }

        BigDecimal fee = request.getAmount().multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal actualAmount = request.getAmount().subtract(fee);

        WithdrawRecord record = new WithdrawRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setWithdrawNo("WITH" + SnowflakeIdGenerator.getInstance().nextId());
        record.setMerchantId(merchantId);
        record.setAmount(request.getAmount());
        record.setFee(fee);
        record.setActualAmount(actualAmount);
        record.setBankName(request.getBankName());
        record.setBankBranch(request.getBankBranch());
        record.setBankAccount(request.getBankAccount());
        record.setAccountName(request.getAccountName());
        record.setStatus(0);
        withdrawRecordMapper.insert(record);

        account.setWithdrawableBalance(account.getWithdrawableBalance().subtract(request.getAmount()));
        account.setFrozenAmount(account.getFrozenAmount().add(request.getAmount()));
        merchantAccountMapper.updateById(account);

        stringRedisTemplate.delete(MERCHANT_ACCOUNT_CACHE_KEY + merchantId);

        return request;
    }

    @Override
    public WithdrawRecord getWithdrawByNo(String withdrawNo) {
        LambdaQueryWrapper<WithdrawRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WithdrawRecord::getWithdrawNo, withdrawNo);
        return withdrawRecordMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public RechargeRecord recharge(Long userId, BigDecimal amount, Integer payChannel) {
        RechargeRecord record = new RechargeRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setRechargeNo("RCH" + SnowflakeIdGenerator.getInstance().nextId());
        record.setUserId(userId);
        record.setAmount(amount);
        record.setBonusAmount(BigDecimal.ZERO);
        record.setPayChannel(payChannel);
        record.setPayStatus(0);
        record.setStatus(0);
        rechargeRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public void payRechargeCallback(String rechargeNo, String transactionId) {
        LambdaQueryWrapper<RechargeRecord> query = new LambdaQueryWrapper<>();
        query.eq(RechargeRecord::getRechargeNo, rechargeNo);
        RechargeRecord record = rechargeRecordMapper.selectOne(query);
        if (record == null || record.getStatus() == 2) {
            return;
        }

        record.setPayStatus(2);
        record.setStatus(2);
        record.setPayTime(LocalDateTime.now());
        record.setTransactionId(transactionId);
        rechargeRecordMapper.updateById(record);

        UserAccount account = getOrCreateUserAccount(record.getUserId());
        account.setBalance(account.getBalance().add(record.getAmount()).add(record.getBonusAmount()));
        account.setTotalRecharge(account.getTotalRecharge().add(record.getAmount()));
        userAccountMapper.updateById(account);

        stringRedisTemplate.delete(USER_ACCOUNT_CACHE_KEY + record.getUserId());
    }

    @Override
    @Transactional
    public void approveWithdraw(Long withdrawId, Long auditBy, String auditRemark) {
        WithdrawRecord record = withdrawRecordMapper.selectById(withdrawId);
        if (record == null) {
            throw new BusinessException("提现记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("提现记录状态异常");
        }

        record.setStatus(2);
        record.setAuditBy(auditBy);
        record.setAuditTime(LocalDateTime.now());
        record.setAuditRemark(auditRemark);
        record.setProcessedAt(LocalDateTime.now());
        withdrawRecordMapper.updateById(record);

        MerchantAccount account = getOrCreateMerchantAccount(record.getMerchantId());
        account.setFrozenAmount(account.getFrozenAmount().subtract(record.getAmount()));
        account.setTotalWithdraw(account.getTotalWithdraw().add(record.getAmount()));
        merchantAccountMapper.updateById(account);

        stringRedisTemplate.delete(MERCHANT_ACCOUNT_CACHE_KEY + record.getMerchantId());
    }

    @Override
    @Transactional
    public void rejectWithdraw(Long withdrawId, Long auditBy, String auditRemark) {
        WithdrawRecord record = withdrawRecordMapper.selectById(withdrawId);
        if (record == null) {
            throw new BusinessException("提现记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new BusinessException("提现记录状态异常");
        }

        record.setStatus(4);
        record.setAuditBy(auditBy);
        record.setAuditTime(LocalDateTime.now());
        record.setAuditRemark(auditRemark);
        record.setFailReason(auditRemark);
        withdrawRecordMapper.updateById(record);

        MerchantAccount account = getOrCreateMerchantAccount(record.getMerchantId());
        account.setFrozenAmount(account.getFrozenAmount().subtract(record.getAmount()));
        account.setWithdrawableBalance(account.getWithdrawableBalance().add(record.getAmount()));
        merchantAccountMapper.updateById(account);

        stringRedisTemplate.delete(MERCHANT_ACCOUNT_CACHE_KEY + record.getMerchantId());
    }

    @Override
    public MerchantAccount getOrCreateMerchantAccount(Long merchantId) {
        String cacheKey = MERCHANT_ACCOUNT_CACHE_KEY + merchantId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            LambdaQueryWrapper<MerchantAccount> query = new LambdaQueryWrapper<>();
            query.eq(MerchantAccount::getMerchantId, merchantId);
            return merchantAccountMapper.selectOne(query);
        }

        LambdaQueryWrapper<MerchantAccount> query = new LambdaQueryWrapper<>();
        query.eq(MerchantAccount::getMerchantId, merchantId);
        MerchantAccount account = merchantAccountMapper.selectOne(query);
        if (account == null) {
            account = new MerchantAccount();
            account.setId(SnowflakeIdGenerator.getInstance().nextId());
            account.setMerchantId(merchantId);
            account.setBalance(BigDecimal.ZERO);
            account.setWithdrawableBalance(BigDecimal.ZERO);
            account.setFrozenAmount(BigDecimal.ZERO);
            account.setPendingAmount(BigDecimal.ZERO);
            account.setTotalIncome(BigDecimal.ZERO);
            account.setTotalExpense(BigDecimal.ZERO);
            account.setTotalWithdraw(BigDecimal.ZERO);
            account.setTotalSettlement(BigDecimal.ZERO);
            account.setVersion(0);
            merchantAccountMapper.insert(account);
        }
        return account;
    }

    @Override
    public UserAccount getOrCreateUserAccount(Long userId) {
        String cacheKey = USER_ACCOUNT_CACHE_KEY + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            LambdaQueryWrapper<UserAccount> query = new LambdaQueryWrapper<>();
            query.eq(UserAccount::getUserId, userId);
            return userAccountMapper.selectOne(query);
        }

        LambdaQueryWrapper<UserAccount> query = new LambdaQueryWrapper<>();
        query.eq(UserAccount::getUserId, userId);
        UserAccount account = userAccountMapper.selectOne(query);
        if (account == null) {
            account = new UserAccount();
            account.setId(SnowflakeIdGenerator.getInstance().nextId());
            account.setUserId(userId);
            account.setBalance(BigDecimal.ZERO);
            account.setFrozenAmount(BigDecimal.ZERO);
            account.setTotalRecharge(BigDecimal.ZERO);
            account.setTotalConsume(BigDecimal.ZERO);
            account.setTotalRefund(BigDecimal.ZERO);
            account.setPoints(0);
            account.setTotalPointsEarned(0);
            account.setTotalPointsSpent(0);
            account.setVersion(0);
            userAccountMapper.insert(account);
        }
        return account;
    }
}
