package com.tailoris.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.im.entity.ImConversation;
import com.tailoris.im.entity.ImMessage;
import com.tailoris.im.mapper.ImConversationMapper;
import com.tailoris.im.mapper.ImMessageMapper;
import com.tailoris.im.service.ImService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImServiceImpl extends ServiceImpl<ImMessageMapper, ImMessage> implements ImService {

    private final ImMessageMapper imMessageMapper;
    private final ImConversationMapper imConversationMapper;

    @Override
    public ImMessage sendMessage(ImMessage message) {
        message.setSentAt(LocalDateTime.now());
        message.setStatus(0);
        imMessageMapper.insert(message);
        updateConversationAfterMessage(message.getConversationId(), message.getContent());
        return message;
    }

    @Override
    public List<ImMessage> getMessagesByConversationId(Long conversationId, int pageNum, int pageSize) {
        Page<ImMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ImMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImMessage::getConversationId, conversationId)
                .orderByDesc(ImMessage::getSentAt);
        return imMessageMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public void markMessageRead(Long messageId) {
        ImMessage message = imMessageMapper.selectById(messageId);
        if (message != null) {
            message.setStatus(1);
            imMessageMapper.updateById(message);
        }
    }

    @Override
    public ImConversation getOrCreateConversation(Long userId1, Long userId2) {
        LambdaQueryWrapper<ImConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(ImConversation::getUserId1, userId1).eq(ImConversation::getUserId2, userId2)
                .or()
                .eq(ImConversation::getUserId1, userId2).eq(ImConversation::getUserId2, userId1));
        ImConversation conversation = imConversationMapper.selectOne(wrapper);
        if (conversation == null) {
            conversation = new ImConversation();
            conversation.setUserId1(userId1);
            conversation.setUserId2(userId2);
            conversation.setUnreadCount(0);
            imConversationMapper.insert(conversation);
        }
        return conversation;
    }

    @Override
    public List<ImConversation> listUserConversations(Long userId) {
        LambdaQueryWrapper<ImConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ImConversation::getUserId1, userId)
                .or()
                .eq(ImConversation::getUserId2, userId)
                .orderByDesc(ImConversation::getLastMessageAt);
        return imConversationMapper.selectList(wrapper);
    }

    @Override
    public void updateConversationAfterMessage(Long conversationId, String lastMessage) {
        ImConversation conversation = imConversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setLastMessage(lastMessage);
            conversation.setLastMessageAt(LocalDateTime.now());
            Integer unread = conversation.getUnreadCount();
            conversation.setUnreadCount(unread != null ? unread + 1 : 1);
            imConversationMapper.updateById(conversation);
        }
    }
}