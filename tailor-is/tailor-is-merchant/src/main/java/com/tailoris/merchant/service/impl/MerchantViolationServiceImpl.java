package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.constant.ViolationConstants;
import com.tailoris.merchant.dto.ViolationAppealRequest;
import com.tailoris.merchant.dto.ViolationPunishRequest;
import com.tailoris.merchant.dto.ViolationReportRequest;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.entity.MerchantViolation;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantMapper;
import com.tailoris.merchant.mapper.MerchantViolationMapper;
import com.tailoris.merchant.service.IMerchantViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家违规处罚服务实现 - MER-007.
 *
 * <h3>处罚流程</h3>
 * <ol>
 *   <li>用户/系统提交违规举报 → status=PENDING(0)</li>
 *   <li>管理员审核后执行处罚 → status=PUNISHED(1)，记录处罚类型与天数</li>
 *   <li>商家申诉 → status=APPEALED(2)</li>
 *   <li>管理员处理申诉 → 通过：撤销处罚；不通过：维持处罚</li>
 *   <li>处罚到期自动解除 → status=RELEASED(4)</li>
 * </ol>
 *
 * <h3>扣分规则</h3>
 * <ul>
 *   <li>轻微(LEVEL_MINOR): 5分</li>
 *   <li>一般(LEVEL_GENERAL): 15分</li>
 *   <li>严重(LEVEL_SERIOUS): 30分</li>
 *   <li>特别严重(LEVEL_VERY_SERIOUS): 60分</li>
 *   <li>扣分到 0 即清退</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantViolationServiceImpl
        extends ServiceImpl<MerchantViolationMapper, MerchantViolation>
        implements IMerchantViolationService {

    private final MerchantMapper merchantMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantViolation report(ViolationReportRequest request) {
        Merchant merchant = merchantMapper.selectById(request.getMerchantId());
        if (merchant == null) {
            throw new MerchantBusinessException("商家不存在: " + request.getMerchantId());
        }

        MerchantViolation violation = new MerchantViolation();
        violation.setMerchantId(request.getMerchantId());
        violation.setShopId(request.getShopId());
        violation.setViolationType(request.getViolationType());
        violation.setTitle(request.getTitle());
        violation.setDescription(request.getDescription());
        violation.setEvidence(request.getEvidence());
        violation.setReporterId(request.getReporterId());
        violation.setStatus(ViolationConstants.STATUS_PENDING);
        violation.setPunishmentType(ViolationConstants.PUNISH_PENDING);
        save(violation);

        log.info("提交违规举报: merchantId={}, type={}, title={}",
                request.getMerchantId(), request.getViolationType(), request.getTitle());
        return violation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantViolation punish(ViolationPunishRequest request) {
        MerchantViolation violation = getById(request.getViolationId());
        if (violation == null) {
            throw new MerchantBusinessException("违规记录不存在");
        }
        if (!(Integer.valueOf(ViolationConstants.STATUS_PENDING).equals(violation.getStatus())
                || Integer.valueOf(ViolationConstants.STATUS_APPEALED).equals(violation.getStatus()))) {
            throw new MerchantBusinessException("该记录状态不可处罚");
        }

        violation.setViolationLevel(request.getViolationLevel());
        violation.setPunishmentType(request.getPunishmentType());
        violation.setPunishmentDays(request.getPunishmentDays());
        violation.setStatus(ViolationConstants.STATUS_PUNISHED);
        violation.setHandleTime(LocalDateTime.now());
        violation.setHandlerId(request.getHandlerId());

        LocalDateTime now = LocalDateTime.now();
        violation.setPunishmentStart(now);
        if (request.getPunishmentDays() != null && request.getPunishmentDays() > 0) {
            violation.setPunishmentEnd(now.plusDays(request.getPunishmentDays()));
        } else {
            violation.setPunishmentEnd(null);  // 永久
        }

        updateById(violation);

        // 同步更新商家处罚状态与扣分
        applyPunishmentToMerchant(violation);

        log.warn("执行违规处罚: merchantId={}, type={}, level={}, days={}",
                violation.getMerchantId(),
                request.getPunishmentType(),
                request.getViolationLevel(),
                request.getPunishmentDays());
        return violation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantViolation appeal(ViolationAppealRequest request) {
        MerchantViolation violation = getById(request.getViolationId());
        if (violation == null) {
            throw new MerchantBusinessException("违规记录不存在");
        }
        if (!violation.getMerchantId().equals(request.getMerchantId())) {
            throw new MerchantBusinessException("无权申诉该违规记录");
        }
        if (!Integer.valueOf(ViolationConstants.STATUS_PUNISHED).equals(violation.getStatus())) {
            throw new MerchantBusinessException("仅已处罚记录可申诉");
        }
        if (Integer.valueOf(ViolationConstants.APPEAL_YES).equals(violation.getIsAppealed())) {
            throw new MerchantBusinessException("已申诉，请勿重复操作");
        }

        violation.setIsAppealed(ViolationConstants.APPEAL_YES);
        violation.setAppealContent(request.getAppealContent());
        violation.setAppealTime(LocalDateTime.now());
        violation.setStatus(ViolationConstants.STATUS_APPEALED);
        updateById(violation);
        return violation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantViolation handleAppeal(Long violationId, boolean approved, String result, Long handlerId) {
        MerchantViolation violation = getById(violationId);
        if (violation == null) {
            throw new MerchantBusinessException("违规记录不存在");
        }
        if (!Integer.valueOf(ViolationConstants.STATUS_APPEALED).equals(violation.getStatus())) {
            throw new MerchantBusinessException("该记录无申诉待处理");
        }

        violation.setHandleTime(LocalDateTime.now());
        violation.setHandlerId(handlerId);
        violation.setAppealResult(result);
        if (approved) {
            // 申诉通过：撤销处罚
            violation.setStatus(ViolationConstants.STATUS_REVOKED);
            violation.setPunishmentType(ViolationConstants.PUNISH_PENDING);
            violation.setPunishmentStart(null);
            violation.setPunishmentEnd(null);
            // 恢复商家扣分
            reversePunishmentToMerchant(violation);
            log.info("申诉通过，撤销处罚: violationId={}", violationId);
        } else {
            // 申诉不通过：维持原处罚
            violation.setStatus(ViolationConstants.STATUS_PUNISHED);
            log.info("申诉不通过，维持处罚: violationId={}", violationId);
        }
        updateById(violation);
        return violation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revoke(Long violationId, String reason, Long handlerId) {
        MerchantViolation violation = getById(violationId);
        if (violation == null) {
            throw new MerchantBusinessException("违规记录不存在");
        }
        violation.setStatus(ViolationConstants.STATUS_REVOKED);
        violation.setHandleTime(LocalDateTime.now());
        violation.setHandlerId(handlerId);
        violation.setAppealResult(reason);
        updateById(violation);
        reversePunishmentToMerchant(violation);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean release(Long violationId) {
        MerchantViolation violation = getById(violationId);
        if (violation == null) {
            return false;
        }
        if (!Integer.valueOf(ViolationConstants.STATUS_PUNISHED).equals(violation.getStatus())) {
            return false;
        }
        violation.setStatus(ViolationConstants.STATUS_RELEASED);
        updateById(violation);

        // 如果当前没有其他生效的处罚，恢复商家状态
        Merchant merchant = merchantMapper.selectById(violation.getMerchantId());
        if (merchant != null) {
            Integer maxType = baseMapper.selectMaxActivePunishmentType(violation.getMerchantId());
            merchant.setPunishmentStatus(maxType == null ? 0 : Math.max(maxType - 1, 0));
            merchant.setPunishmentEnd(null);
            merchantMapper.updateById(merchant);
        }
        return true;
    }

    /**
     * 每小时扫描一次自动解除到期处罚.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 0 * * * ?")
    public int autoReleaseExpired() {
        LambdaQueryWrapper<MerchantViolation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantViolation::getStatus, ViolationConstants.STATUS_PUNISHED)
               .isNotNull(MerchantViolation::getPunishmentEnd)
               .lt(MerchantViolation::getPunishmentEnd, LocalDateTime.now());
        List<MerchantViolation> expired = list(wrapper);
        int count = 0;
        for (MerchantViolation v : expired) {
            if (release(v.getId())) {
                count++;
            }
        }
        if (count > 0) {
            log.info("自动解除到期处罚: 数量={}", count);
        }
        return count;
    }

    @Override
    public List<MerchantViolation> listByMerchant(Long merchantId) {
        LambdaQueryWrapper<MerchantViolation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantViolation::getMerchantId, merchantId)
               .orderByDesc(MerchantViolation::getCreateTime);
        return list(wrapper);
    }

    @Override
    public long countByMerchantAndDateRange(Long merchantId, String startDate, String endDate) {
        String start = (startDate != null ? startDate : "1970-01-01") + " 00:00:00";
        String end = (endDate != null ? endDate : "2999-12-31") + " 23:59:59";
        Long cnt = baseMapper.countByMerchantAndDateRange(merchantId, start, end);
        return cnt == null ? 0L : cnt;
    }

    @Override
    public long countActivePunishment(Long merchantId) {
        Long cnt = baseMapper.countActivePunishment(merchantId);
        return cnt == null ? 0L : cnt;
    }

    @Override
    public Integer getMaxActivePunishmentType(Long merchantId) {
        return baseMapper.selectMaxActivePunishmentType(merchantId);
    }

    @Override
    public boolean isBanned(Long merchantId) {
        Integer maxType = getMaxActivePunishmentType(merchantId);
        return maxType != null && maxType >= ViolationConstants.PUNISH_BAN;
    }

    @Override
    public boolean isLimited(Long merchantId) {
        Integer maxType = getMaxActivePunishmentType(merchantId);
        return maxType != null && maxType >= ViolationConstants.PUNISH_LIMIT;
    }

    @Override
    public Map<String, Object> getViolationStats(Long merchantId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> sum = baseMapper.sumViolationStats(merchantId);
        if (sum == null) {
            sum = new HashMap<>();
        }
        long totalCount = getLong(sum, "total_count");
        long minor = getLong(sum, "minor_count");
        long general = getLong(sum, "general_count");
        long serious = getLong(sum, "serious_count");
        long verySerious = getLong(sum, "very_serious_count");

        long deducted = minor * ViolationConstants.DEDUCT_MINOR
                + general * ViolationConstants.DEDUCT_GENERAL
                + serious * ViolationConstants.DEDUCT_SERIOUS
                + verySerious * ViolationConstants.DEDUCT_VERY_SERIOUS;
        long currentScore = Math.max(0, ViolationConstants.VIOLATION_MAX_SCORE - deducted);

        result.put("totalCount", totalCount);
        result.put("minorCount", minor);
        result.put("generalCount", general);
        result.put("seriousCount", serious);
        result.put("verySeriousCount", verySerious);
        result.put("deductedScore", deducted);
        result.put("currentScore", currentScore);
        result.put("activePunishment", countActivePunishment(merchantId));
        result.put("isBanned", isBanned(merchantId));
        result.put("isLimited", isLimited(merchantId));
        return result;
    }

    // ============================================================
    // 私有方法
    // ============================================================

    /**
     * 将处罚应用到商家（更新扣分、状态）.
     */
    private void applyPunishmentToMerchant(MerchantViolation violation) {
        Merchant merchant = merchantMapper.selectById(violation.getMerchantId());
        if (merchant == null) {
            return;
        }

        // 扣分
        int deduct = getDeductScore(violation.getViolationLevel());
        int currentScore = merchant.getViolationScore() == null
                ? ViolationConstants.VIOLATION_MAX_SCORE : merchant.getViolationScore();
        int newScore = Math.max(0, currentScore - deduct);
        merchant.setViolationScore(newScore);

        // 处罚状态映射
        Integer status = mapPunishmentToStatus(violation.getPunishmentType());
        merchant.setPunishmentStatus(status);
        merchant.setPunishmentEnd(violation.getPunishmentEnd());

        // 封禁或下架时关闭店铺
        if (Integer.valueOf(ViolationConstants.PUNISH_BAN).equals(violation.getPunishmentType())
                || Integer.valueOf(ViolationConstants.PUNISH_EVICT).equals(violation.getPunishmentType())) {
            merchant.setStatus(MerchantConstants.MERCHANT_STATUS_FROZEN);
        } else if (Integer.valueOf(ViolationConstants.PUNISH_OFFLINE).equals(violation.getPunishmentType())) {
            // 仅下架商品，店铺本身保留
        }

        merchantMapper.updateById(merchant);
    }

    /**
     * 撤销处罚，恢复扣分.
     */
    private void reversePunishmentToMerchant(MerchantViolation violation) {
        Merchant merchant = merchantMapper.selectById(violation.getMerchantId());
        if (merchant == null) {
            return;
        }
        int deduct = getDeductScore(violation.getViolationLevel());
        int currentScore = merchant.getViolationScore() == null
                ? ViolationConstants.VIOLATION_MAX_SCORE : merchant.getViolationScore();
        int newScore = Math.min(ViolationConstants.VIOLATION_MAX_SCORE, currentScore + deduct);
        merchant.setViolationScore(newScore);

        // 重新计算最高生效处罚
        Integer maxType = baseMapper.selectMaxActivePunishmentType(violation.getMerchantId());
        if (maxType == null) {
            merchant.setPunishmentStatus(0);
            merchant.setPunishmentEnd(null);
            if (merchant.getStatus() != null && merchant.getStatus() == MerchantConstants.MERCHANT_STATUS_FROZEN) {
                merchant.setStatus(MerchantConstants.MERCHANT_STATUS_NORMAL);
            }
        } else {
            merchant.setPunishmentStatus(mapPunishmentToStatus(maxType));
        }
        merchantMapper.updateById(merchant);
    }

    private int getDeductScore(Integer level) {
        if (level == null) {
            return 0;
        }
        switch (level) {
            case ViolationConstants.LEVEL_MINOR: return ViolationConstants.DEDUCT_MINOR;
            case ViolationConstants.LEVEL_GENERAL: return ViolationConstants.DEDUCT_GENERAL;
            case ViolationConstants.LEVEL_SERIOUS: return ViolationConstants.DEDUCT_SERIOUS;
            case ViolationConstants.LEVEL_VERY_SERIOUS: return ViolationConstants.DEDUCT_VERY_SERIOUS;
            default: return 0;
        }
    }

    private Integer mapPunishmentToStatus(Integer punishmentType) {
        if (punishmentType == null) {
            return 0;
        }
        switch (punishmentType) {
            case ViolationConstants.PUNISH_WARN: return 0;
            case ViolationConstants.PUNISH_LIMIT: return 1;
            case ViolationConstants.PUNISH_OFFLINE: return 2;
            case ViolationConstants.PUNISH_BAN:
            case ViolationConstants.PUNISH_EVICT: return 3;
            default: return 0;
        }
    }

    private long getLong(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return 0L;
        }
    }
}
