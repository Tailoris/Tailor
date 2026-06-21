package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {

    /**
     * BE-M-26: 参数化方式累加浏览数，避免 SQL 字符串拼接
     */
    @Update("UPDATE community_post SET view_count = view_count + #{count} WHERE id = #{postId}")
    int incrementViewCount(@Param("postId") Long postId, @Param("count") Long count);
}
