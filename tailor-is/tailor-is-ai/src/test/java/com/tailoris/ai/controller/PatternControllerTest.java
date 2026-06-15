package com.tailoris.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.ai.dto.*;
import com.tailoris.ai.entity.PatternIteration;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.entity.PatternVersion;
import com.tailoris.ai.service.PatternService;
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

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PatternController 单元测试")
@ExtendWith(MockitoExtension.class)
class PatternControllerTest {

    @Mock
    private PatternService patternService;
    @Mock
    private PatternGenerateServiceImpl patternGenerateService;

    @InjectMocks
    private PatternController patternController;

    private MockMvc mockMvc;
    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(patternController).build();
        stpUtilMock = mockStatic(StpUtil.class);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Test
    @DisplayName("生成版型 - 成功")
    void testGeneratePattern_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        PatternRecord record = new PatternRecord();
        record.setId(1L);
        record.setPatternName("测试版型");
        when(patternService.generatePattern(anyLong(), any(PatternGenerateRequest.class)))
                .thenReturn(record);

        mockMvc.perform(post("/api/v1/ai/pattern/generate")
                        .contentType("application/json")
                        .content("{\"patternName\":\"测试\",\"bodySizeId\":1,\"patternType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("检查版型 - 成功")
    void testCheckPattern_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        when(patternService.checkPattern(anyLong(), any(PatternCheckRequest.class)))
                .thenReturn("版型有效");

        mockMvc.perform(post("/api/v1/ai/pattern/check")
                        .contentType("application/json")
                        .content("{\"patternId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("版型有效"));
    }

    @Test
    @DisplayName("迭代版型 - 成功")
    void testIteratePattern_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        PatternIteration iteration = new PatternIteration();
        iteration.setId(1L);
        iteration.setPatternId(1L);
        when(patternService.iteratePattern(anyLong(), any(PatternIterationRequest.class)))
                .thenReturn(iteration);

        mockMvc.perform(post("/api/v1/ai/pattern/iterate")
                        .contentType("application/json")
                        .content("{\"patternId\":1,\"iterationType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("保存版本 - 成功")
    void testSaveVersion_Success() throws Exception {
        PatternVersion version = new PatternVersion();
        version.setId(1L);
        version.setVersionNo(1);
        when(patternService.saveVersion(anyLong(), anyString(), anyString()))
                .thenReturn(version);

        mockMvc.perform(post("/api/v1/ai/pattern/version/save")
                        .param("patternId", "1")
                        .param("versionName", "V1")
                        .param("changeDescription", "初始版本"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("查询版本列表 - 成功")
    void testListVersions_Success() throws Exception {
        PatternVersion v1 = new PatternVersion();
        v1.setVersionNo(1);
        PatternVersion v2 = new PatternVersion();
        v2.setVersionNo(2);
        when(patternService.listVersions(anyLong())).thenReturn(Arrays.asList(v1, v2));

        mockMvc.perform(get("/api/v1/ai/pattern/versions")
                        .param("patternId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("导出版型 - 成功")
    void testExportPattern_Success() throws Exception {
        when(patternService.exportPattern(anyLong(), anyString())).thenReturn("/path/to/file.svg");

        mockMvc.perform(get("/api/v1/ai/pattern/export")
                        .param("patternId", "1")
                        .param("format", "SVG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("/path/to/file.svg"));
    }

    @Test
    @DisplayName("导出版型 - 默认格式")
    void testExportPattern_DefaultFormat() throws Exception {
        when(patternService.exportPattern(anyLong(), anyString())).thenReturn("/path/to/file.svg");

        mockMvc.perform(get("/api/v1/ai/pattern/export")
                        .param("patternId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("查询版型详情 - 成功")
    void testGetPatternDetail_Success() throws Exception {
        PatternRecord record = new PatternRecord();
        record.setId(1L);
        record.setPatternName("测试版型");
        when(patternService.getPatternDetail(anyLong())).thenReturn(record);

        mockMvc.perform(get("/api/v1/ai/pattern/detail")
                        .param("patternId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patternName").value("测试版型"));
    }

    @Test
    @DisplayName("查询我的版型列表 - 成功")
    void testListMyPatterns_Success() throws Exception {
        stpUtilMock.when(StpUtil::isLogin).thenReturn(true);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);

        PatternRecord r1 = new PatternRecord();
        r1.setId(1L);
        PatternRecord r2 = new PatternRecord();
        r2.setId(2L);
        when(patternService.listUserPatterns(anyLong())).thenReturn(Arrays.asList(r1, r2));

        mockMvc.perform(get("/api/v1/ai/pattern/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("生成SVG纸样 - 成功")
    void testGenerateSvgPattern_Success() throws Exception {
        PatternGenerateResponse response = PatternGenerateResponse.builder()
                .patternId("PAT-123")
                .name("测试纸样")
                .build();
        when(patternGenerateService.generatePattern(any(PatternGenerateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/ai/pattern/generate-svg")
                        .contentType("application/json")
                        .content("{\"patternName\":\"测试\",\"garmentType\":\"SHIRT\",\"bodySizeId\":1,\"patternType\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patternId").value("PAT-123"));
    }

    @Test
    @DisplayName("预览纸样SVG - 成功")
    void testPreviewPattern_Success() throws Exception {
        mockMvc.perform(get("/api/v1/ai/pattern/preview/123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pattern: 123")));
    }
}
