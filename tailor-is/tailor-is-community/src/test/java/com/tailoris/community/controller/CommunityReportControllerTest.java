package com.tailoris.community.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.entity.CommunityReportAction;
import com.tailoris.community.service.CommunityReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CommunityReportController 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CommunityReportService reportService;

    @InjectMocks
    private CommunityReportController reportController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).build();
    }

    @Test
    @DisplayName("提交举报成功")
    void testSubmit() throws Exception {
        CommunityReport report = new CommunityReport();
        report.setId(1L);

        when(reportService.submitReport(anyLong(), anyLong(), anyInt(), anyInt(), any(), any()))
                .thenReturn(report);

        mockMvc.perform(post("/api/community/report")
                        .header("X-User-Id", 1L)
                        .param("targetId", "100")
                        .param("targetType", "1")
                        .param("reasonType", "1")
                        .param("reason", "违规内容")
                        .param("evidence", "证据图片"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("获取举报列表")
    void testList() throws Exception {
        PageResponse<CommunityReport> response = new PageResponse<>(Collections.emptyList(), 0L, 1, 20);
        when(reportService.listReports(any(PageRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/community/report/list")
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("处理举报成功")
    void testHandle() throws Exception {
        mockMvc.perform(post("/api/community/report/1/handle")
                        .header("X-User-Id", 1L)
                        .param("actionType", "1")
                        .param("actionReason", "删除违规内容")
                        .param("actionDays", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(reportService).handleReport(1L, 1L, 1, "删除违规内容", 7);
    }

    @Test
    @DisplayName("获取举报处理记录")
    void testActions() throws Exception {
        when(reportService.listActions(anyLong())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/community/report/1/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
