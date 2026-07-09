package com.tailoris.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.design.dto.DesignQueryRequest;
import com.tailoris.design.dto.DesignRequest;
import com.tailoris.design.entity.Design;
import com.tailoris.design.service.DesignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/design")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

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

    @DeleteMapping("/{id}")
    public Result<Void> deleteDesign(@PathVariable Long id) {
        designService.deleteDesign(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Design> getDesignById(@PathVariable Long id) {
        Design design = designService.getDesignById(id);
        return Result.success(design);
    }

    @GetMapping("/page")
    public Result<PageResponse<Design>> getDesignPage(@ModelAttribute DesignQueryRequest queryRequest) {
        IPage<Design> page = designService.getDesignPage(queryRequest);
        return Result.success(PageResponse.of(page));
    }

    @GetMapping("/hot")
    public Result<List<Design>> getHotDesigns(@RequestParam(defaultValue = "10") Integer limit) {
        List<Design> designs = designService.getHotDesigns(limit);
        return Result.success(designs);
    }

    @GetMapping("/new")
    public Result<List<Design>> getNewDesigns(@RequestParam(defaultValue = "10") Integer limit) {
        List<Design> designs = designService.getNewDesigns(limit);
        return Result.success(designs);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Design>> getDesignsByUserId(@PathVariable Long userId) {
        List<Design> designs = designService.getDesignsByUserId(userId);
        return Result.success(designs);
    }

    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long id) {
        boolean liked = designService.toggleLike(id, 1L);
        return Result.success(Map.of("liked", liked));
    }

    @PostMapping("/{id}/collect")
    public Result<Map<String, Object>> toggleCollection(@PathVariable Long id) {
        boolean collected = designService.toggleCollection(id, 1L);
        return Result.success(Map.of("collected", collected));
    }

    @GetMapping("/{id}/liked")
    public Result<Map<String, Object>> checkIsLiked(@PathVariable Long id) {
        boolean liked = designService.checkIsLiked(id, 1L);
        return Result.success(Map.of("liked", liked));
    }

    @GetMapping("/{id}/collected")
    public Result<Map<String, Object>> checkIsCollected(@PathVariable Long id) {
        boolean collected = designService.checkIsCollected(id, 1L);
        return Result.success(Map.of("collected", collected));
    }
}