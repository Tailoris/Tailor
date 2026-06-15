package com.tailoris.community.controller;

import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityMessage;
import com.tailoris.community.service.CommunityMessageService;
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
@RequestMapping("/api/community/message")
@RequiredArgsConstructor
@Tag(name = "社区消息", description = "社区消息管理")
public class CommunityMessageController {

    private final CommunityMessageService messageService;

    @GetMapping("/list")
    public Result<List<CommunityMessage>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "200") int limit) {
        return Result.success(messageService.listMessages(userId, limit));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.countUnread(userId));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        messageService.markAsRead(userId, id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Integer> readAll(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(messageService.markAllAsRead(userId));
    }
}
