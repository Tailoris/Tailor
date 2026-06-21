package com.tailoris.payment.service;

import com.tailoris.payment.dto.PayRequest;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.entity.RefundRecord;

public interface PaymentService {

    PaymentRecord createPayment(Long userId, PayRequest request);

    void payCallback(String paymentNo, String transactionId, String channelResponse, String sign, String signType);

    RefundRecord refund(Long userId, Long ticketId, Long orderId, java.math.BigDecimal amount, Integer refundChannel, String remark);

    PaymentRecord getPaymentStatus(Long paymentId);

    PaymentRecord getPaymentByPaymentNo(String paymentNo);
}