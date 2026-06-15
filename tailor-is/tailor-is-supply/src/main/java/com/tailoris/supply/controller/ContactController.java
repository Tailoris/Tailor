package com.tailoris.supply.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.supply.dto.ContactRequest;
import com.tailoris.supply.entity.SupplyContactRecord;
import com.tailoris.supply.service.SupplyContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@SaCheckLogin
@Tag(name = "联系管理", description = "供需联系、回复等接口")
@RestController
@RequestMapping("/api/supply/contact")
@RequiredArgsConstructor
public class ContactController {

    private final SupplyContactService supplyContactService;

    @Operation(summary = "发起联系", description = "向供需帖子发布者发起联系")
    @PostMapping("/create")
    public Result<SupplyContactRecord> createContact(@Valid @RequestBody ContactRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(supplyContactService.createContact(userId, request));
    }

    @Operation(summary = "我的联系记录", description = "分页查询联系记录")
    @GetMapping("/list")
    public Result<PageResponse<SupplyContactRecord>> listContacts(PageRequest pageRequest) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(supplyContactService.listContacts(userId, pageRequest));
    }

    @Operation(summary = "回复联系", description = "回复收到的联系消息")
    @PostMapping("/reply/{contactId}")
    public Result<Void> respondContact(@PathVariable Long contactId, @RequestParam String replyMessage) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        supplyContactService.respondContact(contactId, userId, replyMessage);
        return Result.success();
    }
}
