package com.tailoris.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.marketing.entity.CouponTemplate;
import com.tailoris.marketing.entity.UserCoupon;
import com.tailoris.marketing.mapper.CouponTemplateMapper;
import com.tailoris.marketing.mapper.UserCouponMapper;
import com.tailoris.marketing.service.MktOrderMatchService;
import com.tailoris.marketing.service.MktOrderMatchService.OrderDiscountPlan;
import com.tailoris.marketing.service.MktOrderMatchService.OrderItemInput;
import com.tailoris.marketing.service.MktPromotionService;
import com.tailoris.marketing.service.MktPromotionService.PromotionDiscountResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 营销与订单联动 Service 实现
 * 任务编号: MKT-007
 *
 * <p>采用贪心算法：先算满减最优，再选最大优惠券，最后选积分抵扣</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MktOrderMatchServiceImpl implements MktOrderMatchService {

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final MktPromotionService promotionService;

    /** 积分抵扣比例：100积分=1元 */
    private static final BigDecimal POINTS_RATE = new BigDecimal("0.01");

    @Override
    public OrderDiscountPlan calculateOptimal(Long userId, List<OrderItemInput> items, BigDecimal totalAmount) {
        OrderDiscountPlan plan = new OrderDiscountPlan();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            plan.setTotalDiscount(BigDecimal.ZERO);
            return plan;
        }

        BigDecimal remaining = totalAmount;
        List<String> activities = new ArrayList<>();

        // 1. 秒杀/拼团活动价优先（直接计算差额）
        BigDecimal activityDiscount = BigDecimal.ZERO;
        if (items != null) {
            for (OrderItemInput item : items) {
                if (item.getActivePromotionType() != null
                        && item.getActivePromotionType() > 0
                        && item.getPrice() != null
                        && item.getQuantity() != null) {
                    // 假设 price 已是活动价，差额体现在 promotionDiscount 里
                    // 简化：直接标记活动
                    activities.add("活动" + item.getActivePromotionType() + "#" + item.getActivePromotionId());
                }
            }
        }

        // 2. 满减满赠（按店铺聚合计算）
        if (items != null && !items.isEmpty()) {
            Map<Long, List<OrderItemInput>> byShop = items.stream()
                    .filter(i -> i.getShopId() != null)
                    .collect(Collectors.groupingBy(OrderItemInput::getShopId));
            for (Map.Entry<Long, List<OrderItemInput>> entry : byShop.entrySet()) {
                BigDecimal shopTotal = entry.getValue().stream()
                        .map(OrderItemInput::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int itemCount = entry.getValue().stream()
                        .mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity())
                        .sum();
                PromotionDiscountResult promotion = promotionService.calculateOptimalDiscount(
                        entry.getKey(), shopTotal, itemCount);
                if (promotion != null && promotion.getDiscountAmount() != null
                        && promotion.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    plan.setPromotionId(promotion.getPromotionId());
                    plan.setPromotionDiscount(promotion.getDiscountAmount());
                    remaining = remaining.subtract(promotion.getDiscountAmount());
                    activities.add("满减#" + promotion.getPromotionName());
                    break; // 单店铺单活动
                }
            }
        }

        // 3. 优惠券（选最优）
        if (userId != null) {
            UserCoupon bestCoupon = selectBestCoupon(userId, remaining);
            if (bestCoupon != null) {
                CouponTemplate template = couponTemplateMapper.selectById(bestCoupon.getCouponId());
                if (template != null) {
                    BigDecimal couponDiscount = calcCouponDiscount(template, remaining);
                    if (couponDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        plan.setUserCouponId(bestCoupon.getId());
                        plan.setCouponTemplateId(template.getId());
                        plan.setCouponDiscount(couponDiscount);
                        activities.add("优惠券#" + template.getName());
                    }
                }
            }
        }

        // 4. 积分（可选，暂留 0）
        plan.setPointsUsed(0);
        plan.setPointsDiscount(BigDecimal.ZERO);

        // 总优惠
        BigDecimal total = BigDecimal.ZERO;
        if (plan.getCouponDiscount() != null) total = total.add(plan.getCouponDiscount());
        if (plan.getPromotionDiscount() != null) total = total.add(plan.getPromotionDiscount());
        if (plan.getPointsDiscount() != null) total = total.add(plan.getPointsDiscount());

        // 优惠封顶：不超过订单金额
        if (total.compareTo(totalAmount) > 0) {
            total = totalAmount;
        }
        plan.setTotalDiscount(total);
        plan.setAppliedActivities(activities);
        plan.setDescription(String.format("总优惠%s元", total.toPlainString()));
        return plan;
    }

    @Override
    public OrderDiscountPlan calculateCouponOnly(Long userId, BigDecimal orderAmount) {
        OrderDiscountPlan plan = new OrderDiscountPlan();
        if (userId == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            plan.setTotalDiscount(BigDecimal.ZERO);
            return plan;
        }
        UserCoupon best = selectBestCoupon(userId, orderAmount);
        if (best == null) {
            plan.setTotalDiscount(BigDecimal.ZERO);
            return plan;
        }
        CouponTemplate template = couponTemplateMapper.selectById(best.getCouponId());
        if (template == null) {
            plan.setTotalDiscount(BigDecimal.ZERO);
            return plan;
        }
        BigDecimal discount = calcCouponDiscount(template, orderAmount);
        plan.setUserCouponId(best.getId());
        plan.setCouponTemplateId(template.getId());
        plan.setCouponDiscount(discount);
        plan.setTotalDiscount(discount);
        List<String> activities = new ArrayList<>();
        activities.add("优惠券#" + template.getName());
        plan.setAppliedActivities(activities);
        plan.setDescription(String.format("优惠券优惠%s元", discount.toPlainString()));
        return plan;
    }

    private UserCoupon selectBestCoupon(Long userId, BigDecimal orderAmount) {
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, 0)
                .gt(UserCoupon::getValidEndTime, LocalDateTime.now());
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        if (coupons.isEmpty()) {
            return null;
        }
        // 计算每张券的优惠金额，选最大
        UserCoupon best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (UserCoupon uc : coupons) {
            CouponTemplate template = couponTemplateMapper.selectById(uc.getCouponId());
            if (template == null || template.getMinAmount() == null) {
                continue;
            }
            if (orderAmount.compareTo(template.getMinAmount()) < 0) {
                continue;
            }
            BigDecimal discount = calcCouponDiscount(template, orderAmount);
            if (discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
                best = uc;
            }
        }
        return best;
    }

    private BigDecimal calcCouponDiscount(CouponTemplate template, BigDecimal orderAmount) {
        if (template == null) return BigDecimal.ZERO;
        if (template.getDiscountType() != null && template.getDiscountType() == 2) {
            // 百分比折扣
            BigDecimal rate = template.getDiscountValue()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            BigDecimal discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate));
            if (template.getMaxDiscount() != null && discount.compareTo(template.getMaxDiscount()) > 0) {
                discount = template.getMaxDiscount();
            }
            return discount;
        } else {
            // 固定金额
            BigDecimal discount = template.getDiscountValue();
            if (discount.compareTo(orderAmount) > 0) {
                discount = orderAmount;
            }
            return discount;
        }
    }
}
