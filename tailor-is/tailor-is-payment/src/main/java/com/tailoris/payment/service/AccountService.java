package com.tailoris.payment.service;

import com.tailoris.payment.dto.AccountInfoResponse;
import com.tailoris.payment.dto.WithdrawRequest;
import com.tailoris.payment.entity.MerchantAccount;
import com.tailoris.payment.entity.UserAccount;
import com.tailoris.payment.entity.WithdrawRecord;
import com.tailoris.payment.entity.RechargeRecord;

import java.math.BigDecimal;

public interface AccountService {

    AccountInfoResponse getMerchantAccount(Long merchantId);

    AccountInfoResponse getUserAccount(Long userId);

    WithdrawRequest createWithdraw(Long merchantId, WithdrawRequest request);

    WithdrawRecord getWithdrawByNo(String withdrawNo);

    RechargeRecord recharge(Long userId, BigDecimal amount, Integer payChannel);

    void payRechargeCallback(String rechargeNo, String transactionId);

    void approveWithdraw(Long withdrawId, Long auditBy, String auditRemark);

    void rejectWithdraw(Long withdrawId, Long auditBy, String auditRemark);

    MerchantAccount getOrCreateMerchantAccount(Long merchantId);

    UserAccount getOrCreateUserAccount(Long userId);
}
