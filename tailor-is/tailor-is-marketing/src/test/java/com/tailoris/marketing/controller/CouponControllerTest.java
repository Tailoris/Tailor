package com.tailoris.marketing.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.marketing.dto.CouponCreateRequest;
import com.tailoris.marketing.dto.CouponReceiveRequest;
import com.tailoris.marketing.entity.CouponTemplate;
import com.tailoris.marketing.entity.UserCoupon;
import com.tailoris.marketing.service.CouponService;
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

@DisplayName("CouponController 单元测试")
@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(couponController).build();
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("创建优惠券：成功")
    void testCreateCoupon_Success() throws Exception {
        CouponCreateRequest request = new CouponCreateRequest();
        request.setName("测试券");
        request.setType(1);
        request.setDiscountType(1);
        request.setDiscountValue(new BigDecimal("10"));
        request.setTotalCount(100);
        request.setStartTime(LocalDateTime.now());
        request.setEndTime(LocalDateTime.now().plusDays(7));

        CouponTemplate template = new CouponTemplate();
        template.setId(1L);
        template.setName("测试券");

        when(couponService.createCoupon(any(CouponCreateRequest.class))).thenReturn(template);

        mockMvc.perform(post("/api/marketing/coupon/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试券"));
    }

    @Test
    @DisplayName("领取优惠券：成功")
    void testReceiveCoupon_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        CouponReceiveRequest request = new CouponReceiveRequest();
        request.setCouponId(1L);

        doNothing().when(couponService).receiveCoupon(anyLong(), anyLong());

        mockMvc.perform(post("/api/marketing/coupon/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("使用优惠券：成功")
    void testUseCoupon_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        doNothing().when(couponService).useCoupon(anyLong(), anyLong(), anyLong());

        mockMvc.perform(post("/api/marketing/coupon/use")
                        .param("userCouponId", "1")
                        .param("orderId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询优惠券列表：成功")
    void testListCoupons_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        PageResponse<UserCoupon> response = new PageResponse<>(Collections.emptyList(), 0, 1, 10);
        when(couponService.listCoupons(anyLong(), any(PageRequest.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/marketing/coupon/list")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询可用优惠券：成功")
    void testGetAvailableCoupons_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

        when(couponService.getAvailableCoupons(anyLong(), any(BigDecimal.class), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/marketing/coupon/available")
                        .param("orderAmount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询优惠券详情：成功")
    void testGetCouponDetail_Success() throws Exception {
        CouponTemplate template = new CouponTemplate();
        template.setId(1L);
        template.setName("测试券");

        when(couponService.getCouponTemplate(1L)).thenReturn(template);

        mockMvc.perform(get("/api/marketing/coupon/detail")
                        .param("couponId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试券"));
    }
}
