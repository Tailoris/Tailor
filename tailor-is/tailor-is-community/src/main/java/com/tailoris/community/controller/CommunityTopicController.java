package com.tailoris.community.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityTopic;
import com.tailoris.community.service.CommunityTopicService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/community/topic")
@RequiredArgsConstructor
@Tag(name = "社区话题", description = "话题创建、查询等接口")
public class CommunityTopicController {

    private final CommunityTopicService topicService;

    @Operation(summary = "创建话题", description = "创建新的话题")
    @PostMapping
    public Result<CommunityTopic> create(@RequestBody CommunityTopic topic) {
        return Result.success(topicService.createTopic(topic));
    }

    @Operation(summary = "更新话题", description = "更新指定话题信息")
    @PutMapping("/{id}")
    public Result<CommunityTopic> update(@PathVariable Long id, @RequestBody CommunityTopic topic) {
        topic.setId(id);
        return Result.success(topicService.updateTopic(topic));
    }

    @Operation(summary = "删除话题", description = "删除指定话题")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return Result.success();
    }

    @Operation(summary = "话题详情", description = "获取话题详情")
    @GetMapping("/{id}")
    public Result<CommunityTopic> get(@PathVariable Long id) {
        return Result.success(topicService.getTopic(id));
    }

    @Operation(summary = "热门话题", description = "获取热门话题列表")
    @GetMapping("/hot")
    public Result<List<CommunityTopic>> hot(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(topicService.listHotTopics(limit));
    }

    @Operation(summary = "话题列表", description = "分页查询话题列表")
    @GetMapping("/list")
    public Result<PageResponse<CommunityTopic>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer isHot,
            @RequestParam(required = false) Integer isOfficial) {
        return Result.success(topicService.listTopics(new PageRequest(pageNum, pageSize), isHot, isOfficial));
    }
}
