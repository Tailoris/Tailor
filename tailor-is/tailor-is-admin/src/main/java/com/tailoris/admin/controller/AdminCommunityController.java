package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.api.admin.dto.ReportProcessRequest;
import com.tailoris.admin.service.AdminCommunityService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.result.Result;
import com.tailoris.api.community.entity.CommunityReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("admin")
@Tag(name = "平台社区管理")
@RestController
@RequestMapping("/api/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    @Operation(summary = "查询举报列表")
    @GetMapping("/reports")
    public Result<PageResponse<CommunityReport>> listReports(
            PageRequest request,
            @RequestParam(required = false) Integer status) {
        PageResponse<CommunityReport> page = adminCommunityService.listReports(request, status);
        return Result.success(page);
    }

    @Operation(summary = "处理举报")
    @PutMapping("/report/process")
    public Result<Void> processReport(@Valid @RequestBody ReportProcessRequest request) {
        adminCommunityService.processReport(request, null);
        return Result.success();
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adminCommunityService.deletePost(id, reason);
        return Result.success();
    }

    @Operation(summary = "审核帖子")
    @PutMapping("/post/audit/{id}")
    public Result<Void> auditPost(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestParam(required = false) String remark) {
        adminCommunityService.auditPost(id, status, remark, null);
        return Result.success();
    }
}
