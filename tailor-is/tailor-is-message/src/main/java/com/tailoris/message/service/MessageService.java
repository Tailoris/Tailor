package com.tailoris.message.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.message.entity.MessageInbox;
import com.tailoris.message.entity.SystemMessage;

import java.util.Map;

public interface MessageService {

    void sendSystemMessage(Long userId, String title, String content, Integer type, String relatedType, Long relatedId, String businessNo);

    void sendTemplateMessage(Long userId, String templateCode, Map<String, Object> params);

    PageResponse<MessageInbox> listUserMessages(Long userId, PageRequest pageRequest, Integer isRead, Integer type);

    void markAsRead(Long userId, Long messageId);

    void markAllAsRead(Long userId);

    Long getUnreadCount(Long userId);
}
