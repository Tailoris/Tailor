package com.tailoris.payment.mq;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("PaymentNotificationConsumer 测试")
@ExtendWith(MockitoExtension.class)
class PaymentNotificationConsumerTest {

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private ObjectMapper objectMapper;

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
    void handlePaymentNotification_PaymentNotFound_ShouldReturn() throws Exception {
        Map<String, Object> message = Map.of("paymentNo", PAYMENT_NO, "payStatus", 1);
        String jsonMessage = "{\"paymentNo\":\"PAY202606130001\",\"payStatus\":1}";

        when(objectMapper.readValue(eq(jsonMessage), any(TypeReference.class)))
                .thenReturn(message);
        when(paymentRecordMapper.selectById(PAYMENT_NO)).thenReturn(null);
        when(paymentRecordMapper.selectOne(any())).thenReturn(null);

        paymentNotificationConsumer.handlePaymentNotification(jsonMessage);

        verify(paymentRecordMapper).selectById(PAYMENT_NO);
        verify(paymentRecordMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("支付记录存在 - 通知处理成功并更新状态")
    void handlePaymentNotification_PaymentExists_ShouldProcess() throws Exception {
        PaymentRecord payment = buildPayment(0); // 待支付
        Map<String, Object> message = Map.of("paymentNo", PAYMENT_NO, "payStatus", 1,
                "transactionId", "TXN123456");
        String jsonMessage = "{\"paymentNo\":\"PAY202606130001\",\"payStatus\":1,\"transactionId\":\"TXN123456\"}";

        when(objectMapper.readValue(eq(jsonMessage), any(TypeReference.class)))
                .thenReturn(message);
        when(paymentRecordMapper.selectById(PAYMENT_NO)).thenReturn(payment);

        paymentNotificationConsumer.handlePaymentNotification(jsonMessage);

        verify(paymentRecordMapper).updateById(argThat(p ->
                ((PaymentRecord) p).getPayStatus() == 1
                        && "TXN123456".equals(((PaymentRecord) p).getTransactionId())
        ));
    }
}