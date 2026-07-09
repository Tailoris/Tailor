package com.tailoris.im.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.result.Result;
import com.tailoris.im.dto.SendMessageRequest;
import com.tailoris.im.entity.ImConversation;
import com.tailoris.im.entity.ImMessage;
import com.tailoris.im.service.ImService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.tailoris.common.constant.RoutePortConstant;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Slf4j
@Tag(name = "即时通讯")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_IM)
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

    @Operation(summary = "创建或获取会话")
    @PostMapping("/conversations")
    public Result<ImConversation> createOrGetConversation(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long targetUserId) {
        ImConversation conversation = imService.getOrCreateConversation(userId, targetUserId);
        return Result.success(conversation);
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

    @Operation(summary = "发送消息")
    @PostMapping("/messages")
    public Result<ImMessage> sendMessage(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SendMessageRequest request) {
        ImConversation conversation = imService.getOrCreateConversation(userId, request.getToUserId());
        ImMessage message = new ImMessage();
        message.setFromUserId(userId);
        message.setToUserId(request.getToUserId());
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType());
        message.setConversationId(conversation.getId());
        ImMessage savedMessage = imService.sendMessage(message);
        return Result.success(savedMessage);
    }

    @Operation(summary = "标记消息已读")
    @PutMapping("/messages/{messageId}/read")
    public Result<Void> markMessageRead(@PathVariable Long messageId) {
        imService.markMessageRead(messageId);
        return Result.success();
    }

    @Operation(summary = "标记会话所有消息已读")
    @PutMapping("/conversations/{conversationId}/read")
    public Result<Void> markConversationRead(@PathVariable Long conversationId) {
        List<ImMessage> messages = imService.getMessagesByConversationId(conversationId, 1, Integer.MAX_VALUE);
        messages.forEach(message -> imService.markMessageRead(message.getId()));
        return Result.success();
    }
}