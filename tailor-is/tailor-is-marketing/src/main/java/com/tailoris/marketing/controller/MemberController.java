package com.tailoris.marketing.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.marketing.dto.MemberLevelRequest;
import com.tailoris.marketing.entity.MemberLevel;
import com.tailoris.marketing.entity.ShopMember;
import com.tailoris.marketing.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "会员管理", description = "会员等级查询、店铺会员设置等接口")
@RestController
@RequestMapping("/api/marketing/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "查询会员等级体系", description = "获取所有会员等级及对应权益")
    @GetMapping("/levels")
    public Result<List<MemberLevel>> getMemberLevels() {
        return Result.success(memberService.getMemberBenefits());
    }

    @Operation(summary = "升级会员", description = "根据用户积分自动升级会员等级")
    @PostMapping("/upgrade")
    public Result<Void> upgradeMember() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        memberService.upgradeMember(userId);
        return Result.success();
    }

    @Operation(summary = "设置店铺会员", description = "设置用户在指定店铺的会员等级")
    @PostMapping("/shop/set")
    public Result<Void> setShopMember(@Valid @RequestBody MemberLevelRequest request) {
        memberService.setShopMember(request);
        return Result.success();
    }

    @Operation(summary = "查询店铺会员信息", description = "查询用户在指定店铺的会员信息")
    @GetMapping("/shop/info")
    public Result<ShopMember> getShopMember(@RequestParam Long shopId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(memberService.getShopMember(userId, shopId));
    }
}
