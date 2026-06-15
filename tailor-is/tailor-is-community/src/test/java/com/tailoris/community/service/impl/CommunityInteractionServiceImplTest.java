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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CommunityInteractionServiceImpl 单元测试")
@ExtendWith(MockitoExtension.class)
class CommunityInteractionServiceImplTest {

    @Mock
    private CommunityLikeMapper communityLikeMapper;
    @Mock
    private CommunityFavoriteMapper communityFavoriteMapper;
    @Mock
    private CommunityFollowMapper communityFollowMapper;
    @Mock
    private CommunityPostMapper communityPostMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private SpringSnowflakeIdGenerator snowflakeIdGenerator;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CommunityInteractionServiceImpl interactionService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(snowflakeIdGenerator.nextId()).thenReturn(100001L);
    }

    // ==================== like ====================

    @Test
    @DisplayName("点赞帖子 - 首次点赞成功")
    void testLike_Post_FirstTime() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doReturn(1).when(communityLikeMapper).insert(any(CommunityLike.class));
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setLikeCount(5);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById((CommunityPost) any());

        interactionService.like(1L, 1, 10L);

        verify(communityLikeMapper).insert(any(CommunityLike.class));
        verify(communityPostMapper).updateById(Mockito.<CommunityPost>any());
        verify(valueOperations).set(eq("community:like:1:1:10"), eq("1"), anyLong(), any());
    }

    @Test
    @DisplayName("点赞帖子 - 已点赞则跳过")
    void testLike_Post_AlreadyLiked() {
        when(valueOperations.get("community:like:1:1:10")).thenReturn("1");

        interactionService.like(1L, 1, 10L);

        verify(communityLikeMapper, never()).insert(any(CommunityLike.class));
    }

    @Test
    @DisplayName("点赞参数为空抛出异常")
    void testLike_NullParams() {
        assertThrows(BusinessException.class, () -> interactionService.like(null, 1, 10L));
        assertThrows(BusinessException.class, () -> interactionService.like(1L, null, 10L));
        assertThrows(BusinessException.class, () -> interactionService.like(1L, 1, null));
    }

    // ==================== unlike ====================

    @Test
    @DisplayName("取消点赞 - 帖子点赞数减1")
    void testUnlike_Post_DecrementCount() {
        when(communityLikeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setLikeCount(5);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById((CommunityPost) any());
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        interactionService.unlike(1L, 1, 10L);

        verify(communityPostMapper, times(1)).updateById(any(CommunityPost.class));
    }

    // ==================== favorite ====================

    @Test
    @DisplayName("收藏帖子 - 首次收藏成功")
    void testFavorite_FirstTime() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doReturn(1).when(communityFavoriteMapper).insert(any(CommunityFavorite.class));
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setCollectCount(3);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById((CommunityPost) any());

        interactionService.favorite(1L, 10L);

        verify(communityFavoriteMapper).insert(any(CommunityFavorite.class));
        verify(communityPostMapper).updateById(Mockito.<CommunityPost>any());
    }

    @Test
    @DisplayName("收藏帖子 - 已收藏则跳过")
    void testFavorite_AlreadyFavorited() {
        when(valueOperations.get("community:fav:1:10")).thenReturn("1");

        interactionService.favorite(1L, 10L);

        verify(communityFavoriteMapper, never()).insert(any(CommunityFavorite.class));
    }

    // ==================== unfavorite ====================

    @Test
    @DisplayName("取消收藏 - 收藏数减1")
    void testUnfavorite_DecrementCount() {
        when(communityFavoriteMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setCollectCount(3);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById((CommunityPost) any());
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        interactionService.unfavorite(1L, 10L);

        verify(communityPostMapper).updateById(Mockito.<CommunityPost>any());
    }

    // ==================== follow ====================

    @Test
    @DisplayName("关注用户 - 首次关注成功")
    void testFollow_FirstTime() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityFollowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(communityFollowMapper.insert(any(CommunityFollow.class))).thenReturn(1);
        when(communityFollowMapper.selectByUserTarget(2L, 1, 1L)).thenReturn(null);

        interactionService.follow(1L, 2L);

        verify(communityFollowMapper).insert(any(CommunityFollow.class));
    }

    @Test
    @DisplayName("关注自己抛出异常")
    void testFollow_Self() {
        assertThrows(BusinessException.class, () -> interactionService.follow(1L, 1L));
    }

    @Test
    @DisplayName("关注参数为空抛出异常")
    void testFollow_NullParams() {
        assertThrows(BusinessException.class, () -> interactionService.follow(null, 2L));
        assertThrows(BusinessException.class, () -> interactionService.follow(1L, null));
    }

    @Test
    @DisplayName("互相关注 - 设置互关标记")
    void testFollow_MutualFollow() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityFollowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doReturn(1).when(communityFollowMapper).insert(any(CommunityFollow.class));
        CommunityFollow reverseFollow = new CommunityFollow();
        reverseFollow.setId(200L);
        reverseFollow.setMutual(0);
        when(communityFollowMapper.selectByUserTarget(2L, 1, 1L)).thenReturn(reverseFollow);
        doReturn(1).when(communityFollowMapper).updateById((CommunityFollow) any());

        interactionService.follow(1L, 2L);

        verify(communityFollowMapper, times(2)).updateById((CommunityFollow) any());
    }

    // ==================== unfollow ====================

    @Test
    @DisplayName("取消关注 - 删除关注记录")
    void testUnfollow() {
        when(communityFollowMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(communityFollowMapper.selectByUserTarget(2L, 1, 1L)).thenReturn(null);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        interactionService.unfollow(1L, 2L);

        verify(communityFollowMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("取消关注 - 同时取消互关标记")
    void testUnfollow_RemoveMutual() {
        when(communityFollowMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        CommunityFollow reverseFollow = new CommunityFollow();
        reverseFollow.setId(200L);
        reverseFollow.setMutual(1);
        when(communityFollowMapper.selectByUserTarget(2L, 1, 1L)).thenReturn(reverseFollow);
        doReturn(1).when(communityFollowMapper).updateById((CommunityFollow) any());
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        interactionService.unfollow(1L, 2L);

        verify(communityFollowMapper, times(1)).updateById(any(CommunityFollow.class));
    }

    // ==================== isLiked ====================

    @Test
    @DisplayName("isLiked - 缓存命中返回true")
    void testIsLiked_CacheHit() {
        when(valueOperations.get("community:like:1:1:10")).thenReturn("1");
        assertTrue(interactionService.isLiked(1L, 1, 10L));
        verify(communityLikeMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("isLiked - 缓存未命中查数据库")
    void testIsLiked_CacheMiss_DbHit() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertTrue(interactionService.isLiked(1L, 1, 10L));
    }

    @Test
    @DisplayName("isLiked - 缓存和数据库均无")
    void testIsLiked_NotLiked() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityLikeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertFalse(interactionService.isLiked(1L, 1, 10L));
    }

    // ==================== isFavorite ====================

    @Test
    @DisplayName("isFavorite - 缓存命中返回true")
    void testIsFavorite_CacheHit() {
        when(valueOperations.get("community:fav:1:10")).thenReturn("1");
        assertTrue(interactionService.isFavorite(1L, 10L));
    }

    @Test
    @DisplayName("isFavorite - 数据库查询")
    void testIsFavorite_DbCheck() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertFalse(interactionService.isFavorite(1L, 10L));
    }

    // ==================== isFollowed ====================

    @Test
    @DisplayName("isFollowed - 缓存命中返回true")
    void testIsFollowed_CacheHit() {
        when(valueOperations.get("community:follow:1:2")).thenReturn("1");
        assertTrue(interactionService.isFollowed(1L, 2L));
    }

    @Test
    @DisplayName("isFollowed - 数据库查询")
    void testIsFollowed_DbCheck() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityFollowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertTrue(interactionService.isFollowed(1L, 2L));
    }

    // ==================== getFavoriteCount / getFollowerCount / getFollowingCount ====================

    @Test
    @DisplayName("获取收藏数量")
    void testGetFavoriteCount() {
        when(communityFavoriteMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        assertEquals(5L, interactionService.getFavoriteCount(1L));
    }

    @Test
    @DisplayName("获取粉丝数量")
    void testGetFollowerCount() {
        when(communityFollowMapper.countFollowers(1L)).thenReturn(10L);
        assertEquals(10L, interactionService.getFollowerCount(1L));
    }

    @Test
    @DisplayName("获取关注数量")
    void testGetFollowingCount() {
        when(communityFollowMapper.countFollowing(1L)).thenReturn(8L);
        assertEquals(8L, interactionService.getFollowingCount(1L));
    }

    // ==================== share ====================

    @Test
    @DisplayName("分享帖子 - 成功返回分享信息")
    void testShare_Success() {
        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setShareCount(5);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        doReturn(1).when(communityPostMapper).updateById((CommunityPost) any());

        Map<String, Object> result = interactionService.share(1L, 10L, "wechat");

        assertNotNull(result);
        assertEquals(10L, result.get("postId"));
        assertEquals(6, result.get("shareCount"));
        assertEquals("wechat", result.get("shareType"));
        assertNotNull(result.get("shareUrl"));
        assertNotNull(result.get("shortUrl"));
    }

    @Test
    @DisplayName("分享帖子 - 帖子不存在抛异常")
    void testShare_PostNotFound() {
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> interactionService.share(1L, 99L, "wechat"));
    }

    @Test
    @DisplayName("分享帖子 - 参数为空抛异常")
    void testShare_NullParams() {
        assertThrows(BusinessException.class, () -> interactionService.share(null, 10L, "wechat"));
        assertThrows(BusinessException.class, () -> interactionService.share(1L, null, "wechat"));
    }

    // ==================== getShareCount ====================

    @Test
    @DisplayName("获取分享次数 - 缓存命中")
    void testGetShareCount_CacheHit() {
        when(valueOperations.get("community:share:10")).thenReturn("25");
        assertEquals(25L, interactionService.getShareCount(10L));
    }

    @Test
    @DisplayName("获取分享次数 - 缓存未命中查数据库")
    void testGetShareCount_DbFallback() {
        when(valueOperations.get(anyString())).thenReturn(null);
        CommunityPost post = new CommunityPost();
        post.setShareCount(12);
        when(communityPostMapper.selectById(10L)).thenReturn(post);
        assertEquals(12L, interactionService.getShareCount(10L));
    }

    @Test
    @DisplayName("获取分享次数 - 帖子不存在返回0")
    void testGetShareCount_PostNotFound() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(communityPostMapper.selectById(99L)).thenReturn(null);
        assertEquals(0L, interactionService.getShareCount(99L));
    }
}
