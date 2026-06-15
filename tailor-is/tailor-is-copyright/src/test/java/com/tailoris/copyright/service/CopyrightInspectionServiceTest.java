package com.tailoris.copyright.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.entity.CrInspectionTask;
import com.tailoris.copyright.entity.CrViolationHandling;
import com.tailoris.copyright.mapper.CopyrightRecordMapper;
import com.tailoris.copyright.mapper.CrInspectionTaskMapper;
import com.tailoris.copyright.mapper.CrViolationHandlingMapper;
import com.tailoris.copyright.service.SimilarityCheckService.SimilarityResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CopyrightInspectionService 单元测试")
@ExtendWith(MockitoExtension.class)
class CopyrightInspectionServiceTest {

    @Mock
    private CrInspectionTaskMapper inspectionTaskMapper;
    @Mock
    private CrViolationHandlingMapper violationHandlingMapper;
    @Mock
    private SimilarityCheckService similarityCheckService;
    @Mock
    private CopyrightRecordMapper copyrightRecordMapper;

    @InjectMocks
    private CopyrightInspectionService inspectionService;

    @Test
    @DisplayName("单条记录巡检 - 违规（高相似度）")
    void testCheckSingleRecord_Violation() {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileUrl("http://example.com/file.png");
        record.setFileHash("hash123");
        record.setFileType("image/png");

        SimilarityResult simResult = new SimilarityResult();
        simResult.setScore(90.0);
        simResult.setRiskLevel(4);
        simResult.setEvidenceImageUrl("http://example.com/evidence.jpg");

        when(similarityCheckService.preCheck(anyString(), anyString(), anyString())).thenReturn(simResult);
        when(violationHandlingMapper.insert(any(CrViolationHandling.class))).thenReturn(1);

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            boolean result = inspectionService.checkSingleRecord(record, 100L);

            assertTrue(result);
            verify(violationHandlingMapper, times(1)).insert(any(CrViolationHandling.class));
        }
    }

    @Test
    @DisplayName("单条记录巡检 - 通过（低相似度）")
    void testCheckSingleRecord_Pass() {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileUrl("http://example.com/file.png");
        record.setFileHash("hash123");
        record.setFileType("image/png");

        SimilarityResult simResult = new SimilarityResult();
        simResult.setScore(30.0);
        simResult.setRiskLevel(0);

        when(similarityCheckService.preCheck(anyString(), anyString(), anyString())).thenReturn(simResult);

        boolean result = inspectionService.checkSingleRecord(record, 100L);

        assertFalse(result);
        verify(violationHandlingMapper, never()).insert(any(CrViolationHandling.class));
    }

    @Test
    @DisplayName("单条记录巡检 - 空记录返回false")
    void testCheckSingleRecord_NullRecord() {
        boolean result = inspectionService.checkSingleRecord(null, 100L);
        assertFalse(result);
    }

    @Test
    @DisplayName("单条记录巡检 - 空文件URL返回false")
    void testCheckSingleRecord_NullFileUrl() {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileUrl(null);

        boolean result = inspectionService.checkSingleRecord(record, 100L);
        assertFalse(result);
    }

    @Test
    @DisplayName("处置违规 - 成功")
    void testHandleViolation_Success() {
        CrViolationHandling violation = new CrViolationHandling();
        violation.setId(1L);
        violation.setStatus(0);

        when(violationHandlingMapper.selectById(1L)).thenReturn(violation);
        when(violationHandlingMapper.updateById(any(CrViolationHandling.class))).thenReturn(1);

        CrViolationHandling result = inspectionService.handleViolation(1L, 99L, 1, "确认侵权，下架处理");

        assertNotNull(result);
        assertEquals(1, result.getStatus());
        assertEquals(99L, result.getHandlerId());
        assertEquals(1, result.getHandleType());
        assertEquals("确认侵权，下架处理", result.getHandleRemark());
    }

    @Test
    @DisplayName("处置违规 - 记录不存在抛异常")
    void testHandleViolation_NotFound() {
        when(violationHandlingMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                inspectionService.handleViolation(999L, 99L, 1, "备注"));
    }

    @Test
    @DisplayName("待办违规列表 - 分页查询")
    void testListPending() {
        Page<CrViolationHandling> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(violationHandlingMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResponse<CrViolationHandling> result = inspectionService.listPending(new PageRequest(1, 20));

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("巡检任务列表 - 分页查询")
    void testListTasks() {
        Page<CrInspectionTask> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(inspectionTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResponse<CrInspectionTask> result = inspectionService.listTasks(new PageRequest(1, 20), null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("巡检任务列表 - 按类型和状态过滤")
    void testListTasks_Filtered() {
        Page<CrInspectionTask> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);

        when(inspectionTaskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResponse<CrInspectionTask> result = inspectionService.listTasks(new PageRequest(1, 20), 1, 2);

        assertNotNull(result);
    }

    @Test
    @DisplayName("执行机器巡检 - 成功")
    void testRunMachineInspection_Success() {
        when(inspectionTaskMapper.insert(any(CrInspectionTask.class))).thenReturn(1);
        when(inspectionTaskMapper.updateById(any(CrInspectionTask.class))).thenReturn(1);

        Page<CopyrightRecord> samplePage = new Page<>();
        samplePage.setRecords(Collections.emptyList());
        samplePage.setTotal(0);
        when(copyrightRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(samplePage);

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            int count = inspectionService.runMachineInspection(1, "测试巡检", 10);

            assertEquals(0, count);
        }
    }

    @Test
    @DisplayName("执行机器巡检 - 发现违规")
    void testRunMachineInspection_WithViolations() {
        when(inspectionTaskMapper.insert(any(CrInspectionTask.class))).thenReturn(1);
        when(inspectionTaskMapper.updateById(any(CrInspectionTask.class))).thenReturn(1);

        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setFileUrl("http://example.com/file.png");
        record.setFileHash("hash123");
        record.setFileType("image/png");

        Page<CopyrightRecord> samplePage = new Page<>();
        samplePage.setRecords(Arrays.asList(record));
        samplePage.setTotal(1);
        when(copyrightRecordMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(samplePage);

        SimilarityResult simResult = new SimilarityResult();
        simResult.setScore(95.0);
        simResult.setRiskLevel(4);
        simResult.setEvidenceImageUrl("http://example.com/evidence.jpg");
        when(similarityCheckService.preCheck(anyString(), anyString(), anyString())).thenReturn(simResult);
        when(violationHandlingMapper.insert(any(CrViolationHandling.class))).thenReturn(1);

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            int count = inspectionService.runMachineInspection(1, "测试巡检", 10);

            assertEquals(1, count);
        }
    }
}
