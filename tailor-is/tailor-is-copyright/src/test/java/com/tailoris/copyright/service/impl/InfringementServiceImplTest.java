package com.tailoris.copyright.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.copyright.dto.ArbitrationRequest;
import com.tailoris.copyright.dto.InfringementReportRequest;
import com.tailoris.copyright.entity.ArbitrationRecord;
import com.tailoris.copyright.entity.InfringementRecord;
import com.tailoris.copyright.mapper.ArbitrationRecordMapper;
import com.tailoris.copyright.mapper.InfringementRecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("InfringementServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class InfringementServiceImplTest {

    @Mock
    private InfringementRecordMapper infringementRecordMapper;
    @Mock
    private ArbitrationRecordMapper arbitrationRecordMapper;

    @InjectMocks
    private InfringementServiceImpl infringementService;

    @Test
    @DisplayName("举报侵权 - 成功")
    void testReportInfringement_Success() {
        doReturn(1).when(infringementRecordMapper).insert(any(InfringementRecord.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            InfringementReportRequest request = new InfringementReportRequest();
            request.setCopyrightId(100L);
            request.setReportedProductId(200L);
            request.setReportedUserId(300L);
            request.setInfringementType(2); // 2=抄袭设计
            request.setReason("完全抄袭我的设计");
            request.setUrgency(2);

            InfringementRecord result = infringementService.reportInfringement(1L, request);

            assertNotNull(result);
            assertEquals(1L, result.getReporterId());
            assertEquals(100L, result.getCopyrightId());
            assertEquals(0, result.getStatus());
        }
    }

    @Test
    @DisplayName("处理举报 - 成功")
    void testProcessReport_Success() {
        InfringementRecord record = new InfringementRecord();
        record.setId(100L);
        when(infringementRecordMapper.selectById(100L)).thenReturn(record);
        doReturn(1).when(infringementRecordMapper).updateById(any(InfringementRecord.class));

        infringementService.processReport(100L, 99L, 1, "已核实侵权", 1, "下架商品");

        verify(infringementRecordMapper, times(1)).updateById(argThat((InfringementRecord r) ->
                r.getStatus() == 1 && r.getHandlerId() == 99L));
    }

    @Test
    @DisplayName("处理举报 - 记录不存在抛异常")
    void testProcessReport_NotFound() {
        when(infringementRecordMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                infringementService.processReport(999L, 99L, 1, "原因", 1, "详情"));
    }

    @Test
    @DisplayName("创建仲裁 - 成功")
    void testCreateArbitration_Success() {
        InfringementRecord report = new InfringementRecord();
        report.setId(100L);
        report.setReportNo("RPT123");
        when(infringementRecordMapper.selectById(100L)).thenReturn(report);
        doReturn(1).when(arbitrationRecordMapper).insert(any(ArbitrationRecord.class));
        doReturn(1).when(infringementRecordMapper).updateById(any(InfringementRecord.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            ArbitrationRequest request = new ArbitrationRequest();
            request.setArbitratorId(99L);
            request.setArbitratorName("仲裁员");
            request.setArbitratorType(1); // 1=平台仲裁员

            ArbitrationRecord result = infringementService.createArbitration(100L, request);

            assertNotNull(result);
            assertEquals(100L, result.getInfringementId());
            assertEquals(0, result.getResult());
            verify(infringementRecordMapper, times(1)).updateById(argThat((InfringementRecord r) -> r.getStatus() == 6));
        }
    }

    @Test
    @DisplayName("创建仲裁 - 举报记录不存在抛异常")
    void testCreateArbitration_ReportNotFound() {
        when(infringementRecordMapper.selectById(999L)).thenReturn(null);

        ArbitrationRequest request = new ArbitrationRequest();
        request.setArbitratorId(99L);

        assertThrows(BusinessException.class, () ->
                infringementService.createArbitration(999L, request));
    }

    @Test
    @DisplayName("侵权列表 - 分页查询")
    void testListInfringements() {
        Page<InfringementRecord> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(infringementRecordMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<InfringementRecord> result = infringementService.listInfringements(1L, new PageRequest(1, 20), null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("侵权列表 - 按状态过滤")
    void testListInfringements_ByStatus() {
        Page<InfringementRecord> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(infringementRecordMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<InfringementRecord> result = infringementService.listInfringements(1L, new PageRequest(1, 20), 1);

        assertNotNull(result);
    }

    @Test
    @DisplayName("完成仲裁 - 成功")
    void testCompleteArbitration_Success() {
        ArbitrationRecord arbitration = new ArbitrationRecord();
        arbitration.setId(1L);
        arbitration.setInfringementId(100L);
        when(arbitrationRecordMapper.selectById(1L)).thenReturn(arbitration);
        doReturn(1).when(arbitrationRecordMapper).updateById(any(ArbitrationRecord.class));
        doReturn(1).when(infringementRecordMapper).updateById(any(InfringementRecord.class));

        infringementService.completeArbitration(1L, 1, "判定侵权成立");

        verify(arbitrationRecordMapper, times(1)).updateById(argThat((ArbitrationRecord a) ->
                a.getResult() == 1 && a.getResultDescription().equals("判定侵权成立")));
        verify(infringementRecordMapper, times(1)).updateById(argThat((InfringementRecord r) -> r.getStatus() == 7));
    }

    @Test
    @DisplayName("完成仲裁 - 仲裁记录不存在抛异常")
    void testCompleteArbitration_NotFound() {
        when(arbitrationRecordMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () ->
                infringementService.completeArbitration(999L, 1, "描述"));
    }
}
