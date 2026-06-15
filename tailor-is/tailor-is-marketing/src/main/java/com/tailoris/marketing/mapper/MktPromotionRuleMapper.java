package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.MktPromotionRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MktPromotionRuleMapper extends BaseMapper<MktPromotionRule> {

    @Select("SELECT * FROM mkt_promotion_rule WHERE status = 1 AND start_time <= #{now} AND end_time >= #{now} " +
            "AND (shop_id = #{shopId} OR shop_id IS NULL) " +
            "ORDER BY priority DESC, id ASC")
    List<MktPromotionRule> selectActiveRules(@Param("shopId") Long shopId, @Param("now") LocalDateTime now);
}
