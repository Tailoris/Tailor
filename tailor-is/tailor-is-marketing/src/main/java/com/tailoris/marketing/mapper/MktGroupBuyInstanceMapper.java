package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.MktGroupBuyInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MktGroupBuyInstanceMapper extends BaseMapper<MktGroupBuyInstance> {

    @Select("SELECT * FROM mkt_group_buy_instance WHERE status = 0 AND expire_time < NOW()")
    List<MktGroupBuyInstance> selectExpiredInstances();

    @Select("SELECT * FROM mkt_group_buy_instance WHERE status = 0 AND activity_id = #{activityId} AND id != #{excludeId} ORDER BY current_size DESC, create_time ASC LIMIT 10")
    List<MktGroupBuyInstance> selectActiveGroupsForJoin(@Param("activityId") Long activityId, @Param("excludeId") Long excludeId);
}
