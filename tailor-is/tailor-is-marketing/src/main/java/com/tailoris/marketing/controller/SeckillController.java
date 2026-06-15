package com.tailoris.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.marketing.dto.SeckillCreateRequest;
import com.tailoris.marketing.entity.SeckillActivity;
import com.tailoris.marketing.entity.SeckillProduct;
import com.tailoris.marketing.service.SeckillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "秒杀管理", description = "秒杀活动创建、参与、查询等接口")
@RestController
@RequestMapping("/api/marketing/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @Operation(summary = "创建秒杀活动", description = "创建秒杀活动并添加商品")
    @PostMapping("/create")
    public Result<SeckillActivity> createActivity(@Valid @RequestBody SeckillCreateRequest request) {
        SeckillActivity activity = seckillService.createActivity(request);
        return Result.success(activity);
    }

    @Operation(summary = "参与秒杀", description = "用户参与秒杀抢购")
    @PostMapping("/join")
    public Result<Void> joinSeckill(@RequestParam Long seckillProductId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        seckillService.joinSeckill(userId, seckillProductId);
        return Result.success();
    }

    @Operation(summary = "查询秒杀商品列表", description = "分页查询秒杀商品")
    @GetMapping("/products")
    public Result<PageResponse<SeckillProduct>> listProducts(PageRequest pageRequest, @RequestParam(required = false) Long activityId) {
        PageResponse<SeckillProduct> response = seckillService.listSeckillProducts(pageRequest, activityId);
        return Result.success(response);
    }

    @Operation(summary = "查询进行中的秒杀活动", description = "查询当前正在进行的秒杀活动列表")
    @GetMapping("/activities")
    public Result<List<SeckillActivity>> listActivities() {
        return Result.success(seckillService.listActiveActivities());
    }

    @Operation(summary = "查询秒杀商品详情", description = "根据ID查询秒杀商品详情")
    @GetMapping("/product/detail")
    public Result<SeckillProduct> getProductDetail(@RequestParam Long seckillProductId) {
        return Result.success(seckillService.getSeckillProduct(seckillProductId));
    }
}
