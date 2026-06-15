package com.tailoris.community.service;

import com.tailoris.community.entity.CommunityFavorite;
import com.tailoris.community.entity.CommunityFollow;

import java.util.Map;

public interface CommunityInteractionService {

    void like(Long userId, Integer targetType, Long targetId);

    void unlike(Long userId, Integer targetType, Long targetId);

    void favorite(Long userId, Long postId);

    void unfavorite(Long userId, Long postId);

    void follow(Long userId, Long followUserId);

    void unfollow(Long userId, Long followUserId);

    boolean isLiked(Long userId, Integer targetType, Long targetId);

    boolean isFavorite(Long userId, Long postId);

    boolean isFollowed(Long userId, Long followUserId);

    Long getFavoriteCount(Long userId);

    Long getFollowerCount(Long userId);

    Long getFollowingCount(Long userId);

    Map<String, Object> share(Long userId, Long postId, String shareType);

    Long getShareCount(Long postId);
}
