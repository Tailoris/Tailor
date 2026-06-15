package com.tailoris.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AccountServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceImplTest {

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private MerchantAccountMapper merchantAccountMapper;

    @Mock
    private WithdrawRecordMapper withdrawRecordMapper;

    @Mock
    private RechargeRecordMapper rechargeRecordMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AccountServiceImpl accountService;

    private MerchantAccount merchantAccount;
    private UserAccount userAccount;
    private WithdrawRecord withdrawRecord;
    private RechargeRecord rechargeRecord;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);

        merchantAccount = new MerchantAccount();
        merchantAccount.setId(1L);
        merchantAccount.setMerchantId(100L);
        merchantAccount.setBalance(new BigDecimal("50000.00"));
        merchantAccount.setWithdrawableBalance(new BigDecimal("30000.00"));
        merchantAccount.setFrozenAmount(new BigDecimal("5000.00"));
        merchantAccount.setPendingAmount(new BigDecimal("15000.00"));
        merchantAccount.setTotalIncome(new BigDecimal("100000.00"));
        merchantAccount.setTotalExpense(new BigDecimal("50000.00"));
        merchantAccount.setTotalWithdraw(new BigDecimal("30000.00"));
        merchantAccount.setTotalSettlement(new BigDecimal("20000.00"));
        merchantAccount.setVersion(0);

        userAccount = new UserAccount();
        userAccount.setId(1L);
        userAccount.setUserId(200L);
        userAccount.setBalance(new BigDecimal("10000.00"));
        userAccount.setFrozenAmount(new BigDecimal("500.00"));
        userAccount.setTotalRecharge(new BigDecimal("50000.00"));
        userAccount.setTotalConsume(new BigDecimal("40000.00"));
        userAccount.setTotalRefund(new BigDecimal("1000.00"));
        userAccount.setPoints(100);
        userAccount.setTotalPointsEarned(1000);
        userAccount.setTotalPointsSpent(900);
        userAccount.setVersion(0);

        withdrawRecord = new WithdrawRecord();
        withdrawRecord.setId(300L);
        withdrawRecord.setWithdrawNo("WITH123456");
        withdrawRecord.setMerchantId(100L);
        withdrawRecord.setAmount(new BigDecimal("10000.00"));
        withdrawRecord.setFee(new BigDecimal("100.00"));
        withdrawRecord.setActualAmount(new BigDecimal("9900.00"));
        withdrawRecord.setStatus(0);

        rechargeRecord = new RechargeRecord();
        rechargeRecord.setId(400L);
        rechargeRecord.setRechargeNo("RCH123456");
        rechargeRecord.setUserId(200L);
        rechargeRecord.setAmount(new BigDecimal("5000.00"));
        rechargeRecord.setBonusAmount(BigDecimal.ZERO);
        rechargeRecord.setPayChannel(1);
        rechargeRecord.setPayStatus(0);
        rechargeRecord.setStatus(0);
    }

    @Test
    @DisplayName("查询商家账户信息成功")
    void testGetMerchantAccount_Success() {
        when(valueOperations.get("merchant:account:100")).thenReturn("cached");
        when(merchantAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchantAccount);

        AccountInfoResponse response = accountService.getMerchantAccount(100L);

        assertNotNull(response);
        assertEquals(100L, response.getAccountId());
        assertEquals(2, response.getAccountType());
        assertEquals(merchantAccount.getBalance(), response.getBalance());
        assertEquals(merchantAccount.getWithdrawableBalance(), response.getWithdrawableBalance());
        assertEquals(merchantAccount.getFrozenAmount(), response.getFrozenAmount());
        assertEquals(merchantAccount.getPendingAmount(), response.getPendingAmount());
        assertEquals(merchantAccount.getTotalIncome(), response.getTotalIncome());
        assertEquals(merchantAccount.getTotalExpense(), response.getTotalExpense());
        assertEquals(merchantAccount.getTotalWithdraw(), response.getTotalWithdraw());
        assertEquals(merchantAccount.getTotalSettlement(), response.getTotalSettlement());
    }

    @Test
    @DisplayName("查询用户账户信息成功")
    void testGetUserAccount_Success() {
        when(valueOperations.get("user:account:200")).thenReturn("cached");
        when(userAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(userAccount);

        AccountInfoResponse response = accountService.getUserAccount(200L);

        assertNotNull(response);
        assertEquals(200L, response.getAccountId());
        assertEquals(1, response.getAccountType());
        assertEquals(userAccount.getBalance(), response.getBalance());
        assertEquals(userAccount.getFrozenAmount(), response.getFrozenAmount());
        assertEquals(userAccount.getPoints(), response.getPoints());
        assertEquals(userAccount.getTotalRecharge(), response.getTotalIncome());
        assertEquals(userAccount.getTotalConsume(), response.getTotalExpense());
    }

    @Test
    @DisplayName("查询商家账户-缓存未命中则查库")
    void testGetMerchantAccount_CacheMiss() {
        lenient().when(valueOperations.get("merchant:account:100")).thenReturn(null);
        when(merchantAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(merchantAccount);

        AccountInfoResponse response = accountService.getMerchantAccount(100L);

        assertNotNull(response);
        assertEquals(100L, response.getAccountId());
    }

    @Test
    @DisplayName("创建提现时金额恰好等于可提现余额")
    void testCreateWithdraw_ExactAmount() {
        MerchantAccount exactAccount = new MerchantAccount();
        exactAccount.setId(3L);
        exactAccount.setMerchantId(300L);
        exactAccount.setBalance(new BigDecimal("5000.00"));
        exactAccount.setWithdrawableBalance(new BigDecimal("1000.00"));
        exactAccount.setFrozenAmount(BigDecimal.ZERO);
        exactAccount.setPendingAmount(BigDecimal.ZERO);
        exactAccount.setTotalIncome(BigDecimal.ZERO);
        exactAccount.setTotalExpense(BigDecimal.ZERO);
        exactAccount.setTotalWithdraw(BigDecimal.ZERO);
        exactAccount.setTotalSettlement(BigDecimal.ZERO);
        exactAccount.setVersion(0);

        lenient().when(valueOperations.get("merchant:account:300")).thenReturn(null);
        when(merchantAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(exactAccount);
        when(withdrawRecordMapper.insert(any(WithdrawRecord.class))).thenReturn(1);
        when(merchantAccountMapper.updateById(any(MerchantAccount.class))).thenReturn(1);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(new BigDecimal("1000.00"));
        request.setBankName("农业银行");
        request.setBankBranch("某某支行");
        request.setBankAccount("6222001111111111");
        request.setAccountName("测试");

        assertDoesNotThrow(() -> accountService.createWithdraw(300L, request));

        assertEquals(0, exactAccount.getWithdrawableBalance().compareTo(BigDecimal.ZERO));
    }
}