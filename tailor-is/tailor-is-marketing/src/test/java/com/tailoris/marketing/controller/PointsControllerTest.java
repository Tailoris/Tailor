package com.tailoris.marketing.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.dto.PointsExchangeRequest;
import com.tailoris.marketing.entity.PointsMallProduct;
import com.tailoris.marketing.entity.PointsRecord;
import com.tailoris.marketing.service.PointsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PointsController 单元测试")
@ExtendWith(MockitoExtension.class)
class PointsControllerTest {

    @Mock
    private PointsService pointsService;

    @InjectMocks
    private PointsController pointsController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pointsController).build();
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("兑换积分商品：成功")
    void testExchangePoints_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        PointsExchangeRequest request = new PointsExchangeRequest();
        request.setProductId(1L);
        request.setQuantity(1);

        doNothing().when(pointsService).exchangePoints(anyLong(), any(PointsExchangeRequest.class));

        mockMvc.perform(post("/api/marketing/points/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询积分余额：成功")
    void testGetPointsBalance_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        when(pointsService.getPointsBalance(anyLong())).thenReturn(1000);

        mockMvc.perform(get("/api/marketing/points/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(1000));
    }

    @Test
    @DisplayName("查询积分记录：成功")
    void testGetPointsHistory_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        PageResponse<PointsRecord> response = new PageResponse<>(Collections.emptyList(), 0, 1, 10);
        when(pointsService.getPointsHistory(anyLong(), any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/marketing/points/history")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询积分商城商品列表：成功")
    void testListMallProducts_Success() throws Exception {
        PageResponse<PointsMallProduct> response = new PageResponse<>(Collections.emptyList(), 0, 1, 10);
        when(pointsService.listPointsMallProducts(any(PageRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/marketing/points/mall")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
