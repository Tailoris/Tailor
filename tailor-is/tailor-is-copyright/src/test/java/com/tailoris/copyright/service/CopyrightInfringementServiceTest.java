package com.tailoris.copyright.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.copyright.entity.CrInfringementCase;
import com.tailoris.copyright.entity.CrInfringementLog;
import com.tailoris.copyright.entity.CopyrightRecord;
import com.tailoris.copyright.mapper.CrInfringementCaseMapper;
import com.tailoris.copyright.mapper.CrInfringementLogMapper;
import com.tailoris.copyright.mapper.CopyrightRecordMapper;
import com.tailoris.copyright.service.SimilarityCheckService.SimilarityResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CopyrightInfringementService 单元测试")
@ExtendWith(MockitoExtension.class)
class CopyrightInfringementServiceTest {

    @Mock
    private CrInfringementCaseMapper caseMapper;

    @Mock
    private CrInfringementLogMapper logMapper;

    @Mock
    private CopyrightRecordMapper copyrightRecordMapper;

    @Mock
    private SimilarityCheckService similarityCheckService;

    @InjectMocks
    private CopyrightInfringementService infringementService;

    @Test
    @DisplayName("创建侵权案件 - 成功")
    void testCreateCase_Success() {
        CopyrightRecord record = new CopyrightRecord();
        record.setId(1L);
        record.setUserId(100L);
        record.setFileHash("testhash");
        record.setAuthorRealName("作者");

        when(copyrightRecordMapper.selectById(1L)).thenReturn(record);
        doReturn(1).when(caseMapper).insert(any(CrInfringementCase.class));
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            SimilarityResult evidence = new SimilarityResult();
            evidence.setScore(85.0);
            evidence.setMethod("pHash");

            CrInfringementCase result = infringementService.createCase(
                    1L, "系统检测", "侵权方", "contact@test.com",
                    1, evidence);

            assertNotNull(result);
            assertEquals(1L, result.getRecordId());
            assertEquals(100L, result.getCopyrightUserId());
            assertEquals(0, result.getStatus());
            assertNotNull(result.getArbitrationDeadline());
            verify(caseMapper, times(1)).insert(any(CrInfringementCase.class));
            verify(logMapper, times(1)).insert(any(CrInfringementLog.class));
        }
    }

    @Test
    @DisplayName("创建侵权案件 - 版权记录不存在")
    void testCreateCase_RecordNotFound() {
        when(copyrightRecordMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                infringementService.createCase(999L, "来源", "侵权方", "联系", 1, null));
    }

    @Test
    @DisplayName("受理案件 - 成功")
    void testAcceptCase_Success() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(0);

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateStatusIfMatch(1L, 0, 1);
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        CrInfringementCase result = infringementService.acceptCase(1L, 99L, "操作员");

        assertNotNull(result);
        assertEquals(1, result.getStatus());
        verify(caseMapper, times(1)).updateStatusIfMatch(1L, 0, 1);
    }

    @Test
    @DisplayName("受理案件 - 案件不存在")
    void testAcceptCase_NotFound() {
        when(caseMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                infringementService.acceptCase(999L, 99L, "操作员"));
    }

    @Test
    @DisplayName("提交仲裁 - 成功")
    void testSubmitArbitration_Success() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(1);
        caseEntity.setArbitrationDeadline(LocalDateTime.now().plusHours(24));

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateStatusIfMatch(1L, 1, 3);
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        CrInfringementCase result = infringementService.submitArbitration(1L, 99L, "操作员", "提交仲裁");

        assertNotNull(result);
        assertEquals(3, result.getStatus());
    }

    @Test
    @DisplayName("提交仲裁 - 超过72小时自动升级")
    void testSubmitArbitration_Overdue() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(1);
        caseEntity.setArbitrationDeadline(LocalDateTime.now().minusHours(1));

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateStatusIfMatch(1L, 1, 4);
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        assertThrows(BusinessException.class, () ->
                infringementService.submitArbitration(1L, 99L, "操作员", "备注"));

        verify(caseMapper, times(1)).updateStatusIfMatch(1L, 1, 4);
    }

    @Test
    @DisplayName("完成仲裁 - 支持侵权")
    void testCompleteArbitration_Support() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(3);

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateStatusIfMatch(1L, 3, 5);
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        CrInfringementCase result = infringementService.completeArbitration(
                1L, 99L, "仲裁员", "判定侵权成立", true);

        assertNotNull(result);
        assertEquals(5, result.getStatus());
        assertEquals(99L, result.getArbitratorId());
        assertNotNull(result.getArbitrationAt());
    }

    @Test
    @DisplayName("完成仲裁 - 不支持侵权")
    void testCompleteArbitration_NotSupport() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(3);

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateStatusIfMatch(1L, 3, 6);
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        CrInfringementCase result = infringementService.completeArbitration(
                1L, 99L, "仲裁员", "证据不足", false);

        assertNotNull(result);
        assertEquals(6, result.getStatus());
    }

    @Test
    @DisplayName("立案诉讼 - 成功")
    void testFileLawsuit_Success() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(4);

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateById(any(CrInfringementCase.class));
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        CrInfringementCase result = infringementService.fileLawsuit(
                1L, "北京市朝阳区法院", "2026京0105民初123号",
                "律师", "13800138000", 99L);

        assertNotNull(result);
        assertEquals(4, result.getStatus());
        assertEquals("北京市朝阳区法院", result.getCourtName());
    }

    @Test
    @DisplayName("法院判决 - 成功")
    void testCourtVerdict_Success() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(4);

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateById(any(CrInfringementCase.class));
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        CrInfringementCase result = infringementService.courtVerdict(
                1L, 5, new BigDecimal("50000.00"), 99L);

        assertNotNull(result);
        assertEquals(5, result.getStatus());
        assertEquals(new BigDecimal("50000.00"), result.getCompensation());
        assertNotNull(result.getClosedAt());
    }

    @Test
    @DisplayName("撤回案件 - 本人撤回")
    void testWithdrawCase_ByOwner() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(1);
        caseEntity.setCopyrightUserId(100L);

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);
        doReturn(1).when(caseMapper).updateById(any(CrInfringementCase.class));
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        CrInfringementCase result = infringementService.withdrawCase(1L, 100L, "双方和解");

        assertNotNull(result);
        assertEquals(8, result.getStatus());
        assertNotNull(result.getClosedAt());
    }

    @Test
    @DisplayName("撤回案件 - 非本人不能撤回")
    void testWithdrawCase_NotOwner() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        caseEntity.setStatus(1);
        caseEntity.setCopyrightUserId(100L);

        when(caseMapper.selectById(1L)).thenReturn(caseEntity);

        assertThrows(BusinessException.class, () ->
                infringementService.withdrawCase(1L, 200L, "原因"));
    }

    @Test
    @DisplayName("扫描超时案件 - 处理超时案件")
    void testScanOverdueArbitration_WithOverdue() {
        CrInfringementCase case1 = new CrInfringementCase();
        case1.setId(1L);
        case1.setCaseNo("IN-001");
        case1.setStatus(3);

        List<CrInfringementCase> overdueList = Arrays.asList(case1);
        when(caseMapper.selectOverdueArbitration()).thenReturn(overdueList);
        doReturn(1).when(caseMapper).updateById(any(CrInfringementCase.class));
        doReturn(1).when(logMapper).insert(any(CrInfringementLog.class));

        infringementService.scanOverdueArbitration();

        verify(caseMapper, times(1)).updateById(argThat((CrInfringementCase c) -> c.getStatus() == 4));
        verify(logMapper, times(1)).insert(any(CrInfringementLog.class));
    }

    @Test
    @DisplayName("扫描超时案件 - 无超时案件")
    void testScanOverdueArbitration_NoOverdue() {
        when(caseMapper.selectOverdueArbitration()).thenReturn(Collections.emptyList());

        infringementService.scanOverdueArbitration();

        verify(caseMapper, never()).updateById(any(CrInfringementCase.class));
    }

    @Test
    @DisplayName("获取案件详情")
    void testGetCaseDetail() {
        CrInfringementCase caseEntity = new CrInfringementCase();
        caseEntity.setId(1L);
        when(caseMapper.selectById(1L)).thenReturn(caseEntity);

        CrInfringementCase result = infringementService.getCaseDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("查询用户案件列表")
    void testListUserCases() {
        Page<CrInfringementCase> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(caseMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CrInfringementCase> result = infringementService.listUserCases(100L, new PageRequest(1, 20));

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("查询案件流转日志")
    void testListCaseLogs() {
        CrInfringementLog log1 = new CrInfringementLog();
        log1.setId(1L);
        List<CrInfringementLog> logs = Arrays.asList(log1);
        when(logMapper.selectByCase(1L)).thenReturn(logs);

        List<CrInfringementLog> result = infringementService.listCaseLogs(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
