package com.tailoris.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.community.entity.CommunityBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommunityBlockMapper extends BaseMapper<CommunityBlock> {

    @Select("SELECT * FROM community_block WHERE user_id = #{userId}")
    List<CommunityBlock> selectBlockListByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM community_block WHERE user_id = #{userId} AND blocked_user_id = #{blockedId}")
    long existsBlock(@Param("userId") Long userId, @Param("blockedId") Long blockedId);
}
