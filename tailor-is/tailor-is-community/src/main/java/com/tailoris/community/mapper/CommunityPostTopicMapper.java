package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityPostTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityPostTopicMapper extends BaseMapper<CommunityPostTopic> {

    @Select("SELECT * FROM community_post_topic WHERE post_id = #{postId}")
    List<CommunityPostTopic> selectByPostId(@Param("postId") Long postId);

    @Select("SELECT * FROM community_post_topic WHERE topic_id = #{topicId} ORDER BY create_time DESC LIMIT 100")
    List<CommunityPostTopic> selectByTopicId(@Param("topicId") Long topicId);
}
