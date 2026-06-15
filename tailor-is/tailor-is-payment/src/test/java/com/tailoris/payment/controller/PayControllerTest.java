package com.tailoris.payment.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.exception.GlobalExceptionHandler;
import com.tailoris.payment.dto.PayRequest;
import com.tailoris.payment.entity.PaymentRecord;
import com.tailoris.payment.service.AlipayService;
import com.tailoris.payment.service.PaymentService;
import com.tailoris.payment.service.WechatPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PayController 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private WechatPayService wechatPayService;

    @Mock
    private AlipayService alipayService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PayController payController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> mockedStpUtil;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(payController, "gatewayExternalUrl", "https://api.tailoris.com");
        mockMvc = MockMvcBuilders.standaloneSetup(payController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mockedStpUtil = mockStatic(StpUtil.class);
        mockedStpUtil.when(StpUtil::isLogin).thenReturn(true);
        mockedStpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        mockedStpUtil.close();
    }

    @Test
    @DisplayName("微信支付-创建成功")
    void testWechatPay_Success() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setOrderId(1001L);
        record.setAmount(new BigDecimal("99.00"));

        when(paymentService.createPayment(anyLong(), any(PayRequest.class))).thenReturn(record);
        when(wechatPayService.createOrder(anyString(), any(BigDecimal.class), anyString(),
                anyString(), anyString(), anyString())).thenReturn(new HashMap<>());

        PayRequest request = new PayRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("99.00"));
        request.setPayChannel(1);
        request.setPayMethod("wechat");
        request.setOpenId("oTestOpenId");
        request.setBody("测试商品");

        mockMvc.perform(post("/api/v1/payment/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("微信支付-使用默认body")
    void testWechatPay_DefaultBody() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setOrderId(1001L);
        record.setAmount(new BigDecimal("99.00"));

        when(paymentService.createPayment(anyLong(), any(PayRequest.class))).thenReturn(record);
        when(wechatPayService.createOrder(anyString(), any(BigDecimal.class), any(),
                anyString(), anyString(), anyString())).thenReturn(new HashMap<>());

        PayRequest request = new PayRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("99.00"));
        request.setPayChannel(1);

        mockMvc.perform(post("/api/v1/payment/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("支付宝支付-创建成功")
    void testAlipayPay_Success() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY789012");
        record.setOrderId(2001L);
        record.setAmount(new BigDecimal("199.00"));

        when(paymentService.createPayment(anyLong(), any(PayRequest.class))).thenReturn(record);
        when(alipayService.createOrder(anyString(), any(BigDecimal.class), anyString(),
                anyString(), anyString(), anyString())).thenReturn("<form>alipay form</form>");

        PayRequest request = new PayRequest();
        request.setOrderId(2001L);
        request.setAmount(new BigDecimal("199.00"));
        request.setPayChannel(2);
        request.setSubject("测试商品标题");
        request.setBody("测试商品描述");

        mockMvc.perform(post("/api/v1/payment/alipay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("支付宝支付-使用默认subject和body")
    void testAlipayPay_DefaultSubjectBody() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY789012");
        record.setOrderId(2001L);
        record.setAmount(new BigDecimal("199.00"));

        when(paymentService.createPayment(anyLong(), any(PayRequest.class))).thenReturn(record);
        when(alipayService.createOrder(anyString(), any(BigDecimal.class), any(),
                any(), anyString(), anyString())).thenReturn("<form>alipay form</form>");

        PayRequest request = new PayRequest();
        request.setOrderId(2001L);
        request.setAmount(new BigDecimal("199.00"));
        request.setPayChannel(2);

        mockMvc.perform(post("/api/v1/payment/alipay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("查询支付状态-成功")
    void testGetPaymentStatus_Success() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setPayStatus(2);

        when(paymentService.getPaymentByPaymentNo("PAY123456")).thenReturn(record);

        mockMvc.perform(get("/api/v1/payment/status")
                        .param("paymentNo", "PAY123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询支付状态-记录不存在")
    void testGetPaymentStatus_NotFound() throws Exception {
        when(paymentService.getPaymentByPaymentNo("PAY_NOT_EXIST")).thenReturn(null);

        mockMvc.perform(get("/api/v1/payment/status")
                        .param("paymentNo", "PAY_NOT_EXIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @DisplayName("查询微信订单状态")
    void testQueryWechatOrder() throws Exception {
        Map<String, Object> queryResult = new HashMap<>();
        queryResult.put("trade_state", "SUCCESS");
        when(wechatPayService.queryOrder(anyString(), any())).thenReturn(queryResult);

        mockMvc.perform(get("/api/v1/payment/wechat/query")
                        .param("transactionId", "TXN123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询支付宝订单状态")
    void testQueryAlipayOrder() throws Exception {
        Map<String, Object> queryResult = new HashMap<>();
        queryResult.put("trade_status", "TRADE_SUCCESS");
        when(alipayService.queryOrder(anyString(), any())).thenReturn(queryResult);

        mockMvc.perform(get("/api/v1/payment/alipay/query")
                        .param("tradeNo", "ALIPAY123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("申请退款-微信退款成功")
    void testRefund_WechatSuccess() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setAmount(new BigDecimal("100.00"));
        record.setPayStatus(2);
        record.setPayMethod("wechat");

        when(paymentService.getPaymentByPaymentNo("PAY123456")).thenReturn(record);

        Map<String, Object> refundResult = new HashMap<>();
        refundResult.put("refund_id", "REF123");
        when(wechatPayService.refund(anyString(), anyString(), any(BigDecimal.class), any(BigDecimal.class)))
                .thenReturn(refundResult);

        mockMvc.perform(post("/api/v1/payment/refund")
                        .param("paymentNo", "PAY123456")
                        .param("refundAmount", "50.00")
                        .param("refundReason", "测试退款"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("申请退款-支付宝退款成功")
    void testRefund_AlipaySuccess() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setAmount(new BigDecimal("100.00"));
        record.setPayStatus(2);
        record.setPayMethod("alipay");

        when(paymentService.getPaymentByPaymentNo("PAY123456")).thenReturn(record);
        when(alipayService.refund(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn("ALIPAY_TRADE_NO");

        mockMvc.perform(post("/api/v1/payment/refund")
                        .param("paymentNo", "PAY123456")
                        .param("refundAmount", "50.00")
                        .param("refundReason", "测试退款"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("申请退款-支付记录不存在")
    void testRefund_RecordNotFound() throws Exception {
        when(paymentService.getPaymentByPaymentNo("PAY_NOT_EXIST")).thenReturn(null);

        mockMvc.perform(post("/api/v1/payment/refund")
                        .param("paymentNo", "PAY_NOT_EXIST")
                        .param("refundAmount", "50.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @DisplayName("申请退款-订单未支付")
    void testRefund_OrderNotPaid() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setAmount(new BigDecimal("100.00"));
        record.setPayStatus(0);

        when(paymentService.getPaymentByPaymentNo("PAY123456")).thenReturn(record);

        mockMvc.perform(post("/api/v1/payment/refund")
                        .param("paymentNo", "PAY123456")
                        .param("refundAmount", "50.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @DisplayName("申请退款-退款金额超过订单金额")
    void testRefund_AmountExceeds() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setAmount(new BigDecimal("100.00"));
        record.setPayStatus(2);

        when(paymentService.getPaymentByPaymentNo("PAY123456")).thenReturn(record);

        mockMvc.perform(post("/api/v1/payment/refund")
                        .param("paymentNo", "PAY123456")
                        .param("refundAmount", "200.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @DisplayName("申请退款-不支持的支付方式")
    void testRefund_UnsupportedPayMethod() throws Exception {
        PaymentRecord record = new PaymentRecord();
        record.setPaymentNo("PAY123456");
        record.setAmount(new BigDecimal("100.00"));
        record.setPayStatus(2);
        record.setPayMethod("bank");

        when(paymentService.getPaymentByPaymentNo("PAY123456")).thenReturn(record);

        mockMvc.perform(post("/api/v1/payment/refund")
                        .param("paymentNo", "PAY123456")
                        .param("refundAmount", "50.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @DisplayName("微信支付回调-成功处理")
    void testWechatCallback_Success() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("trade_state", new String[]{"SUCCESS"});
        paramMap.put("result_code", new String[]{"SUCCESS"});
        paramMap.put("transaction_id", new String[]{"TXN123456"});
        paramMap.put("sign", new String[]{"VALID_SIGN"});
        paramMap.put("sign_type", new String[]{"MD5"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);
        when(wechatPayService.verifyCallback(anyMap())).thenReturn(true);

        String result = payController.wechatCallback(mockRequest);
        assertTrue(result.contains("SUCCESS"));
    }

    @Test
    @DisplayName("微信支付回调-缺少订单号")
    void testWechatCallback_MissingOrderNo() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("trade_state", new String[]{"SUCCESS"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);

        String result = payController.wechatCallback(mockRequest);
        assertTrue(result.contains("FAIL"));
    }

    @Test
    @DisplayName("微信支付回调-退款状态")
    void testWechatCallback_RefundStatus() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("trade_state", new String[]{"REFUND"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);

        String result = payController.wechatCallback(mockRequest);
        assertTrue(result.contains("SUCCESS"));
    }

    @Test
    @DisplayName("微信支付回调-业务失败")
    void testWechatCallback_BusinessFail() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("trade_state", new String[]{"NOTPAY"});
        paramMap.put("result_code", new String[]{"FAIL"});
        paramMap.put("err_code", new String[]{"SYSTEMERROR"});
        paramMap.put("err_code_des", new String[]{"系统错误"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);

        String result = payController.wechatCallback(mockRequest);
        assertTrue(result.contains("SUCCESS"));
    }

    @Test
    @DisplayName("微信支付回调-验签失败")
    void testWechatCallback_VerifyFail() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("result_code", new String[]{"SUCCESS"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);
        when(wechatPayService.verifyCallback(anyMap())).thenReturn(false);

        String result = payController.wechatCallback(mockRequest);
        assertTrue(result.contains("FAIL"));
    }

    @Test
    @DisplayName("支付宝回调-成功处理")
    void testAlipayCallback_Success() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("trade_status", new String[]{"TRADE_SUCCESS"});
        paramMap.put("trade_no", new String[]{"ALIPAY_TXN123"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);
        when(alipayService.verifyCallback(anyMap())).thenReturn(true);

        String result = payController.alipayCallback(mockRequest);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("支付宝回调-缺少订单号")
    void testAlipayCallback_MissingOrderNo() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("trade_status", new String[]{"TRADE_SUCCESS"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);

        String result = payController.alipayCallback(mockRequest);
        assertEquals("failure", result);
    }

    @Test
    @DisplayName("支付宝回调-非成功状态")
    void testAlipayCallback_NonSuccessStatus() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("trade_status", new String[]{"WAIT_BUYER_PAY"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);

        String result = payController.alipayCallback(mockRequest);
        assertEquals("success", result);
    }

    @Test
    @DisplayName("支付宝回调-验签失败")
    void testAlipayCallback_VerifyFail() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("trade_status", new String[]{"TRADE_SUCCESS"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);
        when(alipayService.verifyCallback(anyMap())).thenReturn(false);

        String result = payController.alipayCallback(mockRequest);
        assertEquals("failure", result);
    }

    @Test
    @DisplayName("支付宝回调-TRADE_FINISHED状态")
    void testAlipayCallback_TradeFinished() throws Exception {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", new String[]{"PAY123456"});
        paramMap.put("trade_status", new String[]{"TRADE_FINISHED"});
        paramMap.put("trade_no", new String[]{"ALIPAY_TXN123"});

        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameterMap()).thenReturn(paramMap);
        when(alipayService.verifyCallback(anyMap())).thenReturn(true);

        String result = payController.alipayCallback(mockRequest);
        assertEquals("success", result);
    }
}
