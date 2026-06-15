package com.tailoris.marketing.controller;

import com.tailoris.marketing.entity.MktOrderPromotion;
import com.tailoris.marketing.entity.MktPromotionStats;
import com.tailoris.marketing.service.MktStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("MktStatisticsController 单元测试")
@ExtendWith(MockitoExtension.class)
class MktStatisticsControllerTest {

    @Mock
    private MktStatisticsService statisticsService;

    @InjectMocks
    private MktStatisticsController statisticsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(statisticsController).build();
    }

    @Test
    @DisplayName("记录曝光：成功")
    void testRecordExposure_Success() throws Exception {
        doNothing().when(statisticsService).recordExposure(anyInt(), anyLong(), any());

        mockMvc.perform(post("/api/marketing/stats/exposure")
                        .param("promotionType", "1")
                        .param("promotionId", "100")
                        .param("promotionName", "测试活动"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("记录点击：成功")
    void testRecordClick_Success() throws Exception {
        doNothing().when(statisticsService).recordClick(anyInt(), anyLong(), any());

        mockMvc.perform(post("/api/marketing/stats/click")
                        .param("promotionType", "1")
                        .param("promotionId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("记录参与：成功")
    void testRecordParticipate_Success() throws Exception {
        doNothing().when(statisticsService).recordParticipate(anyInt(), anyLong(), any());

        mockMvc.perform(post("/api/marketing/stats/participate")
                        .param("promotionType", "1")
                        .param("promotionId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("记录订单：成功")
    void testRecordOrder_Success() throws Exception {
        doNothing().when(statisticsService).recordOrder(anyInt(), anyLong(), any(), any(BigDecimal.class), any(BigDecimal.class));

        mockMvc.perform(post("/api/marketing/stats/order")
                        .param("promotionType", "1")
                        .param("promotionId", "100")
                        .param("orderAmount", "100")
                        .param("discountAmount", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询活动统计：成功")
    void testGetPromotionStats_Success() throws Exception {
        when(statisticsService.getPromotionStats(anyInt(), anyLong(), anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/stats/promotion")
                        .param("promotionType", "1")
                        .param("promotionId", "100")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询营销大盘：成功")
    void testGetMarketingOverview_Success() throws Exception {
        when(statisticsService.getMarketingOverview(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/stats/overview")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询热门活动：成功")
    void testGetTopPromotions_Success() throws Exception {
        when(statisticsService.getTopPromotions(any(LocalDate.class), any(LocalDate.class), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/stats/top")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("更新ROI：成功")
    void testUpdateRoi_Success() throws Exception {
        doNothing().when(statisticsService).updateRoi(anyInt(), anyLong(), any(BigDecimal.class));

        mockMvc.perform(post("/api/marketing/stats/roi")
                        .param("promotionType", "1")
                        .param("promotionId", "100")
                        .param("cost", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("记录订单营销关联：成功")
    void testRecordOrderPromotion_Success() throws Exception {
        MktOrderPromotion promotion = new MktOrderPromotion();
        promotion.setId(1L);

        when(statisticsService.recordOrderPromotion(anyLong(), anyInt(), anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(promotion);

        mockMvc.perform(post("/api/marketing/stats/order-promotion")
                        .param("orderId", "100")
                        .param("promotionType", "1")
                        .param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询订单营销记录：成功")
    void testGetOrderPromotions_Success() throws Exception {
        when(statisticsService.getOrderPromotions(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/stats/order-promotion/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
