package com.tailoris.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tailoris.merchant.entity.MerchantTrialAssessment;

import java.util.List;

/**
 * 商家试运营考核服务接口 - MER-006.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public interface IMerchantTrialAssessmentService extends IService<MerchantTrialAssessment> {

    /**
     * 为新商家创建试运营考核记录.
     */
    MerchantTrialAssessment createTrialRecord(Long merchantId);

    /**
     * 执行考核（按当前数据计算得分）.
     */
    MerchantTrialAssessment performAssessment(Long merchantId);

    /**
     * 商家转正（考核通过）.
     */
    boolean promote(Long merchantId, String remark);

    /**
     * 延期考核.
     */
    boolean extend(Long merchantId, int additionalDays, String remark);

    /**
     * 关闭店铺（考核不通过）.
     */
    boolean failAndClose(Long merchantId, String remark);

    /**
     * 查询待考核列表.
     */
    List<MerchantTrialAssessment> listPendingAssessments();

    /**
     * 查询商家考核历史.
     */
    List<MerchantTrialAssessment> getAssessmentHistory(Long merchantId);
}
