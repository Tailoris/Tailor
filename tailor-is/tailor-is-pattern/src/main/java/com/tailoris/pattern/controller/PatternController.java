package com.tailoris.pattern.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.pattern.entity.Pattern;
import com.tailoris.pattern.service.PatternService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "版型管理")
@RestController
@RequestMapping("/api/v1/pattern")
@RequiredArgsConstructor
public class PatternController {

    private final PatternService patternService;

    @SaCheckRole("merchant")
    @Operation(summary = "版型列表")
    @GetMapping("/list")
    public Result<Page<Pattern>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Pattern> page = patternService.pagePatterns(pageNum, pageSize);
        return Result.success(page);
    }

    @SaCheckRole("merchant")
    @Operation(summary = "版型详情")
    @GetMapping("/{id}")
    public Result<Pattern> getById(@PathVariable Long id) {
        Pattern pattern = patternService.getPatternById(id);
        return Result.success(pattern);
    }
}