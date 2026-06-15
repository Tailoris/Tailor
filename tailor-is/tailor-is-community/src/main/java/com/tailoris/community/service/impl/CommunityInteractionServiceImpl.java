package com.tailoris.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SpringSnowflakeIdGenerator;
import com.tailoris.community.entity.CommunityFavorite;
import com.tailoris.community.entity.CommunityFollow;
import com.tailoris.community.entity.CommunityLike;
import com.tailoris.community.entity.CommunityPost;
import com.tailoris.community.mapper.CommunityFavoriteMapper;
import com.tailoris.community.mapper.CommunityFollowMapper;
import com.tailoris.community.mapper.CommunityLikeMapper;
import com.tailoris.community.mapper.CommunityPostMapper;
import com.tailoris.community.service.CommunityInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 社区互动 Service 实现 - 增强版
 * 任务编号: COM-002, COM-003
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityInteractionServiceImpl implements CommunityInteractionService {

    private final CommunityLikeMapper communityLikeMapper;
    private final CommunityFavoriteMapper communityFavoriteMapper;
    private final CommunityFollowMapper communityFollowMapper;
    private final CommunityPostMapper communityPostMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SpringSnowflakeIdGenerator snowflakeIdGenerator;

    private static final String LIKE_KEY = "community:like:";
    private static final String FAVORITE_KEY = "community:fav:";
    private static final String FOLLOW_KEY = "community:follow:";
    private static final long CACHE_TTL_DAYS = 7;

    @Override
    @Transactional
    public void like(Long userId, Integer targetType, Long targetId) {
        if (userId == null || targetType == null || targetId == null) {
            throw new BusinessException("参数错误");
        }
        if (isLiked(userId, targetType, targetId)) {
            return;
        }
        CommunityLike like = new CommunityLike();
        like.setId(snowflakeIdGenerator.nextId());
        like.setUserId(userId);
        like.setTargetType(targetType);
        like.setTargetId(targetId);
        communityLikeMapper.insert(like);

        // 1=帖子 2=评论
        if (targetType != null && targetType == 1) {
            CommunityPost post = communityPostMapper.selectById(targetId);
            if (post != null) {
                post.setLikeCount(post.getLikeCount() == null ? 1 : post.getLikeCount() + 1);
                communityPostMapper.updateById(post);
            }
        }
        stringRedisTemplate.opsForValue().set(
                LIKE_KEY + userId + ":" + targetType + ":" + targetId, "1",
                CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    @Transactional
    public void unlike(Long userId, Integer targetType, Long targetId) {
        LambdaQueryWrapper<CommunityLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityLike::getUserId, userId)
                .eq(CommunityLike::getTargetType, targetType)
                .eq(CommunityLike::getTargetId, targetId);
        communityLikeMapper.delete(wrapper);

        if (targetType != null && targetType == 1) {
            CommunityPost post = communityPostMapper.selectById(targetId);
            if (post != null && post.getLikeCount() != null && post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
                communityPostMapper.updateById(post);
            }
        }
        stringRedisTemplate.delete(LIKE_KEY + userId + ":" + targetType + ":" + targetId);
    }

    @Override
    @Transactional
    public void favorite(Long userId, Long postId) {
        if (isFavorite(userId, postId)) {
            return;
        }
        CommunityFavorite favorite = new CommunityFavorite();
        favorite.setId(snowflakeIdGenerator.nextId());
        favorite.setUserId(userId);
        favorite.setPostId(postId);
        favorite.setFolderName("默认收藏");
        communityFavoriteMapper.insert(favorite);

        CommunityPost post = communityPostMapper.selectById(postId);
        if (post != null) {
            post.setCollectCount(post.getCollectCount() == null ? 1 : post.getCollectCount() + 1);
            communityPostMapper.updateById(post);
        }
        stringRedisTemplate.opsForValue().set(
                FAVORITE_KEY + userId + ":" + postId, "1",
                CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    @Transactional
    public void unfavorite(Long userId, Long postId) {
        LambdaQueryWrapper<CommunityFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityFavorite::getUserId, userId)
                .eq(CommunityFavorite::getPostId, postId);
        communityFavoriteMapper.delete(wrapper);

        CommunityPost post = communityPostMapper.selectById(postId);
        if (post != null && post.getCollectCount() != null && post.getCollectCount() > 0) {
            post.setCollectCount(post.getCollectCount() - 1);
            communityPostMapper.updateById(post);
        }
        stringRedisTemplate.delete(FAVORITE_KEY + userId + ":" + postId);
    }

    @Override
    @Transactional
    public void follow(Long userId, Long followUserId) {
        if (userId == null || followUserId == null) {
            throw new BusinessException("参数错误");
        }
        if (userId.equals(followUserId)) {
            throw new BusinessException("不能关注自己");
        }
        if (isFollowed(userId, followUserId)) {
            return;
        }
        CommunityFollow follow = new CommunityFollow();
        follow.setId(snowflakeIdGenerator.nextId());
        follow.setUserId(userId);
        follow.setTargetUserId(followUserId);
        follow.setTargetType(1);
        follow.setTargetId(followUserId);
        follow.setMutual(0);
        communityFollowMapper.insert(follow);

        // 检查对方是否已关注我，互关
        CommunityFollow reverse = communityFollowMapper.selectByUserTarget(followUserId, 1, userId);
        if (reverse != null) {
            follow.setMutual(1);
            communityFollowMapper.updateById(follow);
            reverse.setMutual(1);
            communityFollowMapper.updateById(reverse);
        }
        stringRedisTemplate.opsForValue().set(
                FOLLOW_KEY + userId + ":" + followUserId, "1",
                CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    @Transactional
    public void unfollow(Long userId, Long followUserId) {
        LambdaQueryWrapper<CommunityFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityFollow::getUserId, userId)
                .eq(CommunityFollow::getTargetUserId, followUserId);
        communityFollowMapper.delete(wrapper);

        // 取消互关
        CommunityFollow reverse = communityFollowMapper.selectByUserTarget(followUserId, 1, userId);
        if (reverse != null) {
            reverse.setMutual(0);
            communityFollowMapper.updateById(reverse);
        }
        stringRedisTemplate.delete(FOLLOW_KEY + userId + ":" + followUserId);
    }

    @Override
    public boolean isLiked(Long userId, Integer targetType, Long targetId) {
        String key = LIKE_KEY + userId + ":" + targetType + ":" + targetId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return true;
        }
        LambdaQueryWrapper<CommunityLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityLike::getUserId, userId)
                .eq(CommunityLike::getTargetType, targetType)
                .eq(CommunityLike::getTargetId, targetId);
        return communityLikeMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean isFavorite(Long userId, Long postId) {
        String key = FAVORITE_KEY + userId + ":" + postId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return true;
        }
        LambdaQueryWrapper<CommunityFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityFavorite::getUserId, userId)
                .eq(CommunityFavorite::getPostId, postId);
        return communityFavoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean isFollowed(Long userId, Long followUserId) {
        String key = FOLLOW_KEY + userId + ":" + followUserId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return true;
        }
        LambdaQueryWrapper<CommunityFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityFollow::getUserId, userId)
                .eq(CommunityFollow::getTargetUserId, followUserId);
        return communityFollowMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long getFavoriteCount(Long userId) {
        LambdaQueryWrapper<CommunityFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityFavorite::getUserId, userId);
        return communityFavoriteMapper.selectCount(wrapper);
    }

    @Override
    public Long getFollowerCount(Long userId) {
        return communityFollowMapper.countFollowers(userId);
    }

    @Override
    public Long getFollowingCount(Long userId) {
        return communityFollowMapper.countFollowing(userId);
    }

    private static final String SHARE_KEY = "community:share:";

    @Override
    @Transactional
    public Map<String, Object> share(Long userId, Long postId, String shareType) {
        if (userId == null || postId == null) {
            throw new BusinessException("参数错误");
        }

        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        post.setShareCount(post.getShareCount() == null ? 1 : post.getShareCount() + 1);
        communityPostMapper.updateById(post);

        String shareUrl = buildShareUrl(postId);
        String shortUrl = generateShortUrl(shareUrl);

        Map<String, Object> result = new HashMap<>();
        result.put("shareUrl", shareUrl);
        result.put("shortUrl", shortUrl);
        result.put("shareType", shareType);
        result.put("postId", postId);
        result.put("shareCount", post.getShareCount());

        stringRedisTemplate.opsForValue().set(
                SHARE_KEY + postId, String.valueOf(post.getShareCount()),
                CACHE_TTL_DAYS, TimeUnit.DAYS);

        log.info("用户 {} 分享帖子 {}, 分享类型: {}", userId, postId, shareType);
        return result;
    }

    @Override
    public Long getShareCount(Long postId) {
        String key = SHARE_KEY + postId;
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Long.parseLong(cached);
        }
        CommunityPost post = communityPostMapper.selectById(postId);
        return post != null && post.getShareCount() != null ? post.getShareCount().longValue() : 0L;
    }

    private String buildShareUrl(Long postId) {
        return "https://www.tailoris.com/community/post/" + postId;
    }

    private String generateShortUrl(String longUrl) {
        int hash = longUrl.hashCode();
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder shortUrl = new StringBuilder();
        hash = Math.abs(hash);
        while (hash > 0) {
            shortUrl.append(chars.charAt(hash % 62));
            hash /= 62;
        }
        String result = shortUrl.reverse().toString();
        return "https://t.tailoris.com/" + (result.length() > 6 ? result.substring(0, 6) : result);
    }
}
