package com.tailoris.community.service;

import com.tailoris.community.entity.CommunityMessage;

import java.util.List;

/**
 * 社区消息 Service
 */
public interface CommunityMessageService {

    /**
     * 发送消息
     */
    CommunityMessage sendMessage(Long userId, Long senderId, Integer msgType,
                                 String bizType, Long bizId, String title, String content);

    /**
     * 标记已读
     */
    void markAsRead(Long userId, Long messageId);

    /**
     * 全部已读
     */
    int markAllAsRead(Long userId);

    /**
     * 查询用户消息
     */
    List<CommunityMessage> listMessages(Long userId, int limit);

    /**
     * 未读数
     */
    long countUnread(Long userId);
}
