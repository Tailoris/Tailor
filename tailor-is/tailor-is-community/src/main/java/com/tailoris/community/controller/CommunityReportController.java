package com.tailoris.community.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.entity.CommunityReportAction;
import com.tailoris.community.service.CommunityReportService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/community/report")
@RequiredArgsConstructor
@Tag(name = "社区举报", description = "COM-004 举报处理")
public class CommunityReportController {

    private final CommunityReportService reportService;

    @Operation(summary = "提交举报", description = "提交举报信息")
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

    @Operation(summary = "举报列表", description = "分页查询举报列表")
    @GetMapping("/list")
    public Result<PageResponse<CommunityReport>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer targetType) {
        return Result.success(reportService.listReports(
                new PageRequest(pageNum, pageSize), status, targetType));
    }

    @Operation(summary = "处理举报", description = "管理员处理举报")
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

    @Operation(summary = "举报处理记录", description = "获取指定举报的处理记录")
    @GetMapping("/{id}/actions")
    public Result<List<CommunityReportAction>> actions(@PathVariable Long id) {
        return Result.success(reportService.listActions(id));
    }
}
