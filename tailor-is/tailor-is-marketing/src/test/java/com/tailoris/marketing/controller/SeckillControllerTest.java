package com.tailoris.marketing.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.dto.SeckillCreateRequest;
import com.tailoris.marketing.entity.SeckillActivity;
import com.tailoris.marketing.entity.SeckillProduct;
import com.tailoris.marketing.service.SeckillService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("SeckillController 单元测试")
@ExtendWith(MockitoExtension.class)
class SeckillControllerTest {

    @Mock
    private SeckillService seckillService;

    @InjectMocks
    private SeckillController seckillController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(seckillController).build();
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("创建秒杀活动：成功")
    void testCreateActivity_Success() throws Exception {
        SeckillCreateRequest request = new SeckillCreateRequest();
        request.setName("测试秒杀");
        request.setStartTime(LocalDateTime.now());
        request.setEndTime(LocalDateTime.now().plusDays(1));
        request.setProductId(1L);
        request.setSkuId(1L);
        request.setSeckillPrice(new BigDecimal("9.9"));
        request.setOriginalPrice(new BigDecimal("19.9"));
        request.setStock(100);

        SeckillActivity activity = new SeckillActivity();
        activity.setId(1L);
        activity.setName("测试秒杀");

        when(seckillService.createActivity(any(SeckillCreateRequest.class))).thenReturn(activity);

        mockMvc.perform(post("/api/marketing/seckill/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试秒杀"));
    }

    @Test
    @DisplayName("参与秒杀：成功")
    void testJoinSeckill_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        doNothing().when(seckillService).joinSeckill(anyLong(), anyLong());

        mockMvc.perform(post("/api/marketing/seckill/join")
                        .param("seckillProductId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询秒杀商品列表：成功")
    void testListProducts_Success() throws Exception {
        PageResponse<SeckillProduct> response = new PageResponse<>(Collections.emptyList(), 0, 1, 10);
        when(seckillService.listSeckillProducts(any(PageRequest.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/marketing/seckill/products")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询进行中的秒杀活动：成功")
    void testListActivities_Success() throws Exception {
        when(seckillService.listActiveActivities()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/seckill/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询秒杀商品详情：成功")
    void testGetProductDetail_Success() throws Exception {
        SeckillProduct product = new SeckillProduct();
        product.setId(1L);
        product.setSeckillPrice(new BigDecimal("9.9"));

        when(seckillService.getSeckillProduct(1L)).thenReturn(product);

        mockMvc.perform(get("/api/marketing/seckill/product/detail")
                        .param("seckillProductId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
