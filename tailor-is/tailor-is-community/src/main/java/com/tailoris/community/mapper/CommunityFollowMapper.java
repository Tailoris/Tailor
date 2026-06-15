package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityFollowMapper extends BaseMapper<CommunityFollow> {

    @Select("SELECT * FROM community_follow WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId} AND deleted = 0")
    CommunityFollow selectByUserTarget(@Param("userId") Long userId,
                                       @Param("targetType") Integer targetType,
                                       @Param("targetId") Long targetId);

    @Select("SELECT * FROM community_follow WHERE user_id = #{userId} AND target_type = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<CommunityFollow> selectFollowingByUser(@Param("userId") Long userId);

    @Select("SELECT * FROM community_follow WHERE target_user_id = #{userId} AND target_type = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<CommunityFollow> selectFollowersOfUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM community_follow WHERE target_user_id = #{userId} AND target_type = 1 AND deleted = 0")
    long countFollowers(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM community_follow WHERE user_id = #{userId} AND target_type = 1 AND deleted = 0")
    long countFollowing(@Param("userId") Long userId);
}
