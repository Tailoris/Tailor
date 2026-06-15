package com.tailoris.community.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.dto.ReportRequest;
import com.tailoris.community.entity.CommunityReport;
import com.tailoris.community.service.CommunityAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckRole("admin")
@Tag(name = "社区管理后台", description = "帖子审核、举报处理等管理接口")
@RestController
@RequestMapping("/api/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityAdminService communityAdminService;

    @Operation(summary = "审核帖子", description = "管理员审核帖子(通过/拒绝)")
    @PostMapping("/post/audit/{postId}")
    public Result<Void> auditPost(@PathVariable Long postId, @RequestParam Integer status, @RequestParam(required = false) String auditRemark) {
        communityAdminService.auditPost(postId, status, 0L, auditRemark);
        return Result.success();
    }

    @Operation(summary = "删除帖子", description = "管理员删除帖子")
    @DeleteMapping("/post/delete/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        communityAdminService.deletePost(postId);
        return Result.success();
    }

    @Operation(summary = "举报帖子/评论", description = "用户举报帖子或评论")
    @PostMapping("/report/create")
    public Result<Void> createReport(@Valid @RequestBody ReportRequest request) {
        communityAdminService.createReport(0L, request);
        return Result.success();
    }

    @Operation(summary = "处理举报", description = "管理员处理举报")
    @PostMapping("/report/process/{reportId}")
    public Result<Void> processReport(@PathVariable Long reportId, @RequestParam Integer status, @RequestParam(required = false) String handlerRemark, @RequestParam(required = false) Integer punishmentType) {
        communityAdminService.processReport(reportId, 0L, status, handlerRemark, punishmentType);
        return Result.success();
    }

    @Operation(summary = "举报列表", description = "分页查询举报记录列表")
    @GetMapping("/report/list")
    public Result<PageResponse<CommunityReport>> listReports(PageRequest pageRequest, @RequestParam(required = false) Integer status) {
        return Result.success(communityAdminService.listReports(pageRequest, status));
    }

    @Operation(summary = "举报详情", description = "查询举报记录详情")
    @GetMapping("/report/detail/{reportId}")
    public Result<CommunityReport> getReportDetail(@PathVariable Long reportId) {
        return Result.success(communityAdminService.getReportDetail(reportId));
    }
}
