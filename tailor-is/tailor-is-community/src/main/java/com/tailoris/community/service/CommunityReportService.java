package com.tailoris.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.entity.CommunityReportAction;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.mapper.CommunityReportActionMapper;
import com.tailoris.community.mapper.CommunityReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 社区举报 Service
 * 任务编号: COM-004
 *
 * <p>举报工作流：用户举报 → 机审（敏感词） → 人工审核 → 处置（删除/隐藏/封禁）</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityReportService {

    private final CommunityReportMapper reportMapper;
    private final CommunityReportActionMapper actionMapper;
    private final CommunityPostMapper postMapper;
    private final SensitiveWordFilter sensitiveWordFilter;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String REPORT_LIMIT_KEY = "community:report:limit:";
    private static final int MAX_REPORTS_PER_HOUR = 10;
    private static final long LIMIT_TTL_HOURS = 1;

    @Transactional
    public CommunityReport submitReport(Long userId, Long targetId, Integer targetType,
                                         Integer reasonType, String reason, String evidence) {
        if (userId == null || targetId == null || targetType == null) {
            throw new BusinessException("参数错误");
        }
        // 防刷：每小时最多10次举报
        String key = REPORT_LIMIT_KEY + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, LIMIT_TTL_HOURS, TimeUnit.HOURS);
        if (count != null && count > MAX_REPORTS_PER_HOUR) {
            throw new BusinessException("举报过于频繁，请稍后再试");
        }
        // 检查是否重复举报
        Long exist = reportMapper.selectCount(new LambdaQueryWrapper<CommunityReport>()
                .eq(CommunityReport::getReporterId, userId)
                .eq(CommunityReport::getTargetId, targetId)
                .eq(CommunityReport::getTargetType, targetType)
                .ne(CommunityReport::getStatus, 2));
        if (exist != null && exist > 0) {
            throw new BusinessException("已举报过该内容，请勿重复提交");
        }

        CommunityReport report = new CommunityReport();
        report.setId(SnowflakeIdGenerator.getInstance().nextId());
        report.setReporterId(userId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReasonType(reasonType);
        report.setReason(reason);
        report.setEvidence(evidence);
        report.setStatus(0); // 待处理

        // 机审：如果内容包含敏感词，自动设为已处理并删除
        if (targetType == 1) {
            CommunityPost post = postMapper.selectById(targetId);
            if (post != null) {
                String content = (post.getTitle() == null ? "" : post.getTitle())
                        + " " + (post.getContent() == null ? "" : post.getContent());
                if (sensitiveWordFilter.containsSensitive(content)) {
                    // 自动审核通过举报，并隐藏帖子
                    report.setStatus(1); // 已处理
                    report.setHandleResult("机审：内容违规，已隐藏");
                    report.setHandleTime(LocalDateTime.now());
                    post.setStatus(4); // 4=机审违规
                    postMapper.updateById(post);
                }
            }
        }

        reportMapper.insert(report);
        log.info("提交举报: reporter={}, target={}:{}", userId, targetType, targetId);
        return report;
    }

    public PageResponse<CommunityReport> listReports(PageRequest pageRequest, Integer status, Integer targetType) {
        Page<CommunityReport> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CommunityReport::getStatus, status);
        }
        if (targetType != null) {
            wrapper.eq(CommunityReport::getTargetType, targetType);
        }
        wrapper.orderByAsc(CommunityReport::getStatus);
        wrapper.orderByDesc(CommunityReport::getCreateTime);
        Page<CommunityReport> result = reportMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Transactional
    public void handleReport(Long reportId, Long handlerId, Integer actionType,
                              String actionReason, Integer actionDays) {
        CommunityReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (report.getStatus() != null && report.getStatus() == 1) {
            throw new BusinessException("举报已处理");
        }

        // 记录处置
        CommunityReportAction action = new CommunityReportAction();
        action.setId(SnowflakeIdGenerator.getInstance().nextId());
        action.setReportId(reportId);
        action.setHandlerId(handlerId);
        action.setActionType(actionType);
        action.setActionReason(actionReason);
        action.setActionDays(actionDays);
        actionMapper.insert(action);

        // 执行处置
        if (report.getTargetType() != null && report.getTargetType() == 1) {
            CommunityPost post = postMapper.selectById(report.getTargetId());
            if (post != null) {
                switch (actionType == null ? 0 : actionType) {
                    case 1: // 删除
                        post.setStatus(3);
                        break;
                    case 2: // 隐藏
                        post.setStatus(0);
                        break;
                    case 3: // 警告
                        // 仅记录
                        break;
                    case 4: // 封禁
                        post.setStatus(3);
                        // 封禁用户逻辑由调用方实现
                        break;
                    default:
                        // 不做任何操作
                        break;
                }
                postMapper.updateById(post);
            }
        }

        report.setStatus(1);
        report.setHandleResult(actionReason);
        report.setHandleTime(LocalDateTime.now());
        report.setHandlerId(handlerId);
        reportMapper.updateById(report);
        log.info("处理举报: reportId={}, action={}", reportId, actionType);
    }

    public List<CommunityReportAction> listActions(Long reportId) {
        return actionMapper.selectByReportId(reportId);
    }
}
