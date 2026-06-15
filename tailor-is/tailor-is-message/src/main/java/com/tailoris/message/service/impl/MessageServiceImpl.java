package com.tailoris.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.message.entity.MessageInbox;
import com.tailoris.message.entity.MessageTemplate;
import com.tailoris.message.entity.SystemMessage;
import com.tailoris.message.mapper.MessageInboxMapper;
import com.tailoris.message.mapper.MessageTemplateMapper;
import com.tailoris.message.mapper.SystemMessageMapper;
import com.tailoris.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageInboxMapper messageInboxMapper;
    private final SystemMessageMapper systemMessageMapper;
    private final MessageTemplateMapper messageTemplateMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String UNREAD_COUNT_KEY = "message:unread:";

    @Override
    @Transactional
    public void sendSystemMessage(Long userId, String title, String content, Integer type, String relatedType, Long relatedId, String businessNo) {
        SystemMessage message = new SystemMessage();
        message.setId(SnowflakeIdGenerator.getInstance().nextId());
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setPriority(1);
        message.setSenderId(0L);
        message.setSenderType(1);
        message.setTargetType(1);
        message.setTargetUserId(userId);
        message.setRelatedType(relatedType);
        message.setRelatedId(relatedId);
        message.setBusinessNo(businessNo);
        message.setIsPush(0);
        message.setStatus(1);
        systemMessageMapper.insert(message);

        MessageInbox inbox = new MessageInbox();
        inbox.setId(SnowflakeIdGenerator.getInstance().nextId());
        inbox.setUserId(userId);
        inbox.setMessageId(message.getId());
        inbox.setIsRead(0);
        messageInboxMapper.insert(inbox);

        stringRedisTemplate.opsForValue().increment(UNREAD_COUNT_KEY + userId);
    }

    @Override
    @Transactional
    public void sendTemplateMessage(Long userId, String templateCode, Map<String, Object> params) {
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageTemplate::getCode, templateCode);
        wrapper.eq(MessageTemplate::getStatus, 1);
        MessageTemplate template = messageTemplateMapper.selectOne(wrapper);
        if (template == null) {
            log.warn("消息模板不存在或已停用: {}", templateCode);
            return;
        }

        String title = replaceTemplateParams(template.getTitleTemplate(), params);
        String content = replaceTemplateParams(template.getContentTemplate(), params);

        sendSystemMessage(userId, title, content, template.getType(), template.getScene(), null, null);
    }

    @Override
    public PageResponse<MessageInbox> listUserMessages(Long userId, PageRequest pageRequest, Integer isRead, Integer type) {
        Page<MessageInbox> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<MessageInbox> inboxWrapper = new LambdaQueryWrapper<>();
        inboxWrapper.eq(MessageInbox::getUserId, userId);
        if (isRead != null) {
            inboxWrapper.eq(MessageInbox::getIsRead, isRead);
        }
        inboxWrapper.orderByDesc(MessageInbox::getCreateTime);
        inboxWrapper.last("LIMIT " + (pageRequest.getPageNum() * pageRequest.getPageSize()));

        Page<MessageInbox> inboxPage = messageInboxMapper.selectPage(page, inboxWrapper);

        if (!inboxPage.getRecords().isEmpty()) {
            List<Long> messageIds = inboxPage.getRecords().stream()
                    .map(MessageInbox::getMessageId)
                    .toList();
            List<SystemMessage> messages = systemMessageMapper.selectBatchIds(messageIds);
            Map<Long, SystemMessage> messageMap = messages.stream()
                    .collect(java.util.stream.Collectors.toMap(SystemMessage::getId, m -> m));
            for (MessageInbox inbox : inboxPage.getRecords()) {
                SystemMessage msg = messageMap.get(inbox.getMessageId());
                if (msg != null && type != null && !msg.getType().equals(type)) {
                    inboxPage.getRecords().remove(inbox);
                }
            }
        }

        return new PageResponse<>(inboxPage.getRecords(), inboxPage.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long messageId) {
        LambdaUpdateWrapper<MessageInbox> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MessageInbox::getUserId, userId);
        wrapper.eq(MessageInbox::getMessageId, messageId);
        wrapper.eq(MessageInbox::getIsRead, 0);
        wrapper.set(MessageInbox::getIsRead, 1);
        wrapper.set(MessageInbox::getReadTime, LocalDateTime.now());
        messageInboxMapper.update(null, wrapper);

        Long count = getUnreadCount(userId);
        if (count > 0) {
            stringRedisTemplate.opsForValue().set(UNREAD_COUNT_KEY + userId, String.valueOf(count - 1), 1, TimeUnit.HOURS);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<MessageInbox> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MessageInbox::getUserId, userId);
        wrapper.eq(MessageInbox::getIsRead, 0);
        wrapper.set(MessageInbox::getIsRead, 1);
        wrapper.set(MessageInbox::getReadTime, LocalDateTime.now());
        messageInboxMapper.update(null, wrapper);
        stringRedisTemplate.delete(UNREAD_COUNT_KEY + userId);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        String cached = stringRedisTemplate.opsForValue().get(UNREAD_COUNT_KEY + userId);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        LambdaQueryWrapper<MessageInbox> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageInbox::getUserId, userId);
        wrapper.eq(MessageInbox::getIsRead, 0);
        Long count = messageInboxMapper.selectCount(wrapper);
        stringRedisTemplate.opsForValue().set(UNREAD_COUNT_KEY + userId, String.valueOf(count), 1, TimeUnit.HOURS);
        return count;
    }

    private String replaceTemplateParams(String template, Map<String, Object> params) {
        if (template == null || params == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
