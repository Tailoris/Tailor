package com.tailoris.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.ai.dto.BodySizeAnalysisRequest;
import com.tailoris.ai.dto.BodySizeAnalysisResponse;
import com.tailoris.ai.dto.SizeDataRequest;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.service.BodySizeService;
import com.tailoris.ai.service.impl.PatternGenerateServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "体型数据管理", description = "体型数据录入、查询、搜索等接口")
@RestController
@RequestMapping("/api/v1/ai/size")
@RequiredArgsConstructor
public class BodySizeController {

    private final BodySizeService bodySizeService;
    private final PatternGenerateServiceImpl patternGenerateService;

    @Operation(summary = "录入/更新体型数据", description = "保存用户的体型测量数据")
    @PostMapping("/save")
    public Result<BodySizeData> saveSizeData(@Valid @RequestBody SizeDataRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        BodySizeData data = bodySizeService.manageSizeData(userId, request);
        return Result.success(data);
    }

    @Operation(summary = "查询体型数据详情", description = "根据ID查询体型数据详情")
    @GetMapping("/detail")
    public Result<BodySizeData> getSizeData(@RequestParam Long sizeId) {
        return Result.success(bodySizeService.getSizeData(sizeId));
    }

    @Operation(summary = "查询我的体型数据列表", description = "获取当前用户的所有体型数据")
    @GetMapping("/list")
    public Result<List<BodySizeData>> listMySizeData() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(bodySizeService.listUserSizeData(userId));
    }

    @Operation(summary = "按体型分类搜索", description = "按体型分类和性别搜索体型数据")
    @GetMapping("/search")
    public Result<List<BodySizeData>> searchByBodyType(@RequestParam(required = false) String bodyType, @RequestParam(required = false) Integer gender) {
        return Result.success(bodySizeService.searchByBodyType(bodyType, gender));
    }

    @Operation(summary = "设置默认体型", description = "将指定体型数据设为默认")
    @PutMapping("/set-default")
    public Result<Void> setDefaultSize(@RequestParam Long sizeId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        bodySizeService.setDefaultSize(userId, sizeId);
        return Result.success();
    }

    @Operation(summary = "删除体型数据", description = "删除指定体型数据")
    @DeleteMapping("/delete")
    public Result<Void> deleteSizeData(@RequestParam Long sizeId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        bodySizeService.deleteSizeData(userId, sizeId);
        return Result.success();
    }

    @Operation(summary = "AI体型分析(MVP)", description = "AI分析体型数据并推荐尺码")
    @PostMapping("/analyze")
    public Result<BodySizeAnalysisResponse> analyzeBodySize(@Valid @RequestBody BodySizeAnalysisRequest request) {
        BodySizeAnalysisResponse response = patternGenerateService.analyzeBodySize(request);
        return Result.success(response);
    }
}
