package com.tailoris.payment.service.impl;

import com.tailoris.common.exception.BusinessException;
import com.tailoris.payment.entity.EscrowAccount;
import com.tailoris.payment.mapper.EscrowAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("EscrowServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class EscrowServiceImplTest {

    @Mock
    private EscrowAccountMapper escrowAccountMapper;

    @InjectMocks
    private EscrowServiceImpl escrowService;

    private EscrowAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new EscrowAccount();
        testAccount.setId(1L);
        testAccount.setMerchantId(100L);
        testAccount.setBalance(new BigDecimal("10000.00"));
        testAccount.setFrozenAmount(new BigDecimal("2000.00"));
        testAccount.setStatus(1);
    }

    @Test
    @DisplayName("查询担保账户余额成功")
    void testGetBalance_Success() {
        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(testAccount);

        BigDecimal balance = escrowService.getBalance(100L);

        assertNotNull(balance);
        assertEquals(new BigDecimal("10000.00"), balance);
    }

    @Test
    @DisplayName("查询新商户余额时自动创建账户")
    void testGetBalance_AutoCreate() {
        when(escrowAccountMapper.selectByMerchantId(200L)).thenReturn(null);
        when(escrowAccountMapper.insert(any(EscrowAccount.class))).thenReturn(1);

        BigDecimal balance = escrowService.getBalance(200L);

        assertEquals(BigDecimal.ZERO, balance);
        verify(escrowAccountMapper).insert(any(EscrowAccount.class));
    }

    @Test
    @DisplayName("担保账户入金成功")
    void testDeposit_Success() {
        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(testAccount);
        when(escrowAccountMapper.updateBalance(eq(1L), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(1);

        escrowService.deposit(100L, new BigDecimal("5000.00"));

        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(escrowAccountMapper).updateBalance(eq(1L), balanceCaptor.capture(), any(BigDecimal.class));
        assertEquals(new BigDecimal("15000.00"), balanceCaptor.getValue());
    }

    @Test
    @DisplayName("释放资金成功")
    void testRelease_Success() {
        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(testAccount);
        when(escrowAccountMapper.updateBalance(eq(1L), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(1);

        escrowService.release(100L, new BigDecimal("3000.00"));

        ArgumentCaptor<BigDecimal> balanceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(escrowAccountMapper).updateBalance(eq(1L), balanceCaptor.capture(), any(BigDecimal.class));
        assertEquals(new BigDecimal("7000.00"), balanceCaptor.getValue());
    }

    @Test
    @DisplayName("释放资金时可用余额不足")
    void testRelease_InsufficientBalance() {
        EscrowAccount lowBalanceAccount = new EscrowAccount();
        lowBalanceAccount.setId(1L);
        lowBalanceAccount.setMerchantId(100L);
        lowBalanceAccount.setBalance(new BigDecimal("100.00"));
        lowBalanceAccount.setFrozenAmount(new BigDecimal("100.00"));
        lowBalanceAccount.setStatus(1);

        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(lowBalanceAccount);

        assertThrows(BusinessException.class, () ->
                escrowService.release(100L, new BigDecimal("1.00")));
    }

    @Test
    @DisplayName("冻结资金成功")
    void testFreeze_Success() {
        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(testAccount);
        when(escrowAccountMapper.updateBalance(eq(1L), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(1);

        escrowService.freeze(100L, new BigDecimal("1000.00"));

        ArgumentCaptor<BigDecimal> frozenCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(escrowAccountMapper).updateBalance(eq(1L), any(BigDecimal.class), frozenCaptor.capture());
        assertEquals(new BigDecimal("3000.00"), frozenCaptor.getValue());
    }

    @Test
    @DisplayName("冻结资金时可用余额不足")
    void testFreeze_InsufficientAvailable() {
        EscrowAccount fullyFrozen = new EscrowAccount();
        fullyFrozen.setId(1L);
        fullyFrozen.setMerchantId(100L);
        fullyFrozen.setBalance(new BigDecimal("2000.00"));
        fullyFrozen.setFrozenAmount(new BigDecimal("2000.00"));
        fullyFrozen.setStatus(1);

        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(fullyFrozen);

        assertThrows(BusinessException.class, () ->
                escrowService.freeze(100L, new BigDecimal("1.00")));
    }

    @Test
    @DisplayName("解冻资金成功")
    void testUnfreeze_Success() {
        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(testAccount);
        when(escrowAccountMapper.updateBalance(eq(1L), any(BigDecimal.class), any(BigDecimal.class))).thenReturn(1);

        escrowService.unfreeze(100L, new BigDecimal("500.00"));

        ArgumentCaptor<BigDecimal> frozenCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(escrowAccountMapper).updateBalance(eq(1L), any(BigDecimal.class), frozenCaptor.capture());
        assertEquals(new BigDecimal("1500.00"), frozenCaptor.getValue());
    }

    @Test
    @DisplayName("解冻资金时冻结金额不足")
    void testUnfreeze_InsufficientFrozen() {
        EscrowAccount lowFrozen = new EscrowAccount();
        lowFrozen.setId(1L);
        lowFrozen.setMerchantId(100L);
        lowFrozen.setBalance(new BigDecimal("10000.00"));
        lowFrozen.setFrozenAmount(new BigDecimal("100.00"));
        lowFrozen.setStatus(1);

        when(escrowAccountMapper.selectByMerchantId(100L)).thenReturn(lowFrozen);

        assertThrows(BusinessException.class, () ->
                escrowService.unfreeze(100L, new BigDecimal("200.00")));
    }
}