package com.tailoris.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.design.dto.DesignQueryRequest;
import com.tailoris.design.dto.DesignRequest;
import com.tailoris.design.entity.Design;
import com.tailoris.design.service.DesignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(RoutePortConstant.ROUTE_DESIGN)
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

    @Operation(summary = "创建设计", description = "创建新的设计作品")
    @PostMapping
    public Result<Design> createDesign(@Valid @RequestBody DesignRequest request) {
        Design design = new Design();
        design.setName(request.getName());
        design.setDescription(request.getDescription());
        design.setCoverImage(request.getCoverImage());
        design.setDesignType(request.getDesignType());
        design.setStyleCategory(request.getStyleCategory());
        design.setColor(request.getColor());
        design.setMaterial(request.getMaterial());
        design.setSizeRange(request.getSizeRange());
        design.setDesignData(request.getDesignData());
        design.setDifficulty(request.getDifficulty());
        design.setWorkTime(request.getWorkTime());
        design.setPrice(request.getPrice());
        design.setIsFree(request.getIsFree() != null ? request.getIsFree() : 0);
        design.setPatternId(request.getPatternId());
        design.setMerchantId(request.getMerchantId());
        design.setUserId(1L);
        
        Design created = designService.createDesign(design);
        return Result.success(created);
    }

    @Operation(summary = "更新设计", description = "更新指定设计作品信息")
    @PutMapping("/{id}")
    public Result<Design> updateDesign(@PathVariable Long id, @Valid @RequestBody DesignRequest request) {
        Design design = new Design();
        design.setName(request.getName());
        design.setDescription(request.getDescription());
        design.setCoverImage(request.getCoverImage());
        design.setDesignType(request.getDesignType());
        design.setStyleCategory(request.getStyleCategory());
        design.setColor(request.getColor());
        design.setMaterial(request.getMaterial());
        design.setSizeRange(request.getSizeRange());
        design.setDesignData(request.getDesignData());
        design.setDifficulty(request.getDifficulty());
        design.setWorkTime(request.getWorkTime());
        design.setPrice(request.getPrice());
        design.setIsFree(request.getIsFree());
        design.setPatternId(request.getPatternId());
        
        Design updated = designService.updateDesign(id, design);
        return Result.success(updated);
    }

    @Operation(summary = "删除设计", description = "删除指定设计作品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDesign(@PathVariable Long id) {
        designService.deleteDesign(id);
        return Result.success();
    }

    @Operation(summary = "设计详情", description = "获取设计作品详情")
    @GetMapping("/{id}")
    public Result<Design> getDesignById(@PathVariable Long id) {
        Design design = designService.getDesignById(id);
        return Result.success(design);
    }

    @Operation(summary = "设计分页查询", description = "分页查询设计作品列表")
    @GetMapping("/page")
    public Result<PageResponse<Design>> getDesignPage(@ModelAttribute DesignQueryRequest queryRequest) {
        IPage<Design> page = designService.getDesignPage(queryRequest);
        return Result.success(new PageResponse<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize()));
    }

    @Operation(summary = "热门设计", description = "获取热门设计作品列表")
    @GetMapping("/hot")
    public Result<List<Design>> getHotDesigns(@RequestParam(defaultValue = "10") Integer limit) {
        List<Design> designs = designService.getHotDesigns(limit);
        return Result.success(designs);
    }

    @Operation(summary = "最新设计", description = "获取最新设计作品列表")
    @GetMapping("/new")
    public Result<List<Design>> getNewDesigns(@RequestParam(defaultValue = "10") Integer limit) {
        List<Design> designs = designService.getNewDesigns(limit);
        return Result.success(designs);
    }

    @Operation(summary = "用户设计列表", description = "获取指定用户的设计作品列表")
    @GetMapping("/user/{userId}")
    public Result<List<Design>> getDesignsByUserId(@PathVariable Long userId) {
        List<Design> designs = designService.getDesignsByUserId(userId);
        return Result.success(designs);
    }

    @Operation(summary = "点赞设计", description = "切换设计作品点赞状态")
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long id) {
        boolean liked = designService.toggleLike(id, 1L);
        return Result.success(Map.of("liked", liked));
    }

    @Operation(summary = "收藏设计", description = "切换设计作品收藏状态")
    @PostMapping("/{id}/collect")
    public Result<Map<String, Object>> toggleCollection(@PathVariable Long id) {
        boolean collected = designService.toggleCollection(id, 1L);
        return Result.success(Map.of("collected", collected));
    }

    @Operation(summary = "检查是否已点赞", description = "检查当前用户是否已点赞该设计")
    @GetMapping("/{id}/liked")
    public Result<Map<String, Object>> checkIsLiked(@PathVariable Long id) {
        boolean liked = designService.checkIsLiked(id, 1L);
        return Result.success(Map.of("liked", liked));
    }

    @Operation(summary = "检查是否已收藏", description = "检查当前用户是否已收藏该设计")
    @GetMapping("/{id}/collected")
    public Result<Map<String, Object>> checkIsCollected(@PathVariable Long id) {
        boolean collected = designService.checkIsCollected(id, 1L);
        return Result.success(Map.of("collected", collected));
    }
}