package com.tailoris.community.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.service.CommunityTopicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区话题 Controller
 * 任务编号: COM-005 配套
 */
@Slf4j
@RestController
@RequestMapping("/api/community/topic")
@RequiredArgsConstructor
@Tag(name = "社区话题", description = "话题创建、查询等接口")
public class CommunityTopicController {

    private final CommunityTopicService topicService;

    @PostMapping
    public Result<CommunityTopic> create(@RequestBody CommunityTopic topic) {
        return Result.success(topicService.createTopic(topic));
    }

    @PutMapping("/{id}")
    public Result<CommunityTopic> update(@PathVariable Long id, @RequestBody CommunityTopic topic) {
        topic.setId(id);
        return Result.success(topicService.updateTopic(topic));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<CommunityTopic> get(@PathVariable Long id) {
        return Result.success(topicService.getTopic(id));
    }

    @GetMapping("/hot")
    public Result<List<CommunityTopic>> hot(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(topicService.listHotTopics(limit));
    }

    @GetMapping("/list")
    public Result<PageResponse<CommunityTopic>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer isHot,
            @RequestParam(required = false) Integer isOfficial) {
        return Result.success(topicService.listTopics(new PageRequest(pageNum, pageSize), isHot, isOfficial));
    }
}
