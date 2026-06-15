package com.tailoris.copyright.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.copyright.dto.ArbitrationRequest;
import com.tailoris.copyright.dto.InfringementReportRequest;
import com.tailoris.copyright.entity.ArbitrationRecord;
import com.tailoris.copyright.entity.InfringementRecord;
import com.tailoris.copyright.service.InfringementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "侵权管理", description = "侵权举报、仲裁等接口")
@RestController
@RequestMapping("/api/v1/copyright/infringement")
@RequiredArgsConstructor
public class InfringementController {

    private final InfringementService infringementService;

    @Operation(summary = "举报侵权", description = "提交侵权举报")
    @PostMapping("/report")
    public Result<InfringementRecord> reportInfringement(@Valid @RequestBody InfringementReportRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long reporterId = StpUtil.getLoginIdAsLong();
        InfringementRecord record = infringementService.reportInfringement(reporterId, request);
        return Result.success(record);
    }

    @Operation(summary = "查询我的举报列表", description = "分页查询用户的侵权举报记录")
    @GetMapping("/list")
    public Result<PageResponse<InfringementRecord>> listInfringements(PageRequest pageRequest, @RequestParam(required = false) Integer status) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long reporterId = StpUtil.getLoginIdAsLong();
        return Result.success(infringementService.listInfringements(reporterId, pageRequest, status));
    }

    @Operation(summary = "创建仲裁", description = "为侵权举报创建仲裁流程")
    @PostMapping("/arbitration/create")
    public Result<ArbitrationRecord> createArbitration(@RequestParam Long infringementId, @Valid @RequestBody ArbitrationRequest request) {
        ArbitrationRecord arbitration = infringementService.createArbitration(infringementId, request);
        return Result.success(arbitration);
    }

    @Operation(summary = "完成仲裁", description = "仲裁员完成仲裁判定")
    @PostMapping("/arbitration/complete")
    public Result<Void> completeArbitration(@RequestParam Long arbitrationId, @RequestParam Integer result, @RequestParam String resultDescription) {
        infringementService.completeArbitration(arbitrationId, result, resultDescription);
        return Result.success();
    }
}
