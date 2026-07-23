package com.tailoris.community.controller;

import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityMessage;
import com.tailoris.community.service.CommunityMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区消息 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/community/message")
@RequiredArgsConstructor
@Tag(name = "社区消息", description = "社区消息管理")
public class CommunityMessageController {

    private final CommunityMessageService messageService;

    @Operation(summary = "消息列表", description = "获取当前用户的消息列表")
    @GetMapping("/list")
    public Result<List<CommunityMessage>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "200") int limit) {
        return Result.success(messageService.listMessages(userId, limit));
    }

    @Operation(summary = "未读消息数", description = "获取当前用户的未读消息数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.countUnread(userId));
    }

    @Operation(summary = "标记已读", description = "将指定消息标记为已读")
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        messageService.markAsRead(userId, id);
        return Result.success();
    }

    @Operation(summary = "全部已读", description = "将当前用户所有消息标记为已读")
    @PostMapping("/read-all")
    public Result<Integer> readAll(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.markAllAsRead(userId));
    }
}
