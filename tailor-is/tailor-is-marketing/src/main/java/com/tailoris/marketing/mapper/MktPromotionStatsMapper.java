package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.MktPromotionStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface MktPromotionStatsMapper extends BaseMapper<MktPromotionStats> {

    @Select("SELECT * FROM mkt_promotion_stats WHERE promotion_type = #{type} AND promotion_id = #{id} ORDER BY stat_date DESC LIMIT #{days}")
    List<MktPromotionStats> selectByPromotionRecent(@Param("type") Integer type, @Param("id") Long id, @Param("days") Integer days);

    @Select("SELECT * FROM mkt_promotion_stats WHERE stat_date BETWEEN #{startDate} AND #{endDate} ORDER BY order_amount DESC LIMIT #{limit}")
    List<MktPromotionStats> selectTopByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("limit") Integer limit);

    @Select("SELECT promotion_type, SUM(order_count) AS orderCount, SUM(order_amount) AS orderAmount, SUM(discount_amount) AS discountAmount FROM mkt_promotion_stats WHERE stat_date BETWEEN #{startDate} AND #{endDate} GROUP BY promotion_type")
    List<Map<String, Object>> aggregateByType(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
