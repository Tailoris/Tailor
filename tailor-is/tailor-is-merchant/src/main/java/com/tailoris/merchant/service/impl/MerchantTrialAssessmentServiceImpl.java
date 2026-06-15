package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.constant.TrialAssessmentConstants;
import com.tailoris.merchant.entity.Merchant;
import com.tailoris.merchant.entity.MerchantTrialAssessment;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantMapper;
import com.tailoris.merchant.mapper.MerchantTrialAssessmentMapper;
import com.tailoris.merchant.service.IMerchantTrialAssessmentService;
import com.tailoris.merchant.service.IMerchantViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 商家试运营考核服务实现 - MER-006.
 *
 * <p>实现商家30天试运营期的考核逻辑，支持通过、未通过、延期三种结果。</p>
 *
 * <h3>评分模型（满分100分）</h3>
 * <ul>
 *   <li>订单数（30分）：≥10单得30分，每少1单扣3分，0单得0分</li>
 *   <li>订单金额（30分）：≥1000元得30分，每少100元扣3分，0元得0分</li>
 *   <li>商品数（20分）：≥5个得20分，每少1个扣4分，0个得0分</li>
 *   <li>退款率（10分）：≤20%得10分，每超1%扣0.5分</li>
 *   <li>违规次数（10分）：0次得10分，每次扣3分，3次及以上得0分</li>
 * </ul>
 *
 * <h3>硬性指标（一票否决）</h3>
 * <ul>
 *   <li>违规次数 &gt; 3次 → 未通过</li>
 *   <li>投诉数 &gt; 5次 → 未通过</li>
 *   <li>退款率 &gt; 50% → 未通过</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantTrialAssessmentServiceImpl
        extends ServiceImpl<MerchantTrialAssessmentMapper, MerchantTrialAssessment>
        implements IMerchantTrialAssessmentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MerchantMapper merchantMapper;

    /**
     * 违反服务（延迟加载，避免循环依赖）.
     */
    private final ObjectProvider<IMerchantViolationService> violationServiceProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantTrialAssessment createTrialRecord(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new MerchantBusinessException("商家不存在: " + merchantId);
        }

        // 检查是否已存在
        LambdaQueryWrapper<MerchantTrialAssessment> check = new LambdaQueryWrapper<>();
        check.eq(MerchantTrialAssessment::getMerchantId, merchantId)
             .last("LIMIT 1");
        MerchantTrialAssessment exists = getOne(check);
        if (exists != null) {
            return exists;
        }

        LocalDate today = LocalDate.now();
        MerchantTrialAssessment record = new MerchantTrialAssessment();
        record.setMerchantId(merchantId);
        record.setTrialStartDate(today.format(DATE_FMT));
        record.setTrialEndDate(today.plusDays(TrialAssessmentConstants.TRIAL_DAYS).format(DATE_FMT));
        record.setTotalDays(TrialAssessmentConstants.TRIAL_DAYS);
        record.setActualDays(0);
        record.setOrderCount(0L);
        record.setOrderAmount(BigDecimal.ZERO);
        record.setProductCount(0L);
        record.setRefundRate(BigDecimal.ZERO);
        record.setComplaintCount(0L);
        record.setViolationCount(0L);
        record.setScore(BigDecimal.ZERO);
        record.setResult(TrialAssessmentConstants.RESULT_PENDING);
        record.setIsPromoted(0);
        save(record);

        // 同步到 merchant 表
        merchant.setIsTrial(1);
        merchant.setTrialStartDate(record.getTrialStartDate());
        merchant.setTrialEndDate(record.getTrialEndDate());
        merchant.setIsPromoted(0);
        merchantMapper.updateById(merchant);

        log.info("创建试运营记录: merchantId={}, 试运营期={} ~ {}",
                merchantId, record.getTrialStartDate(), record.getTrialEndDate());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantTrialAssessment performAssessment(Long merchantId) {
        MerchantTrialAssessment record = getActiveRecord(merchantId);
        if (record == null) {
            throw new MerchantBusinessException("未找到试运营记录");
        }

        // 计算实际运营天数
        LocalDate start = LocalDate.parse(record.getTrialStartDate(), DATE_FMT);
        LocalDate end = LocalDate.now();
        long actualDays = ChronoUnit.DAYS.between(start, end);
        if (actualDays > TrialAssessmentConstants.TRIAL_DAYS) {
            actualDays = TrialAssessmentConstants.TRIAL_DAYS;
        }
        record.setActualDays((int) actualDays);
        record.setAssessmentDate(LocalDate.now().format(DATE_FMT));

        // 计算实际数据（实际应从订单/支付/商品服务聚合）
        // 这里提供框架实现，业务数据通过其他模块注入
        calculateActualMetrics(record);

        // 计算得分
        BigDecimal score = calculateScore(record);
        record.setScore(score);

        // 判定结果
        int result = determineResult(record, score);
        record.setResult(result);

        updateById(record);
        log.info("试运营考核完成: merchantId={}, score={}, result={}",
                merchantId, score, result);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean promote(Long merchantId, String remark) {
        MerchantTrialAssessment record = getActiveRecord(merchantId);
        if (record == null) {
            throw new MerchantBusinessException("未找到试运营记录");
        }
        if (!Integer.valueOf(TrialAssessmentConstants.RESULT_PASS).equals(record.getResult())) {
            throw new MerchantBusinessException("考核未通过，不可转正");
        }

        record.setIsPromoted(1);
        record.setPromoteTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        record.setRemark(remark);
        updateById(record);

        // 同步到 merchant 表
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant != null) {
            merchant.setIsTrial(0);
            merchant.setIsPromoted(1);
            merchant.setPromoteTime(java.time.LocalDateTime.now());
            merchantMapper.updateById(merchant);
        }

        log.info("商家已转正: merchantId={}", merchantId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean extend(Long merchantId, int additionalDays, String remark) {
        MerchantTrialAssessment record = getActiveRecord(merchantId);
        if (record == null) {
            throw new MerchantBusinessException("未找到试运营记录");
        }
        LocalDate newEnd = LocalDate.parse(record.getTrialEndDate(), DATE_FMT).plusDays(additionalDays);
        record.setTrialEndDate(newEnd.format(DATE_FMT));
        record.setTotalDays(record.getTotalDays() + additionalDays);
        record.setResult(TrialAssessmentConstants.RESULT_EXTEND);
        record.setRemark(remark);
        updateById(record);

        // 同步到 merchant 表
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant != null) {
            merchant.setTrialEndDate(record.getTrialEndDate());
            merchantMapper.updateById(merchant);
        }

        log.info("试运营延期: merchantId={}, 新结束日期={}", merchantId, record.getTrialEndDate());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean failAndClose(Long merchantId, String remark) {
        MerchantTrialAssessment record = getActiveRecord(merchantId);
        if (record == null) {
            throw new MerchantBusinessException("未找到试运营记录");
        }
        record.setResult(TrialAssessmentConstants.RESULT_FAIL);
        record.setRemark(remark);
        updateById(record);

        // 关闭店铺
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant != null) {
            merchant.setStatus(MerchantConstants.MERCHANT_STATUS_CANCELLED);
            merchant.setIsTrial(0);
            merchantMapper.updateById(merchant);
        }

        log.warn("商家试运营未通过，已关闭店铺: merchantId={}", merchantId);
        return true;
    }

    @Override
    public List<MerchantTrialAssessment> listPendingAssessments() {
        return baseMapper.selectPendingAssessment();
    }

    @Override
    public List<MerchantTrialAssessment> getAssessmentHistory(Long merchantId) {
        return baseMapper.selectByMerchantId(merchantId);
    }

    // ============================================================
    // 私有方法
    // ============================================================

    private MerchantTrialAssessment getActiveRecord(Long merchantId) {
        LambdaQueryWrapper<MerchantTrialAssessment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantTrialAssessment::getMerchantId, merchantId)
               .orderByDesc(MerchantTrialAssessment::getId)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    /**
     * 计算实际指标（应从订单/支付/商品服务聚合）.
     * <p>此处提供数据接入点，实际查询由后续集成时实现</p>
     */
    private void calculateActualMetrics(MerchantTrialAssessment record) {
        Long merchantId = record.getMerchantId();
        // 实际应通过 Feign/RPC 从订单服务、商品服务拉取数据
        // 此处保留字段，避免空指针
        if (record.getOrderCount() == null) {
            record.setOrderCount(0L);
        }
        if (record.getOrderAmount() == null) {
            record.setOrderAmount(BigDecimal.ZERO);
        }
        if (record.getProductCount() == null) {
            record.setProductCount(0L);
        }
        if (record.getRefundRate() == null) {
            record.setRefundRate(BigDecimal.ZERO);
        }
        if (record.getComplaintCount() == null) {
            record.setComplaintCount(0L);
        }
        if (record.getViolationCount() == null) {
            // 违规次数从违规服务统计
            try {
                long cnt = violationServiceProvider.getIfAvailable() != null
                        ? violationServiceProvider.getIfAvailable()
                                .countByMerchantAndDateRange(
                                        merchantId, record.getTrialStartDate(), record.getTrialEndDate())
                        : 0L;
                record.setViolationCount(cnt);
            } catch (Exception e) {
                log.warn("统计违规次数失败: merchantId={}, err={}", merchantId, e.getMessage());
                record.setViolationCount(0L);
            }
        }
    }

    /**
     * 综合评分（0-100）.
     */
    private BigDecimal calculateScore(MerchantTrialAssessment r) {
        BigDecimal total = BigDecimal.ZERO;

        // 订单数（30分）
        long orderCount = r.getOrderCount() == null ? 0L : r.getOrderCount();
        BigDecimal orderScore;
        if (orderCount >= TrialAssessmentConstants.MIN_ORDER_COUNT_PASS) {
            orderScore = new BigDecimal("30");
        } else {
            long lack = TrialAssessmentConstants.MIN_ORDER_COUNT_PASS - orderCount;
            orderScore = new BigDecimal("30").subtract(new BigDecimal(lack * 3));
        }
        orderScore = orderScore.max(BigDecimal.ZERO);
        total = total.add(orderScore);

        // 订单金额（30分）
        BigDecimal orderAmount = r.getOrderAmount() == null ? BigDecimal.ZERO : r.getOrderAmount();
        BigDecimal amountScore;
        if (orderAmount.compareTo(TrialAssessmentConstants.MIN_ORDER_AMOUNT_PASS) >= 0) {
            amountScore = new BigDecimal("30");
        } else {
            // 每少100元扣3分
            BigDecimal lack = TrialAssessmentConstants.MIN_ORDER_AMOUNT_PASS.subtract(orderAmount);
            BigDecimal units = lack.divide(new BigDecimal("100"), 0, RoundingMode.CEILING);
            amountScore = new BigDecimal("30").subtract(units.multiply(new BigDecimal("3")));
        }
        amountScore = amountScore.max(BigDecimal.ZERO);
        total = total.add(amountScore);

        // 商品数（20分）
        long productCount = r.getProductCount() == null ? 0L : r.getProductCount();
        BigDecimal productScore;
        if (productCount >= TrialAssessmentConstants.MIN_PRODUCT_COUNT_PASS) {
            productScore = new BigDecimal("20");
        } else {
            long lack = TrialAssessmentConstants.MIN_PRODUCT_COUNT_PASS - productCount;
            productScore = new BigDecimal("20").subtract(new BigDecimal(lack * 4));
        }
        productScore = productScore.max(BigDecimal.ZERO);
        total = total.add(productScore);

        // 退款率（10分）
        BigDecimal refundRate = r.getRefundRate() == null ? BigDecimal.ZERO : r.getRefundRate();
        BigDecimal refundScore;
        if (refundRate.compareTo(TrialAssessmentConstants.MAX_REFUND_RATE) <= 0) {
            refundScore = new BigDecimal("10");
        } else {
            BigDecimal excess = refundRate.subtract(TrialAssessmentConstants.MAX_REFUND_RATE);
            // 每超1%扣0.5分
            BigDecimal units = excess.multiply(new BigDecimal("100"))
                    .divide(new BigDecimal("1"), 0, RoundingMode.CEILING);
            refundScore = new BigDecimal("10").subtract(units.multiply(new BigDecimal("0.5")));
        }
        refundScore = refundScore.max(BigDecimal.ZERO);
        total = total.add(refundScore);

        // 违规次数（10分）
        long violationCount = r.getViolationCount() == null ? 0L : r.getViolationCount();
        BigDecimal violationScore = new BigDecimal("10")
                .subtract(new BigDecimal(violationCount * 3));
        violationScore = violationScore.max(BigDecimal.ZERO);
        total = total.add(violationScore);

        return total.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 判定结果.
     */
    private int determineResult(MerchantTrialAssessment record, BigDecimal score) {
        // 硬性指标一票否决
        if (record.getViolationCount() != null
                && record.getViolationCount() > TrialAssessmentConstants.MAX_VIOLATION_COUNT) {
            return TrialAssessmentConstants.RESULT_FAIL;
        }
        if (record.getComplaintCount() != null
                && record.getComplaintCount() > TrialAssessmentConstants.MAX_COMPLAINT_COUNT) {
            return TrialAssessmentConstants.RESULT_FAIL;
        }
        BigDecimal refundRate = record.getRefundRate() == null ? BigDecimal.ZERO : record.getRefundRate();
        if (refundRate.compareTo(new BigDecimal("0.50")) > 0) {
            return TrialAssessmentConstants.RESULT_FAIL;
        }

        // 按分数判定
        if (score.compareTo(TrialAssessmentConstants.PASS_SCORE) >= 0) {
            return TrialAssessmentConstants.RESULT_PASS;
        }
        if (score.compareTo(TrialAssessmentConstants.FAIL_SCORE) >= 0) {
            return TrialAssessmentConstants.RESULT_EXTEND;
        }
        return TrialAssessmentConstants.RESULT_FAIL;
    }
}
