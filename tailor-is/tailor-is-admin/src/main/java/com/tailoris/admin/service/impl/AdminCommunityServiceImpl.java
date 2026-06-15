package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.ReportProcessRequest;
import com.tailoris.admin.service.AdminCommunityService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.api.community.entity.CommunityPost;
import com.tailoris.api.community.entity.CommunityReport;
import com.tailoris.api.community.mapper.CommunityPostMapper;
import com.tailoris.api.community.mapper.CommunityReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCommunityServiceImpl implements AdminCommunityService {

    private final CommunityReportMapper communityReportMapper;
    private final CommunityPostMapper communityPostMapper;

    @Override
    public PageResponse<CommunityReport> listReports(PageRequest request, Integer status) {
        LambdaQueryWrapper<CommunityReport> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            queryWrapper.eq(CommunityReport::getStatus, status);
        }

        queryWrapper.orderByAsc(CommunityReport::getCreateTime);

        Page<CommunityReport> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<CommunityReport> result = communityReportMapper.selectPage(page, queryWrapper);

        return new PageResponse<>(result.getRecords(), result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processReport(ReportProcessRequest request, Long adminId) {
        CommunityReport report = communityReportMapper.selectById(request.getReportId());
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }

        if (report.getStatus().equals(AdminConstants.REPORT_STATUS_HANDLED)
                || report.getStatus().equals(AdminConstants.REPORT_STATUS_IGNORED)) {
            throw new BusinessException("该举报已处理");
        }

        report.setStatus(AdminConstants.REPORT_STATUS_HANDLED);
        report.setHandlerRemark(request.getRemark());
        report.setHandlerId(adminId);
        report.setHandledAt(LocalDateTime.now());

        communityReportMapper.updateById(report);

        log.info("举报已处理, reportId: {}, result: {}", request.getReportId(), request.getProcessResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long postId, String reason) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        post.setStatus(AdminConstants.POST_STATUS_DELETED);
        post.setAuditRemark(reason);
        post.setAuditBy(null);
        post.setAuditTime(LocalDateTime.now());

        communityPostMapper.updateById(post);

        log.info("帖子已删除, postId: {}, reason: {}", postId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPost(Long postId, Integer status, String remark, Long adminId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        post.setAuditBy(adminId);
        post.setAuditRemark(remark);
        post.setAuditTime(LocalDateTime.now());

        if (status.equals(AdminConstants.AUDIT_STATUS_APPROVED)) {
            post.setStatus(AdminConstants.POST_STATUS_NORMAL);
        } else if (status.equals(AdminConstants.AUDIT_STATUS_REJECTED)) {
            post.setStatus(AdminConstants.POST_STATUS_DELETED);
            post.setAuditRemark("审核不通过: " + remark);
            post.setAuditTime(LocalDateTime.now());
        }

        communityPostMapper.updateById(post);

        log.info("帖子审核完成, postId: {}, status: {}", postId, status);
    }
}
