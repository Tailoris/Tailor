package com.tailoris.im.service;

import com.tailoris.im.entity.ImConversation;
import com.tailoris.im.entity.ImMessage;

import java.util.List;

public interface ImService {

    ImMessage sendMessage(ImMessage message);

    List<ImMessage> getMessagesByConversationId(Long conversationId, int pageNum, int pageSize);

    void markMessageRead(Long messageId);

    ImConversation getOrCreateConversation(Long userId1, Long userId2);

    List<ImConversation> listUserConversations(Long userId);

    void updateConversationAfterMessage(Long conversationId, String lastMessage);
}