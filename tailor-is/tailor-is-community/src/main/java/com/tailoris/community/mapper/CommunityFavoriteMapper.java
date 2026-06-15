package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityFavoriteMapper extends BaseMapper<CommunityFavorite> {

    @Select("SELECT * FROM community_favorite WHERE user_id = #{userId} AND post_id = #{postId} AND deleted = 0")
    CommunityFavorite selectByUserPost(@Param("userId") Long userId, @Param("postId") Long postId);

    @Select("SELECT * FROM community_favorite WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC")
    List<CommunityFavorite> selectByUser(@Param("userId") Long userId);
}
