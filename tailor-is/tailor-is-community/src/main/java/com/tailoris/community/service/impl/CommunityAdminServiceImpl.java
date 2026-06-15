package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.dto.ReportRequest;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityReportMapper;
import com.tailoris.community.service.CommunityAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityAdminServiceImpl implements CommunityAdminService {

    private final CommunityReportMapper communityReportMapper;
    private final CommunityPostMapper communityPostMapper;

    @Override
    @Transactional
    public void auditPost(Long postId, Integer status, Long auditBy, String auditRemark) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setStatus(status);
        post.setAuditBy(auditBy);
        post.setAuditRemark(auditRemark);
        post.setAuditTime(LocalDateTime.now());
        communityPostMapper.updateById(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setStatus(3);
        communityPostMapper.updateById(post);
    }

    @Transactional
    public void createReport(Long reporterId, ReportRequest request) {
        CommunityReport report = new CommunityReport();
        report.setId(SnowflakeIdGenerator.getInstance().nextId());
        report.setPostId(request.getPostId());
        report.setCommentId(request.getCommentId());
        report.setReportedUserId(request.getReportedUserId());
        report.setReporterId(reporterId);
        report.setReason(request.getReason());
        report.setDescription(request.getDescription());
        report.setEvidenceImages(request.getEvidenceImages());
        report.setStatus(0);
        communityReportMapper.insert(report);
    }

    @Override
    @Transactional
    public void processReport(Long reportId, Long handlerId, Integer status, String handlerRemark, Integer punishmentType) {
        CommunityReport report = communityReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }
        report.setStatus(status);
        report.setHandlerId(handlerId);
        report.setHandlerRemark(handlerRemark);
        report.setPunishmentType(punishmentType);
        report.setHandledAt(LocalDateTime.now());
        communityReportMapper.updateById(report);
    }

    @Override
    public PageResponse<CommunityReport> listReports(PageRequest pageRequest, Integer status) {
        Page<CommunityReport> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CommunityReport::getStatus, status);
        }
        wrapper.orderByDesc(CommunityReport::getCreateTime);
        Page<CommunityReport> result = communityReportMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public CommunityReport getReportDetail(Long reportId) {
        return communityReportMapper.selectById(reportId);
    }
}
