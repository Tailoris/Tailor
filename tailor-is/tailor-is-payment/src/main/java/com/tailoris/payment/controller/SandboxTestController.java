package com.tailoris.payment.controller;

import com.tailoris.common.result.Result;
import com.tailoris.payment.service.SandboxTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Tag(name = "沙箱环境测试", description = "支付服务沙箱环境测试接口")
@RestController
@RequestMapping("/api/sandbox")
@RequiredArgsConstructor
public class SandboxTestController {

    private final SandboxTestService sandboxTestService;

    @Operation(summary = "微信支付测试", description = "测试微信支付下单功能")
    @PostMapping("/wechat/pay")
    public Result<Map<String, Object>> testWechatPay(
            @RequestParam String orderNo,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String openId) {
        
        log.info("接收到微信支付沙箱测试请求, orderNo={}, amount={}", orderNo, amount);
        Map<String, Object> result = sandboxTestService.testWechatPay(orderNo, amount, openId);
        
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.fail((String) result.get("message"));
        }
    }

    @Operation(summary = "支付宝支付测试", description = "测试支付宝支付下单功能")
    @PostMapping("/alipay/pay")
    public Result<Map<String, Object>> testAlipay(
            @RequestParam String orderNo,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String subject) {
        
        log.info("接收到支付宝支付沙箱测试请求, orderNo={}, amount={}", orderNo, amount);
        Map<String, Object> result = sandboxTestService.testAlipay(orderNo, amount, subject);
        
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.fail((String) result.get("message"));
        }
    }

    @Operation(summary = "微信退款测试", description = "测试微信退款功能")
    @PostMapping("/wechat/refund")
    public Result<Map<String, Object>> testWechatRefund(
            @RequestParam String outTradeNo,
            @RequestParam BigDecimal refundAmount) {
        
        log.info("接收到微信退款沙箱测试请求, outTradeNo={}, refundAmount={}", outTradeNo, refundAmount);
        Map<String, Object> result = sandboxTestService.testWechatRefund(outTradeNo, refundAmount);
        
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.fail((String) result.get("message"));
        }
    }

    @Operation(summary = "支付宝退款测试", description = "测试支付宝退款功能")
    @PostMapping("/alipay/refund")
    public Result<Map<String, Object>> testAlipayRefund(
            @RequestParam String outTradeNo,
            @RequestParam BigDecimal refundAmount) {
        
        log.info("接收到支付宝退款沙箱测试请求, outTradeNo={}, refundAmount={}", outTradeNo, refundAmount);
        Map<String, Object> result = sandboxTestService.testAlipayRefund(outTradeNo, refundAmount);
        
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.fail((String) result.get("message"));
        }
    }

    @Operation(summary = "验证支付状态", description = "查询并验证支付记录状态")
    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyPaymentStatus(@RequestParam String paymentNo) {
        log.info("接收到支付状态验证请求, paymentNo={}", paymentNo);
        Map<String, Object> result = sandboxTestService.verifyPaymentStatus(paymentNo);
        
        if ((Boolean) result.get("success")) {
            return Result.success(result);
        } else {
            return Result.fail((String) result.get("message"));
        }
    }

    @Operation(summary = "健康检查", description = "检查沙箱环境服务状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> healthCheck() {
        Map<String, Object> result = Map.of(
                "status", "UP",
                "service", "tailor-is-payment",
                "environment", "sandbox",
                "timestamp", System.currentTimeMillis()
        );
        return Result.success(result);
    }

    @Operation(summary = "完整支付流程测试", description = "测试从下单到回调的完整支付流程")
    @PostMapping("/flow/test")
    public Result<Map<String, Object>> testCompleteFlow(
            @RequestParam(required = false, defaultValue = "WECHAT") String channel,
            @RequestParam(required = false, defaultValue = "99.00") BigDecimal amount) {
        
        log.info("接收到完整支付流程测试请求, channel={}, amount={}", channel, amount);
        
        String orderNo = "SANDBOX" + System.currentTimeMillis();
        Map<String, Object> result = new java.util.HashMap<>();
        
        try {
            if ("ALIPAY".equalsIgnoreCase(channel)) {
                Map<String, Object> payResult = sandboxTestService.testAlipay(orderNo, amount, "沙箱完整流程测试");
                result.put("payStep", payResult);
                
                if ((Boolean) payResult.get("success")) {
                    Map<String, Object> statusResult = sandboxTestService.verifyPaymentStatus(orderNo);
                    result.put("verifyStep", statusResult);
                }
            } else {
                Map<String, Object> payResult = sandboxTestService.testWechatPay(orderNo, amount, null);
                result.put("payStep", payResult);
                
                if ((Boolean) payResult.get("success")) {
                    Map<String, Object> statusResult = sandboxTestService.verifyPaymentStatus(orderNo);
                    result.put("verifyStep", statusResult);
                }
            }
            
            result.put("success", true);
            result.put("message", "支付流程测试完成");
            result.put("orderNo", orderNo);
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("完整支付流程测试失败", e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            return Result.fail((String) result.get("message"));
        }
    }
}