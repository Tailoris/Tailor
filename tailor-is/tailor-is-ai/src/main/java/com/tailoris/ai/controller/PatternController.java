package com.tailoris.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.ai.dto.PatternCheckRequest;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternGenerateResponse;
import com.tailoris.ai.dto.PatternIterationRequest;
import com.tailoris.ai.entity.PatternIteration;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.entity.PatternVersion;
import com.tailoris.ai.service.PatternService;
import com.tailoris.ai.service.impl.PatternGenerateServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "版型管理", description = "版型生成、检查、迭代、版本管理、导出等接口")
@RestController
@RequestMapping("/api/v1/ai/pattern")
@RequiredArgsConstructor
public class PatternController {

    private final PatternService patternService;
    private final PatternGenerateServiceImpl patternGenerateService;

    @Operation(summary = "生成版型", description = "基于体型数据和参数生成服装版型")
    @PostMapping("/generate")
    public Result<PatternRecord> generatePattern(@Valid @RequestBody PatternGenerateRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        PatternRecord record = patternService.generatePattern(userId, request);
        return Result.success(record);
    }

    @Operation(summary = "检查版型", description = "AI检测版型的结构合理性")
    @PostMapping("/check")
    public Result<String> checkPattern(@Valid @RequestBody PatternCheckRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String result = patternService.checkPattern(userId, request);
        return Result.success(result);
    }

    @Operation(summary = "迭代版型", description = "基于已有版型进行参数调整和优化")
    @PostMapping("/iterate")
    public Result<PatternIteration> iteratePattern(@Valid @RequestBody PatternIterationRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        PatternIteration iteration = patternService.iteratePattern(userId, request);
        return Result.success(iteration);
    }

    @Operation(summary = "保存版本", description = "为版型创建新的版本快照")
    @PostMapping("/version/save")
    public Result<PatternVersion> saveVersion(@RequestParam Long patternId, @RequestParam String versionName, @RequestParam(required = false) String changeDescription) {
        PatternVersion version = patternService.saveVersion(patternId, versionName, changeDescription);
        return Result.success(version);
    }

    @Operation(summary = "查询版本列表", description = "查询指定版型的所有版本")
    @GetMapping("/versions")
    public Result<List<PatternVersion>> listVersions(@RequestParam Long patternId) {
        return Result.success(patternService.listVersions(patternId));
    }

    @Operation(summary = "导出版型", description = "将版型导出为指定格式(SVG/PDF/DXF)")
    @GetMapping("/export")
    public Result<String> exportPattern(@RequestParam Long patternId, @RequestParam(required = false, defaultValue = "SVG") String format) {
        String url = patternService.exportPattern(patternId, format);
        return Result.success(url);
    }

    @Operation(summary = "查询版型详情", description = "根据ID查询版型详情")
    @GetMapping("/detail")
    public Result<PatternRecord> getPatternDetail(@RequestParam Long patternId) {
        return Result.success(patternService.getPatternDetail(patternId));
    }

    @Operation(summary = "查询我的版型列表", description = "获取当前用户的所有版型")
    @GetMapping("/list")
    public Result<List<PatternRecord>> listMyPatterns() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(patternService.listUserPatterns(userId));
    }

    @Operation(summary = "AI生成SVG纸样(MVP)", description = "基于参数生成SVG纸样预览，无需登录")
    @PostMapping("/generate-svg")
    public Result<PatternGenerateResponse> generateSvgPattern(@Valid @RequestBody PatternGenerateRequest request) {
        PatternGenerateResponse response = patternGenerateService.generatePattern(request);
        return Result.success(response);
    }

    @Operation(summary = "预览纸样SVG", description = "返回指定纸样的SVG内容")
    @GetMapping(value = "/preview/{patternId}", produces = "image/svg+xml")
    public String previewPattern(
            @Parameter(description = "纸样ID") @PathVariable String patternId) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 300\" width=\"200\" height=\"300\">\n" +
               "  <rect x=\"0\" y=\"0\" width=\"200\" height=\"300\" fill=\"#fff\" stroke=\"#333\" stroke-width=\"2\"/>\n" +
               "  <text x=\"100\" y=\"150\" text-anchor=\"middle\" font-size=\"14\" fill=\"#333\">Pattern: " + patternId + "</text>\n" +
               "  <text x=\"100\" y=\"290\" text-anchor=\"middle\" font-size=\"10\" fill=\"#999\">Tailor IS AI Generated</text>\n" +
               "</svg>";
    }
}
