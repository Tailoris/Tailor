package com.tailoris.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.marketing.dto.PointsExchangeRequest;
import com.tailoris.marketing.entity.PointsMallProduct;
import com.tailoris.marketing.entity.PointsRecord;
import com.tailoris.marketing.service.PointsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "积分管理", description = "积分查询、兑换、记录等接口")
@RestController
@RequestMapping("/api/marketing/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    @Operation(summary = "兑换积分商品", description = "使用积分兑换积分商城商品")
    @PostMapping("/exchange")
    public Result<Void> exchangePoints(@Valid @RequestBody PointsExchangeRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        pointsService.exchangePoints(userId, request);
        return Result.success();
    }

    @Operation(summary = "查询积分余额", description = "查询当前用户的积分余额")
    @GetMapping("/balance")
    public Result<Integer> getPointsBalance() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(pointsService.getPointsBalance(userId));
    }

    @Operation(summary = "查询积分记录", description = "分页查询用户的积分变动记录")
    @GetMapping("/history")
    public Result<PageResponse<PointsRecord>> getPointsHistory(PageRequest pageRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(pointsService.getPointsHistory(userId, pageRequest));
    }

    @Operation(summary = "积分商城商品列表", description = "分页查询积分商城上架商品")
    @GetMapping("/mall")
    public Result<PageResponse<PointsMallProduct>> listMallProducts(PageRequest pageRequest) {
        return Result.success(pointsService.listPointsMallProducts(pageRequest));
    }
}
