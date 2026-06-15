package com.tailoris.payment.service.impl;

import com.tailoris.payment.service.AlipayService;
import com.tailoris.payment.service.PaymentService;
import com.tailoris.payment.service.SandboxTestService;
import com.tailoris.payment.service.WechatPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxTestServiceImpl implements SandboxTestService {

    private final WechatPayService wechatPayService;
    private final AlipayService alipayService;
    private final PaymentService paymentService;

    @Override
    public Map<String, Object> testWechatPay(String orderNo, BigDecimal amount, String openId) {
        log.info("开始微信支付沙箱测试, orderNo={}, amount={}", orderNo, amount);
        
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> payResult = wechatPayService.createOrder(
                    orderNo,
                    amount,
                    openId != null ? openId : "oSandboxTestOpenId",
                    "192.168.1.1",
                    "https://sandbox.tailoris.com/api/v1/payment/wechat/callback",
                    "沙箱测试商品"
            );
            
            result.put("success", true);
            result.put("message", "微信支付下单成功");
            result.put("data", payResult);
            log.info("微信支付沙箱测试成功, prepayId={}", payResult.get("prepay_id"));
        } catch (Exception e) {
            log.error("微信支付沙箱测试失败", e);
            result.put("success", false);
            result.put("message", "微信支付下单失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testAlipay(String orderNo, BigDecimal amount, String subject) {
        log.info("开始支付宝沙箱测试, orderNo={}, amount={}", orderNo, amount);
        
        Map<String, Object> result = new HashMap<>();
        try {
            String form = alipayService.createOrder(
                    orderNo,
                    amount,
                    subject != null ? subject : "沙箱测试商品",
                    "沙箱测试订单描述",
                    "https://sandbox.tailoris.com/pay/success",
                    "https://sandbox.tailoris.com/api/v1/payment/alipay/callback"
            );
            
            result.put("success", true);
            result.put("message", "支付宝下单成功");
            result.put("form", form);
            log.info("支付宝沙箱测试成功");
        } catch (Exception e) {
            log.error("支付宝沙箱测试失败", e);
            result.put("success", false);
            result.put("message", "支付宝下单失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testWechatRefund(String outTradeNo, BigDecimal refundAmount) {
        log.info("开始微信退款沙箱测试, outTradeNo={}, refundAmount={}", outTradeNo, refundAmount);
        
        Map<String, Object> result = new HashMap<>();
        try {
            String outRefundNo = "REF" + System.currentTimeMillis();
            Map<String, Object> refundResult = wechatPayService.refund(
                    outTradeNo,
                    outRefundNo,
                    refundAmount,
                    refundAmount
            );
            
            result.put("success", true);
            result.put("message", "微信退款成功");
            result.put("data", refundResult);
            log.info("微信退款沙箱测试成功");
        } catch (Exception e) {
            log.error("微信退款沙箱测试失败", e);
            result.put("success", false);
            result.put("message", "微信退款失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> testAlipayRefund(String outTradeNo, BigDecimal refundAmount) {
        log.info("开始支付宝退款沙箱测试, outTradeNo={}, refundAmount={}", outTradeNo, refundAmount);
        
        Map<String, Object> result = new HashMap<>();
        try {
            String outRequestNo = "REF" + System.currentTimeMillis();
            String tradeNo = alipayService.refund(
                    outTradeNo,
                    outRequestNo,
                    refundAmount,
                    "沙箱测试退款"
            );
            
            result.put("success", true);
            result.put("message", "支付宝退款成功");
            result.put("tradeNo", tradeNo);
            log.info("支付宝退款沙箱测试成功");
        } catch (Exception e) {
            log.error("支付宝退款沙箱测试失败", e);
            result.put("success", false);
            result.put("message", "支付宝退款失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Object> verifyPaymentStatus(String paymentNo) {
        log.info("验证支付状态, paymentNo={}", paymentNo);
        
        Map<String, Object> result = new HashMap<>();
        try {
            var paymentRecord = paymentService.getPaymentByPaymentNo(paymentNo);
            
            if (paymentRecord != null) {
                result.put("success", true);
                result.put("message", "支付记录存在");
                result.put("paymentNo", paymentRecord.getPaymentNo());
                result.put("payStatus", getPayStatusDesc(paymentRecord.getPayStatus()));
                result.put("amount", paymentRecord.getAmount());
            } else {
                result.put("success", false);
                result.put("message", "支付记录不存在");
            }
        } catch (Exception e) {
            log.error("验证支付状态失败", e);
            result.put("success", false);
            result.put("message", "验证失败: " + e.getMessage());
        }
        
        return result;
    }

    private String getPayStatusDesc(Integer status) {
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "支付中";
            case 2 -> "已支付";
            case 3 -> "支付失败";
            default -> "未知状态";
        };
    }
}