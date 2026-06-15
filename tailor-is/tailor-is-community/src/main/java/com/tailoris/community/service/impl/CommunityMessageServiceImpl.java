package com.tailoris.community.service.impl;

import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.entity.CommunityMessage;
import com.tailoris.community.mapper.CommunityMessageMapper;
import com.tailoris.community.service.CommunityMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区消息 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityMessageServiceImpl implements CommunityMessageService {

    private final CommunityMessageMapper messageMapper;

    @Override
    @Transactional
    public CommunityMessage sendMessage(Long userId, Long senderId, Integer msgType,
                                         String bizType, Long bizId, String title, String content) {
        CommunityMessage message = new CommunityMessage();
        message.setId(SnowflakeIdGenerator.getInstance().nextId());
        message.setUserId(userId);
        message.setSenderId(senderId);
        message.setMsgType(msgType);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setTitle(title);
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);
        return message;
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long messageId) {
        CommunityMessage message = messageMapper.selectById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            return;
        }
        if (message.getIsRead() != null && message.getIsRead() == 1) {
            return;
        }
        message.setIsRead(1);
        message.setReadTime(LocalDateTime.now());
        messageMapper.updateById(message);
    }

    @Override
    @Transactional
    public int markAllAsRead(Long userId) {
        return messageMapper.markAllAsRead(userId);
    }

    @Override
    public List<CommunityMessage> listMessages(Long userId, int limit) {
        return messageMapper.selectByUser(userId);
    }

    @Override
    public long countUnread(Long userId) {
        return messageMapper.countUnread(userId);
    }
}
