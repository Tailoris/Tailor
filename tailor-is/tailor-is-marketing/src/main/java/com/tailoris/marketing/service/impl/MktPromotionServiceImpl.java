package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.marketing.entity.MktPromotionRule;
import com.tailoris.marketing.entity.MktPromotionStep;
import com.tailoris.marketing.mapper.MktPromotionRuleMapper;
import com.tailoris.marketing.mapper.MktPromotionStepMapper;
import com.tailoris.marketing.service.MktPromotionService;
import com.tailoris.marketing.service.MktPromotionService.PromotionDiscountResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 阶梯满减满赠 Service 实现
 * 任务编号: MKT-004
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MktPromotionServiceImpl implements MktPromotionService {

    private final MktPromotionRuleMapper ruleMapper;
    private final MktPromotionStepMapper stepMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_KEY_RULES = "mkt:promotion:rules:";
    private static final long CACHE_TTL_MINUTES = 5;

    @Override
    @Transactional
    public MktPromotionRule createPromotion(MktPromotionRule rule, List<MktPromotionStep> steps) {
        validateRule(rule);
        if (steps == null || steps.isEmpty()) {
            throw new BusinessException("阶梯规则不能为空");
        }
        rule.setId(SnowflakeIdGenerator.getInstance().nextId());
        rule.setStatus(0);
        ruleMapper.insert(rule);

        for (MktPromotionStep step : steps) {
            step.setId(SnowflakeIdGenerator.getInstance().nextId());
            step.setPromotionId(rule.getId());
            stepMapper.insert(step);
        }
        invalidateCache(rule.getShopId());
        log.info("创建促销活动: id={}, name={}, stepCount={}",
                rule.getId(), rule.getPromotionName(), steps.size());
        return rule;
    }

    @Override
    @Transactional
    public MktPromotionRule updatePromotion(MktPromotionRule rule, List<MktPromotionStep> steps) {
        MktPromotionRule existing = ruleMapper.selectById(rule.getId());
        if (existing == null) {
            throw new BusinessException("促销不存在");
        }
        if (existing.getStatus() != null && existing.getStatus() == 1) {
            throw new BusinessException("进行中的促销不能修改");
        }
        ruleMapper.updateById(rule);

        if (steps != null) {
            // 删除原阶梯
            stepMapper.delete(new LambdaQueryWrapper<MktPromotionStep>()
                    .eq(MktPromotionStep::getPromotionId, rule.getId()));
            // 插入新阶梯
            for (MktPromotionStep step : steps) {
                step.setId(SnowflakeIdGenerator.getInstance().nextId());
                step.setPromotionId(rule.getId());
                stepMapper.insert(step);
            }
        }
        invalidateCache(rule.getShopId());
        return rule;
    }

    @Override
    @Transactional
    public void cancelPromotion(Long promotionId) {
        MktPromotionRule rule = ruleMapper.selectById(promotionId);
        if (rule == null) {
            throw new BusinessException("促销不存在");
        }
        rule.setStatus(3);
        ruleMapper.updateById(rule);
        invalidateCache(rule.getShopId());
        log.info("取消促销活动: id={}", promotionId);
    }

    @Override
    public MktPromotionRule getPromotionDetail(Long promotionId) {
        MktPromotionRule rule = ruleMapper.selectById(promotionId);
        if (rule != null) {
            rule.setPromotionName(rule.getPromotionName());
        }
        return rule;
    }

    @Override
    public PageResponse<MktPromotionRule> listPromotions(PageRequest pageRequest, Long shopId, Integer status) {
        Page<MktPromotionRule> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<MktPromotionRule> wrapper = new LambdaQueryWrapper<>();
        if (shopId != null) {
            wrapper.eq(MktPromotionRule::getShopId, shopId);
        }
        if (status != null) {
            wrapper.eq(MktPromotionRule::getStatus, status);
        }
        wrapper.orderByDesc(MktPromotionRule::getPriority);
        wrapper.orderByDesc(MktPromotionRule::getCreateTime);
        Page<MktPromotionRule> result = ruleMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(),
                pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MktPromotionRule> listActiveRules(Long shopId) {
        String cacheKey = CACHE_KEY_RULES + (shopId == null ? "platform" : shopId);
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                // 简化处理：缓存命中则返回从 DB 重新查
            }
        } catch (Exception e) {
            log.warn("读取促销缓存失败: {}", e.getMessage());
        }
        List<MktPromotionRule> rules = ruleMapper.selectActiveRules(shopId, LocalDateTime.now());
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, "1", CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
        return rules;
    }

    @Override
    public PromotionDiscountResult calculateOptimalDiscount(Long shopId, BigDecimal totalAmount, int itemCount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        List<MktPromotionRule> rules = listActiveRules(shopId);
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        PromotionDiscountResult best = null;
        for (MktPromotionRule rule : rules) {
            PromotionDiscountResult result = matchBestStep(rule, totalAmount, itemCount);
            if (result == null) {
                continue;
            }
            if (best == null
                    || (result.getDiscountAmount() != null
                        && best.getDiscountAmount() != null
                        && result.getDiscountAmount().compareTo(best.getDiscountAmount()) > 0)) {
                best = result;
            }
        }
        return best;
    }

    private PromotionDiscountResult matchBestStep(MktPromotionRule rule, BigDecimal totalAmount, int itemCount) {
        List<MktPromotionStep> steps = stepMapper.selectByPromotionId(rule.getId());
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        // 按 threshold 升序
        steps.sort(Comparator.comparing(MktPromotionStep::getThresholdValue));

        MktPromotionStep matched = null;
        for (MktPromotionStep step : steps) {
            if (rule.getThresholdType() != null && rule.getThresholdType() == 2) {
                // 件数门槛
                if (itemCount >= step.getThresholdValue().intValue()) {
                    matched = step;
                }
            } else {
                // 金额门槛（默认）
                if (totalAmount.compareTo(step.getThresholdValue()) >= 0) {
                    matched = step;
                }
            }
        }
        if (matched == null) {
            return null;
        }

        PromotionDiscountResult result = new PromotionDiscountResult();
        result.setPromotionId(rule.getId());
        result.setPromotionName(rule.getPromotionName());
        result.setPromotionType(rule.getPromotionType());
        result.setGiftProductId(matched.getGiftProductId());
        result.setGiftQuantity(matched.getGiftQuantity());

        BigDecimal discount = BigDecimal.ZERO;
        if (matched.getDiscountType() == 1) {
            // 减金额
            discount = matched.getDiscountValue();
        } else if (matched.getDiscountType() == 2) {
            // 打折
            BigDecimal rate = matched.getDiscountValue().divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
            discount = totalAmount.multiply(BigDecimal.ONE.subtract(rate));
            if (matched.getMaxDiscount() != null && discount.compareTo(matched.getMaxDiscount()) > 0) {
                discount = matched.getMaxDiscount();
            }
        }
        result.setDiscountAmount(discount);
        result.setDescription(String.format("活动【%s】优惠%s元", rule.getPromotionName(), discount.toPlainString()));
        return result;
    }

    private void invalidateCache(Long shopId) {
        try {
            stringRedisTemplate.delete(CACHE_KEY_RULES + (shopId == null ? "platform" : shopId));
        } catch (Exception ignored) {
        }
    }

    private void validateRule(MktPromotionRule rule) {
        if (rule.getPromotionName() == null || rule.getPromotionName().isEmpty()) {
            throw new BusinessException("活动名称不能为空");
        }
        if (rule.getStartTime() == null || rule.getEndTime() == null) {
            throw new BusinessException("活动时间不能为空");
        }
        if (rule.getEndTime().isBefore(rule.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
    }
}
