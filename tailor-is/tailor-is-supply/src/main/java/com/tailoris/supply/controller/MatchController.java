package com.tailoris.supply.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.supply.dto.MatchQueryRequest;
import com.tailoris.supply.entity.SupplyDemandPost;
import com.tailoris.supply.service.SupplyMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "智能匹配", description = "供需智能匹配、供应商推荐等接口")
@RestController
@RequestMapping("/api/supply/match")
@RequiredArgsConstructor
public class MatchController {

    private final SupplyMatchService supplyMatchService;

    @Operation(summary = "智能匹配", description = "根据需求智能匹配合适的供应商")
    @GetMapping("/find")
    public Result<PageResponse<SupplyDemandPost>> findMatches(MatchQueryRequest request, PageRequest pageRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(supplyMatchService.findMatches(userId, request, pageRequest));
    }

    @Operation(summary = "推荐供应商", description = "根据城市推荐附近的供应商")
    @GetMapping("/recommend")
    public Result<PageResponse<SupplyDemandPost>> recommendSuppliers(@RequestParam(required = false) String city, PageRequest pageRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(supplyMatchService.recommendSuppliers(userId, city, pageRequest));
    }
}
