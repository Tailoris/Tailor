package com.tailoris.payment.service;

import java.math.BigDecimal;

public interface EscrowService {

    BigDecimal getBalance(Long merchantId);

    void deposit(Long merchantId, BigDecimal amount);

    void release(Long merchantId, BigDecimal amount);

    void freeze(Long merchantId, BigDecimal amount);

    void unfreeze(Long merchantId, BigDecimal amount);
}