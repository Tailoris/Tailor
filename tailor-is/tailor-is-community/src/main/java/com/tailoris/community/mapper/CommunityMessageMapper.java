package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommunityMessageMapper extends BaseMapper<CommunityMessage> {

    @Select("SELECT * FROM community_message WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 200")
    List<CommunityMessage> selectByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM community_message WHERE user_id = #{userId} AND is_read = 0")
    long countUnread(@Param("userId") Long userId);

    @Update("UPDATE community_message SET is_read = 1, read_time = NOW() WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);
}
