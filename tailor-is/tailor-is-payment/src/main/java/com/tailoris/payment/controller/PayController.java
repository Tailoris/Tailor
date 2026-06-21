package com.tailoris.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.payment.dto.PayRequest;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.service.AlipayService;
import com.tailoris.payment.service.PaymentService;
import com.tailoris.payment.service.WechatPayService;
import com.tailoris.payment.service.impl.AlipayServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@SaCheckLogin
@Tag(name = "支付管理", description = "微信支付、支付宝支付")
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PayController {

    private final PaymentService paymentService;
    private final WechatPayService wechatPayService;
    private final AlipayService alipayService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${tailoris.gateway.external-url:https://api.tailoris.com}")
    private String gatewayExternalUrl;

    private static final String CALLBACK_IDEMPOTENT_KEY = "payment:callback:idempotent:";

    @Operation(summary = "微信支付", description = "创建微信支付订单")
    @PostMapping("/wechat")
    public Result<Map<String, Object>> wechatPay(@RequestBody PayRequest request, HttpServletRequest httpRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        PaymentRecord record = paymentService.createPayment(userId, request);
        
        String clientIp = getClientIp(httpRequest);
        String notifyUrl = buildNotifyUrl("/api/v1/payment/wechat/callback");
        
        Map<String, Object> result = wechatPayService.createJsapiPayment(
                record.getPaymentNo(),
                record.getAmount(),
                request.getOpenId(),
                request.getBody() != null ? request.getBody() : "Tailor IS 订单支付",
                clientIp,
                notifyUrl
        );
        
        result.put("paymentNo", record.getPaymentNo());
        result.put("orderId", record.getOrderId());
        log.info("微信支付订单创建成功, paymentNo: {}, orderId: {}", record.getPaymentNo(), record.getOrderId());
        return Result.success(result);
    }

    @Operation(summary = "支付宝支付", description = "创建支付宝支付订单")
    @PostMapping("/alipay")
    public Result<String> alipayPay(@RequestBody PayRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        PaymentRecord record = paymentService.createPayment(userId, request);

        String returnUrl = buildReturnUrl("/pay/success");
        String notifyUrl = buildNotifyUrl("/api/v1/payment/alipay/callback");
        
        String form = alipayService.createOrder(
                record.getPaymentNo(),
                record.getAmount(),
                request.getSubject() != null ? request.getSubject() : "Tailor IS 订单支付",
                request.getBody() != null ? request.getBody() : "订单支付",
                returnUrl,
                notifyUrl
        );
        
        log.info("支付宝支付订单创建成功, paymentNo: {}, orderId: {}", record.getPaymentNo(), record.getOrderId());
        return Result.success(form);
    }

    @Operation(summary = "微信支付回调", description = "微信支付 V3 API 异步回调通知")
    @PostMapping("/wechat/callback")
    public String wechatCallback(HttpServletRequest request) {
        try {
            // 读取原始请求体
            String body;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
                body = reader.lines().collect(Collectors.joining("\n"));
            }

            // 获取 V3 签名头
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serialNo = request.getHeader("Wechatpay-Serial");

            // 同时从请求参数中提取业务字段（兼容 V2 参数格式）
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, value) -> {
                params.put(key, value[0]);
            });

            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_state");
            
            if (outTradeNo == null || outTradeNo.isEmpty()) {
                log.warn("微信支付回调缺少out_trade_no参数");
                return buildWechatCallbackResponse(false, "缺少订单号");
            }

            if ("REFUND".equals(tradeStatus)) {
                log.info("微信支付回调-交易已退款, outTradeNo: {}", outTradeNo);
                return buildWechatCallbackResponse(true, "OK");
            }

            if (!"SUCCESS".equals(params.get("result_code"))) {
                log.warn("微信支付回调-业务失败, outTradeNo: {}, errCode: {}, errMsg: {}", 
                        outTradeNo, params.get("err_code"), params.get("err_code_des"));
                return buildWechatCallbackResponse(true, "业务处理中");
            }

            // 使用 V3 API 验签
            if (!wechatPayService.verifyCallback(body, signature, timestamp, nonce, serialNo)) {
                log.warn("微信支付回调验签失败, outTradeNo: {}", outTradeNo);
                return buildWechatCallbackResponse(false, "验签失败");
            }

            String transactionId = params.get("transaction_id");
            String channelResponse = params.toString();

            paymentService.payCallback(outTradeNo, transactionId, channelResponse, null, null);

            log.info("微信支付回调处理成功, outTradeNo: {}, transactionId: {}", outTradeNo, transactionId);
            return buildWechatCallbackResponse(true, "OK");
        } catch (BusinessException e) {
            log.warn("微信支付回调业务异常: {}", e.getMessage());
            return buildWechatCallbackResponse(true, e.getMessage());
        } catch (Exception e) {
            log.error("微信支付回调处理失败", e);
            return buildWechatCallbackResponse(false, "处理失败");
        }
    }

    @Operation(summary = "支付宝支付回调", description = "支付宝支付异步回调通知")
    @PostMapping("/alipay/callback")
    public String alipayCallback(HttpServletRequest request) {
        try {
            Map<String, String> params = new HashMap<>();
            request.getParameterMap().forEach((key, value) -> {
                params.put(key, value[0]);
            });

            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            
            if (outTradeNo == null || outTradeNo.isEmpty()) {
                log.warn("支付宝支付回调缺少out_trade_no参数");
                return "failure";
            }

            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                log.info("支付宝支付回调-非成功状态, outTradeNo: {}, tradeStatus: {}", outTradeNo, tradeStatus);
                return "success";
            }

            if (!alipayService.verifyCallback(params)) {
                log.warn("支付宝支付回调验签失败, outTradeNo: {}", outTradeNo);
                return "failure";
            }

            String transactionId = params.get("trade_no");
            String channelResponse = params.toString();

            paymentService.payCallback(outTradeNo, transactionId, channelResponse, null, null);

            log.info("支付宝支付回调处理成功, outTradeNo: {}, transactionId: {}", outTradeNo, transactionId);
            return "success";
        } catch (BusinessException e) {
            log.warn("支付宝支付回调业务异常: {}", e.getMessage());
            return "success";
        } catch (Exception e) {
            log.error("支付宝支付回调处理失败", e);
            return "failure";
        }
    }

    @Operation(summary = "查询支付状态", description = "根据支付编号查询支付状态")
    @GetMapping("/status")
    public Result<PaymentRecord> getPaymentStatus(@RequestParam String paymentNo) {
        PaymentRecord record = paymentService.getPaymentByPaymentNo(paymentNo);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        return Result.success(record);
    }

    @Operation(summary = "查询微信订单状态", description = "调用微信API查询订单状态")
    @GetMapping("/wechat/query")
    public Result<Map<String, Object>> queryWechatOrder(
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String outTradeNo) {
        Map<String, Object> result = wechatPayService.queryPayment(transactionId, outTradeNo);
        return Result.success(result);
    }

    @Operation(summary = "查询支付宝订单状态", description = "调用支付宝API查询订单状态")
    @GetMapping("/alipay/query")
    public Result<Map<String, Object>> queryAlipayOrder(
            @RequestParam(required = false) String tradeNo,
            @RequestParam(required = false) String outTradeNo) {
        Map<String, Object> result = alipayService.queryOrder(tradeNo, outTradeNo);
        return Result.success(result);
    }

    @Operation(summary = "申请退款", description = "根据支付编号申请退款")
    @PostMapping("/refund")
    public Result<Map<String, Object>> refund(
            @RequestParam String paymentNo,
            @RequestParam BigDecimal refundAmount,
            @RequestParam(required = false) String refundReason) {
        
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        
        PaymentRecord record = paymentService.getPaymentByPaymentNo(paymentNo);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        
        if (record.getPayStatus() != 2) {
            throw new BusinessException("订单未支付，无法退款");
        }
        
        if (refundAmount.compareTo(record.getAmount()) > 0) {
            throw new BusinessException("退款金额不能超过订单金额");
        }
        
        String refundNo = "REF" + System.currentTimeMillis();
        
        Map<String, Object> result;
        if ("wechat".equalsIgnoreCase(record.getPayMethod())) {
            result = wechatPayService.createRefund(paymentNo, refundNo, refundAmount, record.getAmount(), refundReason);
        } else if ("alipay".equalsIgnoreCase(record.getPayMethod())) {
            String tradeNo = alipayService.refund(paymentNo, refundNo, refundAmount, refundReason);
            result = new HashMap<>();
            result.put("trade_no", tradeNo);
            result.put("out_refund_no", refundNo);
        } else {
            throw new BusinessException("不支持的支付方式");
        }
        
        result.put("refundNo", refundNo);
        log.info("退款申请成功, paymentNo: {}, refundNo: {}, amount: {}", paymentNo, refundNo, refundAmount);
        return Result.success(result);
    }

    @Operation(summary = "微信Native支付", description = "创建微信Native扫码支付订单")
    @PostMapping("/wechat/native")
    public Result<Map<String, Object>> wechatNativePay(@RequestBody PayRequest request, HttpServletRequest httpRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        PaymentRecord record = paymentService.createPayment(userId, request);

        String clientIp = getClientIp(httpRequest);
        String notifyUrl = buildNotifyUrl("/api/v1/payment/wechat/callback");

        Map<String, Object> result = wechatPayService.createPayment(
                record.getPaymentNo(),
                record.getAmount(),
                request.getBody() != null ? request.getBody() : "Tailor IS 订单支付",
                clientIp,
                notifyUrl
        );

        result.put("paymentNo", record.getPaymentNo());
        result.put("orderId", record.getOrderId());
        log.info("微信Native支付订单创建成功, paymentNo: {}, orderId: {}", record.getPaymentNo(), record.getOrderId());
        return Result.success(result);
    }

    @Operation(summary = "支付宝扫码支付", description = "创建支付宝扫码支付订单")
    @PostMapping("/alipay/qrcode")
    public Result<String> alipayQrCodePay(@RequestBody PayRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        PaymentRecord record = paymentService.createPayment(userId, request);

        String notifyUrl = buildNotifyUrl("/api/v1/payment/alipay/callback");
        AlipayServiceImpl alipayServiceImpl = (AlipayServiceImpl) alipayService;

        String qrCode = alipayServiceImpl.createQrCodePayment(
                record.getPaymentNo(),
                record.getAmount(),
                request.getSubject() != null ? request.getSubject() : "Tailor IS 订单支付",
                request.getBody() != null ? request.getBody() : "订单支付",
                notifyUrl
        );

        log.info("支付宝扫码支付订单创建成功, paymentNo: {}, orderId: {}", record.getPaymentNo(), record.getOrderId());
        return Result.success(qrCode);
    }

    @Operation(summary = "支付宝APP支付", description = "创建支付宝APP支付订单")
    @PostMapping("/alipay/app")
    public Result<String> alipayAppPay(@RequestBody PayRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();

        PaymentRecord record = paymentService.createPayment(userId, request);

        String notifyUrl = buildNotifyUrl("/api/v1/payment/alipay/callback");
        AlipayServiceImpl alipayServiceImpl = (AlipayServiceImpl) alipayService;

        String orderStr = alipayServiceImpl.createAppPayment(
                record.getPaymentNo(),
                record.getAmount(),
                request.getSubject() != null ? request.getSubject() : "Tailor IS 订单支付",
                request.getBody() != null ? request.getBody() : "订单支付",
                notifyUrl
        );

        log.info("支付宝APP支付订单创建成功, paymentNo: {}, orderId: {}", record.getPaymentNo(), record.getOrderId());
        return Result.success(orderStr);
    }

    @Operation(summary = "关闭支付订单", description = "关闭未支付的微信支付订单")
    @PostMapping("/wechat/close")
    public Result<Boolean> closeWechatPayment(@RequestParam String paymentNo) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }

        PaymentRecord record = paymentService.getPaymentByPaymentNo(paymentNo);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }
        if (!"wechat".equalsIgnoreCase(record.getPayMethod())) {
            throw new BusinessException("仅支持关闭微信支付订单");
        }

        boolean closed = wechatPayService.closePayment(paymentNo);
        return Result.success(closed);
    }

    @Operation(summary = "查询退款状态", description = "查询退款处理状态")
    @GetMapping("/refund/query")
    public Result<Map<String, Object>> queryRefund(
            @RequestParam(required = false) String refundNo,
            @RequestParam String paymentNo) {

        PaymentRecord record = paymentService.getPaymentByPaymentNo(paymentNo);
        if (record == null) {
            throw new BusinessException("支付记录不存在");
        }

        Map<String, Object> result;
        if ("wechat".equalsIgnoreCase(record.getPayMethod())) {
            result = wechatPayService.queryRefund(refundNo);
        } else if ("alipay".equalsIgnoreCase(record.getPayMethod())) {
            AlipayServiceImpl alipayServiceImpl = (AlipayServiceImpl) alipayService;
            result = alipayServiceImpl.queryRefund(paymentNo, refundNo);
        } else {
            throw new BusinessException("不支持的支付方式");
        }

        return Result.success(result);
    }

    private String buildNotifyUrl(String path) {
        return gatewayExternalUrl + path;
    }

    private String buildReturnUrl(String path) {
        return gatewayExternalUrl.replace("api.", "www.") + path;
    }

    private String buildWechatCallbackResponse(boolean success, String message) {
        String code = success ? "SUCCESS" : "FAIL";
        return String.format("<xml><return_code><![CDATA[%s]]></return_code><return_msg><![CDATA[%s]]></return_msg></xml>", code, message);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}