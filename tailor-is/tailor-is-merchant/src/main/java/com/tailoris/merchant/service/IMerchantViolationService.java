package com.tailoris.merchant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tailoris.merchant.dto.ViolationAppealRequest;
import com.tailoris.merchant.dto.ViolationPunishRequest;
import com.tailoris.merchant.dto.ViolationReportRequest;
import com.tailoris.merchant.entity.MerchantViolation;

import java.util.List;
import java.util.Map;

/**
 * 商家违规处罚服务接口 - MER-007.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public interface IMerchantViolationService extends IService<MerchantViolation> {

    /**
     * 提交违规举报.
     */
    MerchantViolation report(ViolationReportRequest request);

    /**
     * 管理员处理处罚.
     */
    MerchantViolation punish(ViolationPunishRequest request);

    /**
     * 商家申诉.
     */
    MerchantViolation appeal(ViolationAppealRequest request);

    /**
     * 处理申诉.
     */
    MerchantViolation handleAppeal(Long violationId, boolean approved, String result, Long handlerId);

    /**
     * 撤销违规.
     */
    boolean revoke(Long violationId, String reason, Long handlerId);

    /**
     * 解除处罚（到期自动或人工）.
     */
    boolean release(Long violationId);

    /**
     * 检查并自动解除到期处罚.
     */
    int autoReleaseExpired();

    /**
     * 查询商家违规记录.
     */
    List<MerchantViolation> listByMerchant(Long merchantId);

    /**
     * 统计商家违规次数（按日期范围）.
     */
    long countByMerchantAndDateRange(Long merchantId, String startDate, String endDate);

    /**
     * 统计商家当前正在被处罚的记录数.
     */
    long countActivePunishment(Long merchantId);

    /**
     * 获取商家当前最高处罚级别.
     */
    Integer getMaxActivePunishmentType(Long merchantId);

    /**
     * 商家是否被封禁.
     */
    boolean isBanned(Long merchantId);

    /**
     * 商家是否被限流.
     */
    boolean isLimited(Long merchantId);

    /**
     * 获取商家违规扣分明细.
     */
    Map<String, Object> getViolationStats(Long merchantId);
}
