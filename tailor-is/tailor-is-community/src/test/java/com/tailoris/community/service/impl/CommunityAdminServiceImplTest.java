package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.community.dto.ReportRequest;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityReportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityAdminServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityAdminServiceImplTest {

    @Mock
    private CommunityReportMapper communityReportMapper;
    @Mock
    private CommunityPostMapper communityPostMapper;

    @InjectMocks
    private CommunityAdminServiceImpl adminService;

    @Test
    @DisplayName("审核帖子 - 成功")
    void testAuditPost_Success() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById(Mockito.<CommunityPost>any());

        adminService.auditPost(10L, 1, 99L, "通过");

        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("审核帖子 - 不存在抛异常")
    void testAuditPost_NotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> adminService.auditPost(99L, 1, 99L, "ok"));
    }

    @Test
    @DisplayName("删除帖子 - 成功")
    void testDeletePost_Success() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById(Mockito.<CommunityPost>any());

        adminService.deletePost(10L);

        verify(communityPostMapper).updateById(any(CommunityPost.class));
    }

    @Test
    @DisplayName("删除帖子 - 不存在抛异常")
    void testDeletePost_NotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> adminService.deletePost(99L));
    }

    @Test
    @DisplayName("创建举报 - 成功")
    void testCreateReport_Success() {
        ReportRequest request = new ReportRequest();
        request.setPostId(10L);
        request.setReportedUserId(2L);
        request.setReason("广告");
        request.setDescription("垃圾广告");

        doReturn(1).when(communityReportMapper).insert(any(CommunityReport.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            adminService.createReport(1L, request);

            verify(communityReportMapper).insert(any(CommunityReport.class));
        }
    }

    @Test
    @DisplayName("处理举报 - 成功")
    void testProcessReport_Success() {
        CommunityReport report = new CommunityReport();
        report.setId(100L);
        when(communityReportMapper.selectById(100L)).thenReturn(report);
        doReturn(1).when(communityReportMapper).updateById(Mockito.<CommunityReport>any());

        adminService.processReport(100L, 99L, 1, "已处理", 1);

        verify(communityReportMapper).updateById((CommunityReport) any());
    }

    @Test
    @DisplayName("处理举报 - 不存在抛异常")
    void testProcessReport_NotFound() {
        when(communityReportMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> adminService.processReport(999L, 99L, 1, "ok", 1));
    }

    @Test
    @DisplayName("举报列表 - 分页查询")
    void testListReports() {
        Page<CommunityReport> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(communityReportMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityReport> result = adminService.listReports(new PageRequest(1, 20), null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("举报列表 - 按状态过滤")
    void testListReports_ByStatus() {
        Page<CommunityReport> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(communityReportMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityReport> result = adminService.listReports(new PageRequest(1, 20), 0);

        assertNotNull(result);
    }

    @Test
    @DisplayName("获取举报详情")
    void testGetReportDetail() {
        CommunityReport report = new CommunityReport();
        report.setId(100L);
        when(communityReportMapper.selectById(100L)).thenReturn(report);

        CommunityReport result = adminService.getReportDetail(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
    }
}
