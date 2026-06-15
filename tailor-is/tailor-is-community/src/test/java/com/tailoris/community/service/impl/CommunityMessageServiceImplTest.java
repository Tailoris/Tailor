package com.tailoris.community.service.impl;

import com.tailoris.community.entity.CommunityMessage;
import com.tailoris.community.mapper.CommunityMessageMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityMessageServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityMessageServiceImplTest {

    @Mock
    private CommunityMessageMapper messageMapper;

    @InjectMocks
    private CommunityMessageServiceImpl messageService;

    @Test
    @DisplayName("发送消息 - 成功")
    void testSendMessage_Success() {
        doReturn(1).when(messageMapper).insert(any(CommunityMessage.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            CommunityMessage result = messageService.sendMessage(1L, 2L, 1, "like", 10L, "点赞通知", "有人赞了你");

            assertNotNull(result);
            assertEquals(1L, result.getUserId());
            assertEquals(2L, result.getSenderId());
            assertEquals(0, result.getIsRead());
        }
    }

    @Test
    @DisplayName("标记已读 - 成功")
    void testMarkAsRead_Success() {
        CommunityMessage message = new CommunityMessage();
        message.setId(100L);
        message.setUserId(1L);
        message.setIsRead(0);
        when(messageMapper.selectById(100L)).thenReturn(message);
        doReturn(1).when(messageMapper).updateById(any(CommunityMessage.class));

        messageService.markAsRead(1L, 100L);

        verify(messageMapper).updateById(any(CommunityMessage.class));
    }

    @Test
    @DisplayName("标记已读 - 消息不存在则跳过")
    void testMarkAsRead_NotFound() {
        when(messageMapper.selectById(999L)).thenReturn(null);

        messageService.markAsRead(1L, 999L);

        verify(messageMapper, never()).updateById(any(CommunityMessage.class));
    }

    @Test
    @DisplayName("标记已读 - 非本人消息跳过")
    void testMarkAsRead_NotOwner() {
        CommunityMessage message = new CommunityMessage();
        message.setId(100L);
        message.setUserId(999L);
        when(messageMapper.selectById(100L)).thenReturn(message);

        messageService.markAsRead(1L, 100L);

        verify(messageMapper, never()).updateById(any(CommunityMessage.class));
    }

    @Test
    @DisplayName("标记已读 - 已读消息跳过")
    void testMarkAsRead_AlreadyRead() {
        CommunityMessage message = new CommunityMessage();
        message.setId(100L);
        message.setUserId(1L);
        message.setIsRead(1);
        when(messageMapper.selectById(100L)).thenReturn(message);

        messageService.markAsRead(1L, 100L);

        verify(messageMapper, never()).updateById(any(CommunityMessage.class));
    }

    @Test
    @DisplayName("全部标记已读")
    void testMarkAllAsRead() {
        when(messageMapper.markAllAsRead(1L)).thenReturn(5);

        int result = messageService.markAllAsRead(1L);

        assertEquals(5, result);
    }

    @Test
    @DisplayName("消息列表")
    void testListMessages() {
        CommunityMessage msg = new CommunityMessage();
        msg.setId(1L);
        when(messageMapper.selectByUser(1L)).thenReturn(Arrays.asList(msg));

        List<CommunityMessage> result = messageService.listMessages(1L, 20);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("未读消息计数")
    void testCountUnread() {
        when(messageMapper.countUnread(1L)).thenReturn(3L);

        long result = messageService.countUnread(1L);

        assertEquals(3L, result);
    }
}
