package com.tailoris.payment.mq;

import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@DisplayName("PaymentNotificationConsumer 测试")
@ExtendWith(MockitoExtension.class)
class PaymentNotificationConsumerTest {

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @InjectMocks
    private PaymentNotificationConsumer paymentNotificationConsumer;

    private static final String PAYMENT_NO = "PAY202606130001";

    private PaymentRecord buildPayment(Integer payStatus) {
        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentNo(PAYMENT_NO);
        payment.setPayStatus(payStatus);
        payment.setAmount(new BigDecimal("100.00"));
        return payment;
    }

    @Test
    @DisplayName("支付记录不存在 - 记录警告并返回")
    void handlePaymentNotification_PaymentNotFound_ShouldReturn() {
        when(paymentRecordMapper.selectById(PAYMENT_NO)).thenReturn(null);

        paymentNotificationConsumer.handlePaymentNotification(PAYMENT_NO);

        verify(paymentRecordMapper).selectById(PAYMENT_NO);
    }

    @Test
    @DisplayName("支付记录存在 - 通知处理成功")
    void handlePaymentNotification_PaymentExists_ShouldProcess() {
        PaymentRecord payment = buildPayment(2); // 已支付
        when(paymentRecordMapper.selectById(PAYMENT_NO)).thenReturn(payment);

        paymentNotificationConsumer.handlePaymentNotification(PAYMENT_NO);

        verify(paymentRecordMapper).selectById(PAYMENT_NO);
    }
}