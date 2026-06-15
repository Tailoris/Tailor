package com.tailoris.payment.mq;

import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.mapper.PaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationConsumer {

    private final PaymentRecordMapper paymentRecordMapper;

    public static final String PAYMENT_NOTIFY_QUEUE = "payment.notify.queue";

    @RabbitListener(queues = PAYMENT_NOTIFY_QUEUE)
    public void handlePaymentNotification(String paymentNo) {
        log.info("收到支付通知消息, paymentNo: {}", paymentNo);

        PaymentRecord payment = paymentRecordMapper.selectById(paymentNo);
        if (payment == null) {
            log.warn("支付记录不存在, paymentNo: {}", paymentNo);
            return;
        }

        log.info("支付通知处理完成, paymentNo: {}, status: {}", paymentNo, payment.getPayStatus());
    }
}