package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantViolation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商家违规处罚Mapper - MER-007.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Mapper
public interface MerchantViolationMapper extends BaseMapper<MerchantViolation> {

    /**
     * 统计商家在指定时间区间内的违规次数.
     */
    @Select("SELECT COUNT(*) AS cnt FROM merchant_violation " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND create_time BETWEEN #{startTime} AND #{endTime} " +
            "  AND status IN (1, 2) " +  // 已处罚或申诉中
            "  AND deleted = 0")
    Long countByMerchantAndDateRange(
            @Param("merchantId") Long merchantId,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime);

    /**
     * 统计商家当前正在被处罚的记录数.
     */
    @Select("SELECT COUNT(*) AS cnt FROM merchant_violation " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND status = 1 " +
            "  AND punishment_end IS NULL " +
            "  AND deleted = 0")
    Long countActivePunishment(@Param("merchantId") Long merchantId);

    /**
     * 统计商家当前处罚级别.
     */
    @Select("SELECT MAX(punishment_type) AS max_type FROM merchant_violation " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND status = 1 " +
            "  AND (punishment_end IS NULL OR punishment_end > NOW()) " +
            "  AND deleted = 0")
    Integer selectMaxActivePunishmentType(@Param("merchantId") Long merchantId);

    /**
     * 按违规类型统计.
     */
    @Select("SELECT violation_type, COUNT(*) AS cnt FROM merchant_violation " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND deleted = 0 " +
            "GROUP BY violation_type")
    List<Map<String, Object>> countByType(@Param("merchantId") Long merchantId);

    /**
     * 统计商家累计扣分.
     */
    @Select("SELECT COUNT(*) AS total_count, " +
            "  SUM(CASE WHEN violation_level=1 THEN 1 ELSE 0 END) AS minor_count, " +
            "  SUM(CASE WHEN violation_level=2 THEN 1 ELSE 0 END) AS general_count, " +
            "  SUM(CASE WHEN violation_level=3 THEN 1 ELSE 0 END) AS serious_count, " +
            "  SUM(CASE WHEN violation_level=4 THEN 1 ELSE 0 END) AS very_serious_count " +
            "FROM merchant_violation " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND status IN (1, 2) " +
            "  AND deleted = 0")
    Map<String, Object> sumViolationStats(@Param("merchantId") Long merchantId);
}
