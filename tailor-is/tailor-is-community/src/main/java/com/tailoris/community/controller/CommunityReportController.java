package com.tailoris.community.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.entity.CommunityReportAction;
import com.tailoris.community.service.CommunityReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区举报 Controller
 * 任务编号: COM-004
 */
@Slf4j
@RestController
@RequestMapping("/api/community/report")
@RequiredArgsConstructor
@Tag(name = "社区举报", description = "COM-004 举报处理")
public class CommunityReportController {

    private final CommunityReportService reportService;

    @PostMapping
    public Result<CommunityReport> submit(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long targetId,
            @RequestParam Integer targetType,
            @RequestParam Integer reasonType,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String evidence) {
        return Result.success(reportService.submitReport(userId, targetId, targetType, reasonType, reason, evidence));
    }

    @GetMapping("/list")
    public Result<PageResponse<CommunityReport>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer targetType) {
        return Result.success(reportService.listReports(
                new PageRequest(pageNum, pageSize), status, targetType));
    }

    @PostMapping("/{id}/handle")
    public Result<Void> handle(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long handlerId,
            @RequestParam Integer actionType,
            @RequestParam(required = false) String actionReason,
            @RequestParam(required = false) Integer actionDays) {
        reportService.handleReport(id, handlerId, actionType, actionReason, actionDays);
        return Result.success();
    }

    @GetMapping("/{id}/actions")
    public Result<List<CommunityReportAction>> actions(@PathVariable Long id) {
        return Result.success(reportService.listActions(id));
    }
}
