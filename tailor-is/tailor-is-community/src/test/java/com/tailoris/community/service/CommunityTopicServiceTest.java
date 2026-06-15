package com.tailoris.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.mapper.CommunityTopicMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityTopicService 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityTopicServiceTest {

    @Mock
    private CommunityTopicMapper topicMapper;

    @InjectMocks
    private CommunityTopicService topicService;

    @Test
    @DisplayName("创建话题 - 成功")
    void testCreateTopic_Success() {
        CommunityTopic topic = new CommunityTopic();
        topic.setTopicName("新话题");
        when(topicMapper.selectByName("新话题")).thenReturn(null);
        doReturn(1).when(topicMapper).insert(any(CommunityTopic.class));

        try (var mock = mockStatic(com.tailoris.common.util.SnowflakeIdGenerator.class)) {
            var gen = mock(com.tailoris.common.util.SnowflakeIdGenerator.class);
            mock.when(com.tailoris.common.util.SnowflakeIdGenerator::getInstance).thenReturn(gen);
            when(gen.nextId()).thenReturn(12345L);

            CommunityTopic result = topicService.createTopic(topic);

            assertNotNull(result);
            assertEquals("新话题", result.getTopicName());
            assertEquals(0, result.getPostCount());
            assertEquals(1, result.getStatus());
        }
    }

    @Test
    @DisplayName("创建话题 - 名称为空抛异常")
    void testCreateTopic_EmptyName() {
        CommunityTopic topic = new CommunityTopic();
        topic.setTopicName("");
        assertThrows(BusinessException.class, () -> topicService.createTopic(topic));
    }

    @Test
    @DisplayName("创建话题 - 名称为null抛异常")
    void testCreateTopic_NullName() {
        CommunityTopic topic = new CommunityTopic();
        topic.setTopicName(null);
        assertThrows(BusinessException.class, () -> topicService.createTopic(topic));
    }

    @Test
    @DisplayName("创建话题 - 已存在返回已有话题")
    void testCreateTopic_AlreadyExists() {
        CommunityTopic existing = new CommunityTopic();
        existing.setId(100L);
        existing.setTopicName("已存在话题");
        when(topicMapper.selectByName("已存在话题")).thenReturn(existing);

        CommunityTopic topic = new CommunityTopic();
        topic.setTopicName("已存在话题");

        CommunityTopic result = topicService.createTopic(topic);

        assertEquals(100L, result.getId());
        verify(topicMapper, never()).insert(any(CommunityTopic.class));
    }

    @Test
    @DisplayName("更新话题 - 成功")
    void testUpdateTopic_Success() {
        CommunityTopic existing = new CommunityTopic();
        existing.setId(100L);
        when(topicMapper.selectById(100L)).thenReturn(existing);
        doReturn(1).when(topicMapper).updateById((CommunityTopic) any());

        CommunityTopic topic = new CommunityTopic();
        topic.setId(100L);
        topic.setTopicName("更新后话题");

        CommunityTopic result = topicService.updateTopic(topic);

        assertNotNull(result);
        assertEquals("更新后话题", result.getTopicName());
    }

    @Test
    @DisplayName("更新话题 - 不存在抛异常")
    void testUpdateTopic_NotFound() {
        when(topicMapper.selectById(999L)).thenReturn(null);
        CommunityTopic topic = new CommunityTopic();
        topic.setId(999L);
        assertThrows(BusinessException.class, () -> topicService.updateTopic(topic));
    }

    @Test
    @DisplayName("删除话题 - 成功")
    void testDeleteTopic_Success() {
        CommunityTopic existing = new CommunityTopic();
        existing.setId(100L);
        existing.setStatus(1);
        when(topicMapper.selectById(100L)).thenReturn(existing);
        doReturn(1).when(topicMapper).updateById((CommunityTopic) any());

        topicService.deleteTopic(100L);

        verify(topicMapper).updateById(Mockito.<CommunityTopic>any());
    }

    @Test
    @DisplayName("删除话题 - 不存在抛异常")
    void testDeleteTopic_NotFound() {
        when(topicMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> topicService.deleteTopic(999L));
    }

    @Test
    @DisplayName("获取话题详情")
    void testGetTopic() {
        CommunityTopic topic = new CommunityTopic();
        topic.setId(100L);
        topic.setTopicName("测试话题");
        when(topicMapper.selectById(100L)).thenReturn(topic);

        CommunityTopic result = topicService.getTopic(100L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    @DisplayName("热门话题列表")
    void testListHotTopics() {
        CommunityTopic topic = new CommunityTopic();
        topic.setTopicName("热门");
        when(topicMapper.selectHotTopics(10)).thenReturn(Arrays.asList(topic));

        List<CommunityTopic> result = topicService.listHotTopics(10);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("话题列表 - 分页查询")
    void testListTopics() {
        Page<CommunityTopic> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(topicMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityTopic> result = topicService.listTopics(new PageRequest(1, 20), null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("话题列表 - 按热门过滤")
    void testListTopics_ByHot() {
        Page<CommunityTopic> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(topicMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityTopic> result = topicService.listTopics(new PageRequest(1, 20), 1, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("话题列表 - 按官方过滤")
    void testListTopics_ByOfficial() {
        Page<CommunityTopic> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        doReturn(page).when(topicMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));

        PageResponse<CommunityTopic> result = topicService.listTopics(new PageRequest(1, 20), null, 1);

        assertNotNull(result);
    }

    @Test
    @DisplayName("增加话题帖子数")
    void testIncrPostCount() {
        when(topicMapper.incrPostCount(100L)).thenReturn(1);

        topicService.incrPostCount(100L);

        verify(topicMapper).incrPostCount(100L);
    }
}
