package com.tailoris.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.entity.CommunityReportAction;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityReportActionMapper;
import com.tailoris.community.mapper.CommunityReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityReportService 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityReportServiceTest {

    @Mock
    private CommunityReportMapper reportMapper;
    @Mock
    private CommunityReportActionMapper actionMapper;
    @Mock
    private CommunityPostMapper postMapper;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CommunityReportService reportService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("提交举报 - 成功")
    void testSubmitReport_Success() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(reportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doReturn(1).when(reportMapper).insert(any(CommunityReport.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            CommunityReport result = reportService.submitReport(1L, 10L, 1, 1, "广告", "证据");

            assertNotNull(result);
            assertEquals(0, result.getStatus());
        }
    }

    @Test
    @DisplayName("提交举报 - 参数为空抛异常")
    void testSubmitReport_NullParams() {
        assertThrows(BusinessException.class, () ->
                reportService.submitReport(null, 10L, 1, 1, "原因", "证据"));
        assertThrows(BusinessException.class, () ->
                reportService.submitReport(1L, null, 1, 1, "原因", "证据"));
        assertThrows(BusinessException.class, () ->
                reportService.submitReport(1L, 10L, null, 1, "原因", "证据"));
    }

    @Test
    @DisplayName("提交举报 - 超过频率限制抛异常")
    void testSubmitReport_RateLimited() {
        when(valueOperations.increment(anyString())).thenReturn(11L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                reportService.submitReport(1L, 10L, 1, 1, "原因", "证据"));
    }

    @Test
    @DisplayName("提交举报 - 重复举报抛异常")
    void testSubmitReport_Duplicate() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(reportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class, () ->
                reportService.submitReport(1L, 10L, 1, 1, "原因", "证据"));
    }

    @Test
    @DisplayName("提交举报 - 机审自动处理违规内容")
    void testSubmitReport_AutoModeration() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(stringRedisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(reportMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doReturn(1).when(reportMapper).insert(any(CommunityReport.class));

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setTitle("违规标题");
        post.setContent("违规内容");
        when(postMapper.selectById(10L)).thenReturn(post);
        when(sensitiveWordFilter.containsSensitive(anyString())).thenReturn(true);
        doReturn(1).when(postMapper).updateById(any(CommunityPost.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            CommunityReport result = reportService.submitReport(1L, 10L, 1, 1, "违规", "证据");

            assertNotNull(result);
            assertEquals(1, result.getStatus());
            verify(postMapper).updateById(Mockito.<CommunityPost>any());
        }
    }

    @Test
    @DisplayName("举报列表 - 分页查询")
    void testListReports() {
        Page<CommunityReport> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(reportMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityReport> result = reportService.listReports(new PageRequest(1, 20), null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("举报列表 - 按状态过滤")
    void testListReports_ByStatus() {
        Page<CommunityReport> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(reportMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityReport> result = reportService.listReports(new PageRequest(1, 20), 0, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("举报列表 - 按类型过滤")
    void testListReports_ByType() {
        Page<CommunityReport> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(reportMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityReport> result = reportService.listReports(new PageRequest(1, 20), null, 1);

        assertNotNull(result);
    }

    @Test
    @DisplayName("处理举报 - 删除帖子")
    void testHandleReport_DeletePost() {
        CommunityReport report = new CommunityReport();
        report.setId(100L);
        report.setTargetType(1);
        report.setTargetId(10L);
        report.setStatus(0);
        when(reportMapper.selectById(100L)).thenReturn(report);

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(postMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(actionMapper).insert(any(CommunityReportAction.class));
        doReturn(1).when(postMapper).updateById(any(CommunityPost.class));
        doReturn(1).when(reportMapper).updateById((CommunityReport) any());

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            reportService.handleReport(100L, 99L, 1, "删除违规帖子", null);

            verify(postMapper).updateById(Mockito.<CommunityPost>any());
            verify(reportMapper).updateById(any(CommunityReport.class));
        }
    }

    @Test
    @DisplayName("处理举报 - 隐藏帖子")
    void testHandleReport_HidePost() {
        CommunityReport report = new CommunityReport();
        report.setId(100L);
        report.setTargetType(1);
        report.setTargetId(10L);
        report.setStatus(0);
        when(reportMapper.selectById(100L)).thenReturn(report);

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(postMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(actionMapper).insert(any(CommunityReportAction.class));
        doReturn(1).when(postMapper).updateById(any(CommunityPost.class));
        doReturn(1).when(reportMapper).updateById((CommunityReport) any());

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            reportService.handleReport(100L, 99L, 2, "隐藏帖子", null);

            verify(postMapper).updateById(Mockito.<CommunityPost>any());
        }
    }

    @Test
    @DisplayName("处理举报 - 仅警告")
    void testHandleReport_Warn() {
        CommunityReport report = new CommunityReport();
        report.setId(100L);
        report.setTargetType(1);
        report.setTargetId(10L);
        report.setStatus(0);
        when(reportMapper.selectById(100L)).thenReturn(report);

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(postMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(actionMapper).insert(any(CommunityReportAction.class));
        doReturn(1).when(postMapper).updateById(any(CommunityPost.class));
        doReturn(1).when(reportMapper).updateById((CommunityReport) any());

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            reportService.handleReport(100L, 99L, 3, "警告", null);

            verify(postMapper).updateById(any(CommunityPost.class));
        }
    }

    @Test
    @DisplayName("处理举报 - 举报不存在抛异常")
    void testHandleReport_NotFound() {
        when(reportMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () ->
                reportService.handleReport(999L, 99L, 1, "原因", null));
    }

    @Test
    @DisplayName("处理举报 - 已处理举报抛异常")
    void testHandleReport_AlreadyHandled() {
        CommunityReport report = new CommunityReport();
        report.setId(100L);
        report.setStatus(1);
        when(reportMapper.selectById(100L)).thenReturn(report);

        assertThrows(BusinessException.class, () ->
                reportService.handleReport(100L, 99L, 1, "原因", null));
    }

    @Test
    @DisplayName("获取举报处置记录")
    void testListActions() {
        CommunityReportAction action = new CommunityReportAction();
        action.setId(1L);
        when(actionMapper.selectByReportId(100L)).thenReturn(Arrays.asList(action));

        List<CommunityReportAction> result = reportService.listActions(100L);

        assertEquals(1, result.size());
    }
}
