package com.tailoris.community.service;

import com.tailoris.community.entity.CommunityBlock;

import java.util.List;

/**
 * 社区屏蔽 Service
 * 任务编号: COM-004
 */
public interface CommunityBlockService {

    /**
     * 屏蔽用户
     */
    void blockUser(Long userId, Long blockedUserId, String reason);

    /**
     * 取消屏蔽
     */
    void unblockUser(Long userId, Long blockedUserId);

    /**
     * 我的屏蔽列表
     */
    List<CommunityBlock> listBlocked(Long userId);

    /**
     * 是否已屏蔽
     */
    boolean isBlocked(Long userId, Long targetUserId);
}
