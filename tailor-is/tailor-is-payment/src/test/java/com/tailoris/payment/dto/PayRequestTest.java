package com.tailoris.payment.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PayRequest 单元测试 - 业务模块 payment-service
 * 覆盖: DTO 校验 + 金额边界
 */
class PayRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("有效支付请求 - 应校验通过")
    void validRequest_shouldPass() {
        PayRequest req = new PayRequest();
        req.setOrderId(123L);
        req.setAmount(new BigDecimal("99.99"));
        req.setPayChannel(1);
        req.setPayMethod("WECHAT_JSAPI");
        req.setDeviceType(3);
        req.setSubject("测试商品");

        var violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("订单 ID 为 null - 应校验失败")
    void nullOrderId_shouldFail() {
        PayRequest req = new PayRequest();
        req.setOrderId(null);
        req.setAmount(new BigDecimal("10"));
        req.setPayChannel(1);

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("订单ID不能为空"));
    }

    @Test
    @DisplayName("支付金额为 0 - 应校验失败（@DecimalMin）")
    void zeroAmount_shouldFail() {
        PayRequest req = new PayRequest();
        req.setOrderId(1L);
        req.setAmount(BigDecimal.ZERO);
        req.setPayChannel(1);

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("支付金额必须大于0"));
    }

    @Test
    @DisplayName("支付金额为负数 - 应校验失败")
    void negativeAmount_shouldFail() {
        PayRequest req = new PayRequest();
        req.setOrderId(1L);
        req.setAmount(new BigDecimal("-10.00"));
        req.setPayChannel(1);

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("支付金额必须大于0"));
    }

    @Test
    @DisplayName("支付金额为 0.01 - 应校验通过（边界）")
    void minAmount_shouldPass() {
        PayRequest req = new PayRequest();
        req.setOrderId(1L);
        req.setAmount(new BigDecimal("0.01"));
        req.setPayChannel(1);

        var violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("支付渠道为 null - 应校验失败")
    void nullPayChannel_shouldFail() {
        PayRequest req = new PayRequest();
        req.setOrderId(1L);
        req.setAmount(new BigDecimal("10"));
        req.setPayChannel(null);

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("支付渠道不能为空"));
    }

    @Test
    @DisplayName("各支付渠道值 - 1~6 应全部接受")
    void allPayChannels_shouldPass() {
        for (int channel = 1; channel <= 6; channel++) {
            PayRequest req = new PayRequest();
            req.setOrderId(1L);
            req.setAmount(new BigDecimal("10"));
            req.setPayChannel(channel);
            var violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }
}
