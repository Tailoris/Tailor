package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantTrialAssessment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 商家试运营考核Mapper - MER-006.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Mapper
public interface MerchantTrialAssessmentMapper extends BaseMapper<MerchantTrialAssessment> {

    /**
     * 查询待考核商家列表（试运营到期但未考核）.
     */
    @Select("SELECT * FROM merchant_trial_assessment " +
            "WHERE result = 0 " +
            "  AND trial_end_date <= CURDATE() " +
            "  AND deleted = 0 " +
            "ORDER BY trial_end_date ASC")
    List<MerchantTrialAssessment> selectPendingAssessment();

    /**
     * 按结果统计数量.
     */
    @Select("SELECT result, COUNT(*) AS cnt FROM merchant_trial_assessment " +
            "WHERE deleted = 0 " +
            "GROUP BY result")
    List<Map<String, Object>> countByResult();

    /**
     * 按转正状态统计.
     */
    @Select("SELECT COUNT(*) AS cnt FROM merchant_trial_assessment " +
            "WHERE deleted = 0 " +
            "  AND is_promoted = #{isPromoted}")
    Long countByPromoted(@Param("isPromoted") Integer isPromoted);

    /**
     * 查询某商家历史考核记录.
     */
    @Select("SELECT * FROM merchant_trial_assessment " +
            "WHERE merchant_id = #{merchantId} " +
            "  AND deleted = 0 " +
            "ORDER BY trial_start_date DESC")
    List<MerchantTrialAssessment> selectByMerchantId(@Param("merchantId") Long merchantId);
}
