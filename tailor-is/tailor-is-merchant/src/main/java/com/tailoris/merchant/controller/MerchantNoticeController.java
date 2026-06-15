package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.entity.MerchantNotice;
import com.tailoris.merchant.service.IMerchantNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Merchant notice controller
 * <p>商家公告控制器</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
@Tag(name = "Merchant Notice API", description = "商家公告管理接口")
@RestController
@RequestMapping("/api/v1/merchant/notices")
@RequiredArgsConstructor
@Validated
public class MerchantNoticeController {

    private final IMerchantNoticeService noticeService;

    @Operation(summary = "Get notice list")
    @GetMapping
    @SaCheckLogin
    @RateLimit(key = "merchant_notice_list", permitsPerSecond = 5)
    public Result<IPage<MerchantNotice>> listNotices(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer noticeType) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(noticeService.listNotices(merchantId, pageNum, pageSize, noticeType));
    }

    @Operation(summary = "Get unread count")
    @GetMapping("/unread-count")
    @SaCheckLogin
    public Result<Long> countUnread(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        return Result.success(noticeService.countUnread(merchantId));
    }

    @Operation(summary = "Get notice detail")
    @GetMapping("/{id}")
    @SaCheckLogin
    public Result<MerchantNotice> getDetail(@PathVariable Long id) {
        return Result.success(noticeService.getById(id));
    }

    @Operation(summary = "Mark as read")
    @PutMapping("/{id}/read")
    @SaCheckLogin
    public Result<Void> markAsRead(@PathVariable @Parameter(description = "Notice ID") Long id) {
        boolean success = noticeService.markAsRead(id);
        return success ? Result.success() : Result.fail("Failed to mark as read");
    }

    @Operation(summary = "Mark all as read")
    @PutMapping("/read-all")
    @SaCheckLogin
    public Result<Integer> markAllAsRead(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        int count = noticeService.markAllAsRead(merchantId);
        return Result.success(count);
    }
}
