package com.tailoris.community.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.mapper.CommunityTopicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 社区话题 Service
 * 任务编号: COM-005 配套
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityTopicService {

    private final CommunityTopicMapper topicMapper;

    public CommunityTopic createTopic(CommunityTopic topic) {
        if (topic.getTopicName() == null || topic.getTopicName().isEmpty()) {
            throw new BusinessException("话题名称不能为空");
        }
        CommunityTopic exist = topicMapper.selectByName(topic.getTopicName());
        if (exist != null) {
            return exist;
        }
        topic.setId(SnowflakeIdGenerator.getInstance().nextId());
        topic.setPostCount(0);
        topic.setFollowCount(0);
        topic.setViewCount(0L);
        topic.setStatus(1);
        if (topic.getIsHot() == null) topic.setIsHot(0);
        if (topic.getIsOfficial() == null) topic.setIsOfficial(0);
        topicMapper.insert(topic);
        log.info("创建话题: id={}, name={}", topic.getId(), topic.getTopicName());
        return topic;
    }

    public CommunityTopic updateTopic(CommunityTopic topic) {
        CommunityTopic existing = topicMapper.selectById(topic.getId());
        if (existing == null) {
            throw new BusinessException("话题不存在");
        }
        topicMapper.updateById(topic);
        return topic;
    }

    public void deleteTopic(Long topicId) {
        CommunityTopic existing = topicMapper.selectById(topicId);
        if (existing == null) {
            throw new BusinessException("话题不存在");
        }
        existing.setStatus(0);
        topicMapper.updateById(existing);
    }

    public CommunityTopic getTopic(Long topicId) {
        return topicMapper.selectById(topicId);
    }

    public List<CommunityTopic> listHotTopics(int limit) {
        return topicMapper.selectHotTopics(limit);
    }

    public PageResponse<CommunityTopic> listTopics(PageRequest pageRequest, Integer isHot, Integer isOfficial) {
        Page<CommunityTopic> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<CommunityTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityTopic::getStatus, 1);
        if (isHot != null) {
            wrapper.eq(CommunityTopic::getIsHot, isHot);
        }
        if (isOfficial != null) {
            wrapper.eq(CommunityTopic::getIsOfficial, isOfficial);
        }
        wrapper.orderByDesc(CommunityTopic::getIsOfficial);
        wrapper.orderByDesc(CommunityTopic::getPostCount);
        Page<CommunityTopic> result = topicMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Transactional
    public void incrPostCount(Long topicId) {
        topicMapper.incrPostCount(topicId);
    }
}
