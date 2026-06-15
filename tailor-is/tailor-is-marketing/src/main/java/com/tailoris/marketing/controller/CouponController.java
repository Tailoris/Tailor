package com.tailoris.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.marketing.dto.CouponCreateRequest;
import com.tailoris.marketing.dto.CouponReceiveRequest;
import com.tailoris.marketing.entity.CouponTemplate;
import com.tailoris.marketing.entity.UserCoupon;
import com.tailoris.marketing.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@SaCheckLogin
@Tag(name = "优惠券管理", description = "优惠券创建、领取、使用、查询等接口")
@RestController
@RequestMapping("/api/marketing/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "创建优惠券", description = "创建优惠券模板")
    @PostMapping("/create")
    public Result<CouponTemplate> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        CouponTemplate template = couponService.createCoupon(request);
        return Result.success(template);
    }

    @Operation(summary = "领取优惠券", description = "用户领取优惠券")
    @PostMapping("/receive")
    public Result<Void> receiveCoupon(@Valid @RequestBody CouponReceiveRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        couponService.receiveCoupon(userId, request.getCouponId());
        return Result.success();
    }

    @Operation(summary = "使用优惠券", description = "下单时使用优惠券")
    @PostMapping("/use")
    public Result<Void> useCoupon(@RequestParam Long userCouponId, @RequestParam Long orderId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        couponService.useCoupon(userId, userCouponId, orderId);
        return Result.success();
    }

    @Operation(summary = "查询我的优惠券列表", description = "分页查询用户已领取的优惠券")
    @GetMapping("/list")
    public Result<PageResponse<UserCoupon>> listCoupons(PageRequest pageRequest, @RequestParam(required = false) Integer status) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        PageResponse<UserCoupon> response = couponService.listCoupons(userId, pageRequest, status);
        return Result.success(response);
    }

    @Operation(summary = "查询可用优惠券", description = "查询可用于指定订单金额的优惠券")
    @GetMapping("/available")
    public Result<java.util.List<UserCoupon>> getAvailableCoupons(@RequestParam BigDecimal orderAmount, @RequestParam(required = false) Long shopId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(couponService.getAvailableCoupons(userId, orderAmount, shopId));
    }

    @Operation(summary = "查询优惠券模板详情", description = "根据ID查询优惠券模板")
    @GetMapping("/detail")
    public Result<CouponTemplate> getCouponDetail(@RequestParam Long couponId) {
        return Result.success(couponService.getCouponTemplate(couponId));
    }
}
