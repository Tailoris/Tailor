package com.tailoris.order.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CreateOrderRequest 单元测试 - 业务模块 order-service
 * 覆盖率: DTO 校验逻辑
 */
class CreateOrderRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("有效创建订单请求 - 应校验通过")
    void validRequest_shouldPass() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCartIds(Arrays.asList(1L, 2L, 3L));
        req.setAddressId(100L);
        req.setCouponId(50L);
        req.setRequestId(UUID.randomUUID().toString());
        req.setRemark("请尽快发货");

        var violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("购物车 ID 列表为空 - 应校验失败")
    void emptyCartIds_shouldFail() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCartIds(Collections.emptyList());
        req.setRequestId(UUID.randomUUID().toString());

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("购物车ID列表不能为空"));
    }

    @Test
    @DisplayName("购物车 ID 列表为 null - 应校验失败")
    void nullCartIds_shouldFail() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCartIds(null);
        req.setRequestId(UUID.randomUUID().toString());

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("购物车ID列表不能为空"));
    }

    @Test
    @DisplayName("请求 ID 为空 - 应校验失败（防重复提交）")
    void emptyRequestId_shouldFail() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCartIds(Arrays.asList(1L));
        req.setRequestId("");

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("请求ID不能为空"));
    }

    @Test
    @DisplayName("请求 ID 为 null - 应校验失败")
    void nullRequestId_shouldFail() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCartIds(Arrays.asList(1L));
        req.setRequestId(null);

        var violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("请求ID不能为空"));
    }

    @Test
    @DisplayName("可选字段 - 全部为 null 应通过")
    void onlyRequiredFields_shouldPass() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCartIds(Arrays.asList(1L));
        req.setRequestId(UUID.randomUUID().toString());

        var violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("完整流程 - 设置所有字段")
    void setAllFields_shouldPersist() {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setCartIds(Arrays.asList(10L, 20L));
        req.setAddressId(1L);
        req.setCouponId(2L);
        req.setPromotionId(3L);
        req.setRemark("测试备注");
        req.setRequestId("req-123");

        assertThat(req.getCartIds()).hasSize(2);
        assertThat(req.getAddressId()).isEqualTo(1L);
        assertThat(req.getCouponId()).isEqualTo(2L);
        assertThat(req.getPromotionId()).isEqualTo(3L);
        assertThat(req.getRemark()).isEqualTo("测试备注");
        assertThat(req.getRequestId()).isEqualTo("req-123");
    }
}
