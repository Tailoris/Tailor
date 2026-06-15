package com.tailoris.supply.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.supply.dto.SupplyPostRequest;
import com.tailoris.supply.entity.SupplyDemandPost;
import com.tailoris.supply.service.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "供需管理", description = "供需帖子发布、编辑、删除、查询等接口")
@RestController
@RequestMapping("/api/supply")
@RequiredArgsConstructor
public class SupplyController {

    private final SupplyService supplyService;

    @Operation(summary = "发布供需帖子", description = "发布供应或需求帖子")
    @PostMapping("/create")
    public Result<SupplyDemandPost> createPost(@Valid @RequestBody SupplyPostRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(supplyService.createPost(userId, request));
    }

    @Operation(summary = "编辑供需帖子", description = "编辑自己的供需帖子")
    @PutMapping("/update/{postId}")
    public Result<SupplyDemandPost> updatePost(@PathVariable Long postId, @Valid @RequestBody SupplyPostRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(supplyService.updatePost(userId, postId, request));
    }

    @Operation(summary = "删除供需帖子", description = "删除自己的供需帖子")
    @DeleteMapping("/delete/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        supplyService.deletePost(userId, postId);
        return Result.success();
    }

    @Operation(summary = "帖子详情", description = "查看供需帖子详情")
    @GetMapping("/detail/{postId}")
    public Result<SupplyDemandPost> getPostDetail(@PathVariable Long postId) {
        return Result.success(supplyService.getPostDetail(postId));
    }

    @Operation(summary = "帖子列表", description = "分页查询供需帖子列表")
    @GetMapping("/list")
    public Result<PageResponse<SupplyDemandPost>> listPosts(PageRequest pageRequest, @RequestParam(required = false) Integer postType, @RequestParam(required = false) Long categoryId, @RequestParam(required = false) String city) {
        return Result.success(supplyService.listPosts(pageRequest, postType, categoryId, city));
    }

    @Operation(summary = "我的帖子列表", description = "分页查询自己的供需帖子")
    @GetMapping("/my")
    public Result<PageResponse<SupplyDemandPost>> listMyPosts(PageRequest pageRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(supplyService.listUserPosts(userId, pageRequest));
    }
}
