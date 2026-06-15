package com.tailoris.community.controller;

import com.tailoris.common.result.Result;
import com.tailoris.community.entity.CommunityBlock;
import com.tailoris.community.service.CommunityBlockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区屏蔽 Controller
 * 任务编号: COM-004
 */
@Slf4j
@RestController
@RequestMapping("/api/community/block")
@RequiredArgsConstructor
@Tag(name = "社区屏蔽", description = "COM-004 屏蔽用户")
public class CommunityBlockController {

    private final CommunityBlockService blockService;

    @PostMapping
    public Result<Void> block(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long blockedUserId,
            @RequestParam(required = false) String reason) {
        blockService.blockUser(userId, blockedUserId, reason);
        return Result.success();
    }

    @DeleteMapping("/{blockedUserId}")
    public Result<Void> unblock(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long blockedUserId) {
        blockService.unblockUser(userId, blockedUserId);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<CommunityBlock>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(blockService.listBlocked(userId));
    }
}
