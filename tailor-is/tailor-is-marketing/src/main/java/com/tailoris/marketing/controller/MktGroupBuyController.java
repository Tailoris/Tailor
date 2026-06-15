package com.tailoris.marketing.controller;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.marketing.entity.MktGroupBuy;
import com.tailoris.marketing.entity.MktGroupBuyInstance;
import com.tailoris.marketing.entity.MktGroupBuyMember;
import com.tailoris.marketing.service.MktGroupBuyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 拼团活动 Controller
 * 任务编号: MKT-002
 */
@Slf4j
@RestController
@RequestMapping("/api/marketing/group-buy")
@RequiredArgsConstructor
@Tag(name = "拼团活动", description = "MKT-002 拼团活动相关接口")
public class MktGroupBuyController {

    private final MktGroupBuyService groupBuyService;

    /** 创建拼团活动 */
    @PostMapping("/activity")
    public Result<MktGroupBuy> createActivity(@RequestBody MktGroupBuy activity) {
        return Result.success(groupBuyService.createActivity(activity));
    }

    /** 更新活动 */
    @PutMapping("/activity/{id}")
    public Result<MktGroupBuy> updateActivity(@PathVariable Long id, @RequestBody MktGroupBuy activity) {
        activity.setId(id);
        return Result.success(groupBuyService.updateActivity(activity));
    }

    /** 取消活动 */
    @PostMapping("/activity/{id}/cancel")
    public Result<Void> cancelActivity(@PathVariable Long id) {
        groupBuyService.cancelActivity(id);
        return Result.success();
    }

    /** 活动详情 */
    @GetMapping("/activity/{id}")
    public Result<MktGroupBuy> getActivityDetail(@PathVariable Long id) {
        return Result.success(groupBuyService.getActivityDetail(id));
    }

    /** 活动列表 */
    @GetMapping("/activity/list")
    public Result<PageResponse<MktGroupBuy>> listActivities(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Integer status) {
        return Result.success(groupBuyService.listActivities(
                new PageRequest(pageNum, pageSize), shopId, status));
    }

    /** 开团 */
    @PostMapping("/open")
    public Result<MktGroupBuyInstance> openGroup(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long activityId) {
        return Result.success(groupBuyService.openGroup(userId, activityId));
    }

    /** 加入团 */
    @PostMapping("/join")
    public Result<MktGroupBuyInstance> joinGroup(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long instanceId) {
        return Result.success(groupBuyService.joinGroup(userId, instanceId));
    }

    /** 团详情 */
    @GetMapping("/instance/{id}")
    public Result<MktGroupBuyInstance> getInstanceDetail(@PathVariable Long id) {
        return Result.success(groupBuyService.getInstanceDetail(id));
    }

    /** 用户参与的团 */
    @GetMapping("/user/instances")
    public Result<PageResponse<MktGroupBuyInstance>> listUserInstances(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(groupBuyService.listUserInstances(
                userId, new PageRequest(pageNum, pageSize), status));
    }

    /** 可加入的团 */
    @GetMapping("/joinable")
    public Result<List<MktGroupBuyInstance>> listJoinableGroups(@RequestParam Long activityId) {
        return Result.success(groupBuyService.listJoinableGroups(activityId));
    }

    /** 团成员 */
    @GetMapping("/instance/{id}/members")
    public Result<List<MktGroupBuyMember>> listGroupMembers(@PathVariable Long id) {
        return Result.success(groupBuyService.listGroupMembers(id));
    }
}
