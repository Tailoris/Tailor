package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.MktPromotionStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MktPromotionStepMapper extends BaseMapper<MktPromotionStep> {

    @Select("SELECT * FROM mkt_promotion_step WHERE promotion_id = #{promotionId} ORDER BY threshold_value ASC")
    List<MktPromotionStep> selectByPromotionId(@Param("promotionId") Long promotionId);
}
