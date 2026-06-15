package com.tailoris.marketing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.entity.MktPromotionRule;
import com.tailoris.marketing.entity.MktPromotionStep;
import com.tailoris.marketing.service.MktPromotionService;
import com.tailoris.marketing.service.MktPromotionService.PromotionDiscountResult;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MktPromotionController 单元测试")
@ExtendWith(MockitoExtension.class)
class MktPromotionControllerTest {

    @Mock
    private MktPromotionService promotionService;

    @InjectMocks
    private MktPromotionController promotionController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(promotionController).build();
    }

    @Test
    @DisplayName("创建促销：成功")
    void testCreatePromotion_Success() throws Exception {
        MktPromotionRule rule = new MktPromotionRule();
        rule.setId(1L);
        rule.setPromotionName("满100减10");

        when(promotionService.createPromotion(any(MktPromotionRule.class), any())).thenReturn(rule);

        MktPromotionController.PromotionCreateRequest request = new MktPromotionController.PromotionCreateRequest();
        request.setRule(rule);
        request.setSteps(Arrays.asList());

        mockMvc.perform(post("/api/marketing/promotion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("更新促销：成功")
    void testUpdatePromotion_Success() throws Exception {
        MktPromotionRule rule = new MktPromotionRule();
        rule.setPromotionName("更新促销");

        when(promotionService.updatePromotion(any(MktPromotionRule.class), any())).thenReturn(rule);

        MktPromotionController.PromotionCreateRequest request = new MktPromotionController.PromotionCreateRequest();
        request.setRule(rule);
        request.setSteps(Arrays.asList());

        mockMvc.perform(put("/api/marketing/promotion/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("取消促销：成功")
    void testCancelPromotion_Success() throws Exception {
        doNothing().when(promotionService).cancelPromotion(1L);

        mockMvc.perform(post("/api/marketing/promotion/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询促销详情：成功")
    void testGetPromotionDetail_Success() throws Exception {
        MktPromotionRule rule = new MktPromotionRule();
        rule.setId(1L);
        rule.setPromotionName("满100减10");

        when(promotionService.getPromotionDetail(1L)).thenReturn(rule);

        mockMvc.perform(get("/api/marketing/promotion/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.promotionName").value("满100减10"));
    }

    @Test
    @DisplayName("查询促销列表：成功")
    void testListPromotions_Success() throws Exception {
        PageResponse<MktPromotionRule> response = new PageResponse<>(Collections.emptyList(), 0, 1, 20);
        when(promotionService.listPromotions(any(PageRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/marketing/promotion/list")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询生效中的促销：成功")
    void testListActiveRules_Success() throws Exception {
        when(promotionService.listActiveRules(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/promotion/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("计算最优优惠：成功")
    void testCalculate_Success() throws Exception {
        PromotionDiscountResult result = new PromotionDiscountResult();
        result.setDiscountAmount(new BigDecimal("10"));

        when(promotionService.calculateOptimalDiscount(any(), any(BigDecimal.class), anyInt())).thenReturn(result);

        mockMvc.perform(post("/api/marketing/promotion/calculate")
                        .param("totalAmount", "100")
                        .param("itemCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
