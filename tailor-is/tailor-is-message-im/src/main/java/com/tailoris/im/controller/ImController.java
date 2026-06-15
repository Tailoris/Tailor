package com.tailoris.im.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.result.Result;
import com.tailoris.im.entity.ImConversation;
import com.tailoris.im.entity.ImMessage;
import com.tailoris.im.service.ImService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Slf4j
@Tag(name = "即时通讯")
@RestController
@RequestMapping("/api/v1/im")
@RequiredArgsConstructor
public class ImController {

    private final ImService imService;

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public Result<List<ImConversation>> listConversations(
            @RequestHeader("X-User-Id") Long userId) {
        List<ImConversation> conversations = imService.listUserConversations(userId);
        return Result.success(conversations);
    }

    @Operation(summary = "消息列表")
    @GetMapping("/messages/{conversationId}")
    public Result<List<ImMessage>> listMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<ImMessage> messages = imService.getMessagesByConversationId(conversationId, pageNum, pageSize);
        return Result.success(messages);
    }
}