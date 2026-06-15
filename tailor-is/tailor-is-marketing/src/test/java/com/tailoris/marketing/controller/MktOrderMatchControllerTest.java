package com.tailoris.marketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.marketing.service.MktOrderMatchService;
import com.tailoris.marketing.service.MktOrderMatchService.OrderDiscountPlan;
import com.tailoris.marketing.service.MktOrderMatchService.OrderItemInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MktOrderMatchController 单元测试")
@ExtendWith(MockitoExtension.class)
class MktOrderMatchControllerTest {

    @Mock
    private MktOrderMatchService orderMatchService;

    @InjectMocks
    private MktOrderMatchController orderMatchController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderMatchController).build();
    }

    @Test
    @DisplayName("计算订单最优优惠方案：成功")
    void testCalculate_Success() throws Exception {
        OrderDiscountPlan plan = new OrderDiscountPlan();
        plan.setTotalDiscount(new BigDecimal("10"));
        plan.setDescription("总优惠10元");

        when(orderMatchService.calculateOptimal(any(), any(), any(BigDecimal.class))).thenReturn(plan);

        MktOrderMatchController.CalculateRequest request = new MktOrderMatchController.CalculateRequest();
        request.setTotalAmount(new BigDecimal("100"));
        request.setItems(Arrays.asList());

        mockMvc.perform(post("/api/marketing/order-match/calculate")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("仅优惠券最优：成功")
    void testCouponOnly_Success() throws Exception {
        OrderDiscountPlan plan = new OrderDiscountPlan();
        plan.setTotalDiscount(new BigDecimal("5"));

        when(orderMatchService.calculateCouponOnly(any(), any(BigDecimal.class))).thenReturn(plan);

        mockMvc.perform(get("/api/marketing/order-match/coupon-only")
                        .header("X-User-Id", 1L)
                        .param("orderAmount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
