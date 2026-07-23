package com.tailoris.pattern.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.result.Result;
import com.tailoris.pattern.dto.PatternCreateDTO;
import com.tailoris.pattern.entity.Pattern;
import com.tailoris.pattern.mapper.PatternMapper;
import com.tailoris.pattern.service.PatternService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.tailoris.common.constant.RoutePortConstant;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "版型管理")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_PATTERN)
@RequiredArgsConstructor
public class PatternController {

    private final PatternService patternService;
    private final PatternMapper patternMapper;
    private final ObjectMapper objectMapper;

    @Operation(summary = "版型列表（公开）")
    @GetMapping("/list")
    public Result<Page<Pattern>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Pattern> page = patternService.pagePatterns(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "版型详情（公开）")
    @GetMapping("/{id}")
    public Result<Pattern> getById(@PathVariable Long id) {
        Pattern pattern = patternService.getPatternById(id);
        return Result.success(pattern);
    }

    @Operation(summary = "创建版型")
    @PostMapping
    public Result<Long> create(@RequestBody PatternCreateDTO dto) {
        Pattern pattern = new Pattern();
        pattern.setName(buildPatternName(dto.getStyleType(), dto.getPatternStyle()));
        pattern.setCategory(dto.getStyleType());
        pattern.setDescription(dto.getDescription());
        pattern.setImageUrl(dto.getImageUrl());
        
        // 计算价格（加急+50%）
        BigDecimal basePrice = calculateBasePrice(dto.getStyleType(), dto.getTechRequirements());
        if (Boolean.TRUE.equals(dto.getIsUrgent())) {
            basePrice = basePrice.multiply(new BigDecimal("1.5"));
        }
        pattern.setPrice(basePrice.longValue());
        
        // 将额外字段存入dimensions JSON
        pattern.setDimensions(buildDimensions(dto));
        
        // 默认状态为进行中
        pattern.setStatus(1);
        
        Long id = patternService.createPattern(pattern);
        return Result.success(id);
    }
    
    private String buildPatternName(String styleType, String patternStyle) {
        Map<String, String> styleLabels = new HashMap<>();
        styleLabels.put("dress", "连衣裙");
        styleLabels.put("top", "上衣");
        styleLabels.put("pants", "裤子");
        styleLabels.put("coat", "外套");
        styleLabels.put("skirt", "半身裙");
        styleLabels.put("shirt", "衬衫");
        styleLabels.put("other", "其他");
        
        Map<String, String> styleStyleLabels = new HashMap<>();
        styleStyleLabels.put("casual", "休闲");
        styleStyleLabels.put("business", "商务");
        styleStyleLabels.put("sport", "运动");
        styleStyleLabels.put("elegant", "优雅");
        styleStyleLabels.put("vintage", "复古");
        styleStyleLabels.put("fashion", "时尚");
        
        return styleLabels.getOrDefault(styleType, styleType) + " - " + 
               styleStyleLabels.getOrDefault(patternStyle, patternStyle);
    }
    
    private BigDecimal calculateBasePrice(String styleType, List<String> techRequirements) {
        Map<String, BigDecimal> basePrices = new HashMap<>();
        basePrices.put("dress", new BigDecimal("199"));
        basePrices.put("top", new BigDecimal("99"));
        basePrices.put("pants", new BigDecimal("129"));
        basePrices.put("coat", new BigDecimal("299"));
        basePrices.put("skirt", new BigDecimal("89"));
        basePrices.put("shirt", new BigDecimal("79"));
        basePrices.put("other", new BigDecimal("150"));
        
        BigDecimal price = basePrices.getOrDefault(styleType, new BigDecimal("150"));
        
        // 工艺要求加价
        if (techRequirements != null) {
            for (String tech : techRequirements) {
                switch (tech) {
                    case "visible-stitch":
                        price = price.add(new BigDecimal("20"));
                        break;
                    case "hidden-stitch":
                        price = price.add(new BigDecimal("30"));
                        break;
                    case "overlock":
                        price = price.add(new BigDecimal("15"));
                        break;
                    case "buttonhole":
                        price = price.add(new BigDecimal("10"));
                        break;
                    case "zipper":
                        price = price.add(new BigDecimal("25"));
                        break;
                    case "interlining":
                        price = price.add(new BigDecimal("35"));
                        break;
                }
            }
        }
        
        return price;
    }
    
    private String buildDimensions(PatternCreateDTO dto) {
        try {
            Map<String, Object> dimensions = new HashMap<>();
            dimensions.put("targetAudience", dto.getTargetAudience());
            dimensions.put("minSize", dto.getMinSize());
            dimensions.put("maxSize", dto.getMaxSize());
            dimensions.put("techRequirements", dto.getTechRequirements());
            dimensions.put("minBudget", dto.getMinBudget());
            dimensions.put("maxBudget", dto.getMaxBudget());
            dimensions.put("deliveryDate", dto.getDeliveryDate());
            dimensions.put("isUrgent", dto.getIsUrgent());
            return objectMapper.writeValueAsString(dimensions);
        } catch (Exception e) {
            log.warn("Failed to serialize dimensions", e);
            return "{}";
        }
    }

    @Operation(summary = "更新版型")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Pattern pattern) {
        patternService.updatePattern(id, pattern);
        return Result.success();
    }

    @Operation(summary = "删除版型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        patternService.deletePattern(id);
        return Result.success();
    }

    @Operation(summary = "获取当前用户的版型列表")
    @GetMapping("/my-patterns")
    public Result<List<Pattern>> getMyPatterns() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<Pattern> patterns = patternService.listByMerchantId(userId);
        return Result.success(patterns);
    }

    @SaCheckRole("merchant")
    @Operation(summary = "商家版型列表")
    @GetMapping("/merchant/list")
    public Result<Page<Pattern>> merchantList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long merchantId = StpUtil.getLoginIdAsLong();
        Page<Pattern> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Pattern> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pattern::getMerchantId, merchantId);
        Page<Pattern> result = patternMapper.selectPage(page, wrapper);
        return Result.success(result);
    }
}