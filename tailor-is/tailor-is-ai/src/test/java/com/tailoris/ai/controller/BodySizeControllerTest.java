package com.tailoris.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.ai.dto.BodySizeAnalysisRequest;
import com.tailoris.ai.dto.BodySizeAnalysisResponse;
import com.tailoris.ai.dto.SizeDataRequest;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.service.BodySizeService;
import com.tailoris.ai.service.impl.PatternGenerateServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BodySizeController 单元测试")
@ExtendWith(MockitoExtension.class)
class BodySizeControllerTest {

    @Mock
    private BodySizeService bodySizeService;
    @Mock
    private PatternGenerateServiceImpl patternGenerateService;

    @InjectMocks
    private BodySizeController bodySizeController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bodySizeController).build();
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("保存体型数据 - 成功")
    void testSaveSizeData_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        BodySizeData data = new BodySizeData();
        data.setId(1L);
        data.setSizeName("标准体型");
        when(bodySizeService.manageSizeData(anyLong(), any(SizeDataRequest.class))).thenReturn(data);

        mockMvc.perform(post("/api/v1/ai/size/save")
                        .contentType("application/json")
                        .content("{\"sizeName\":\"标准体型\",\"height\":170,\"gender\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询体型数据详情 - 成功")
    void testGetSizeData_Success() throws Exception {
        BodySizeData data = new BodySizeData();
        data.setId(1L);
        data.setSizeName("标准体型");
        when(bodySizeService.getSizeData(anyLong())).thenReturn(data);

        mockMvc.perform(get("/api/v1/ai/size/detail")
                        .param("sizeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sizeName").value("标准体型"));
    }

    @Test
    @DisplayName("查询我的体型数据列表 - 成功")
    void testListMySizeData_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        BodySizeData data1 = new BodySizeData();
        data1.setSizeName("标准");
        BodySizeData data2 = new BodySizeData();
        data2.setSizeName("宽松");
        when(bodySizeService.listUserSizeData(anyLong())).thenReturn(Arrays.asList(data1, data2));

        mockMvc.perform(get("/api/v1/ai/size/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("按体型分类搜索 - 成功")
    void testSearchByBodyType_Success() throws Exception {
        BodySizeData data = new BodySizeData();
        data.setSizeName("标准体型");
        when(bodySizeService.searchByBodyType(anyString(), anyInt())).thenReturn(Arrays.asList(data));

        mockMvc.perform(get("/api/v1/ai/size/search")
                        .param("bodyType", "normal")
                        .param("gender", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("按体型分类搜索 - 无参数")
    void testSearchByBodyType_NoParams() throws Exception {
        when(bodySizeService.searchByBodyType(isNull(), isNull())).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/v1/ai/size/search"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("设置默认体型 - 成功")
    void testSetDefaultSize_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        doNothing().when(bodySizeService).setDefaultSize(anyLong(), anyLong());

        mockMvc.perform(put("/api/v1/ai/size/set-default")
                        .param("sizeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除体型数据 - 成功")
    void testDeleteSizeData_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        doNothing().when(bodySizeService).deleteSizeData(anyLong(), anyLong());

        mockMvc.perform(delete("/api/v1/ai/size/delete")
                        .param("sizeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("AI体型分析 - 成功")
    void testAnalyzeBodySize_Success() throws Exception {
        BodySizeAnalysisResponse response = BodySizeAnalysisResponse.builder()
                .height(new BigDecimal("170"))
                .weight(new BigDecimal("65"))
                .build();
        when(patternGenerateService.analyzeBodySize(any(BodySizeAnalysisRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/size/analyze")
                        .contentType("application/json")
                        .content("{\"height\":170,\"weight\":65}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
