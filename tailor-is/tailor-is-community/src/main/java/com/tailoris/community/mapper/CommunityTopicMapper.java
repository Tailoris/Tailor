package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommunityTopicMapper extends BaseMapper<CommunityTopic> {

    @Select("SELECT * FROM community_topic WHERE topic_name = #{name} AND deleted = 0 LIMIT 1")
    CommunityTopic selectByName(@Param("name") String name);

    @Select("SELECT * FROM community_topic WHERE is_hot = 1 AND status = 1 AND deleted = 0 ORDER BY post_count DESC LIMIT #{limit}")
    List<CommunityTopic> selectHotTopics(@Param("limit") int limit);

    @Select("SELECT * FROM community_topic WHERE status = 1 AND deleted = 0 ORDER BY post_count DESC LIMIT #{limit}")
    List<CommunityTopic> selectTopByPostCount(@Param("limit") int limit);

    @Update("UPDATE community_topic SET post_count = post_count + 1 WHERE id = #{id}")
    int incrPostCount(@Param("id") Long id);

    @Update("UPDATE community_topic SET follow_count = follow_count + 1 WHERE id = #{id}")
    int incrFollowCount(@Param("id") Long id);
}
