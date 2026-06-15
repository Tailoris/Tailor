package com.tailoris.message.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.message.entity.MessageInbox;
import com.tailoris.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SaCheckLogin
@Tag(name = "消息管理", description = "系统消息查询、已读标记、未读数等接口")
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "查询我的消息列表", description = "分页查询用户的系统消息")
    @GetMapping("/list")
    public Result<PageResponse<MessageInbox>> listMessages(PageRequest pageRequest, @RequestParam(required = false) Integer isRead, @RequestParam(required = false) Integer type) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(messageService.listUserMessages(userId, pageRequest, isRead, type));
    }

    @Operation(summary = "标记消息已读", description = "将指定消息标记为已读")
    @PostMapping("/read/{messageId}")
    public Result<Void> markAsRead(@PathVariable Long messageId) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        messageService.markAsRead(userId, messageId);
        return Result.success();
    }

    @Operation(summary = "全部标记已读", description = "将所有消息标记为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        messageService.markAllAsRead(userId);
        return Result.success();
    }

    @Operation(summary = "未读消息数", description = "获取当前用户的未读消息数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(messageService.getUnreadCount(userId));
    }

    @Operation(summary = "发送模板消息", description = "根据模板发送系统消息")
    @PostMapping("/send-template")
    public Result<Void> sendTemplateMessage(@RequestParam String templateCode, @RequestParam Map<String, Object> params) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        messageService.sendTemplateMessage(userId, templateCode, params);
        return Result.success();
    }
}
