package com.tailoris.marketing.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 营销与订单联动 Service
 * 任务编号: MKT-007
 *
 * <p>在订单结算时，根据用户优惠券、积分、活动规则计算最优优惠方案</p>
 */
public interface MktOrderMatchService {

    /**
     * 优惠方案 DTO
     */
    class OrderDiscountPlan {
        /** 使用的优惠券ID（用户优惠券ID） */
        private Long userCouponId;
        /** 使用的优惠券模板ID */
        private Long couponTemplateId;
        /** 优惠券抵扣金额 */
        private BigDecimal couponDiscount;
        /** 满减满赠活动ID */
        private Long promotionId;
        /** 满减满赠优惠金额 */
        private BigDecimal promotionDiscount;
        /** 使用的积分数 */
        private Integer pointsUsed;
        /** 积分抵扣金额 */
        private BigDecimal pointsDiscount;
        /** 总优惠金额 */
        private BigDecimal totalDiscount;
        /** 优惠明细描述 */
        private String description;
        /** 使用的活动列表（拼团/秒杀） */
        private List<String> appliedActivities;

        public Long getUserCouponId() { return userCouponId; }
        public void setUserCouponId(Long userCouponId) { this.userCouponId = userCouponId; }
        public Long getCouponTemplateId() { return couponTemplateId; }
        public void setCouponTemplateId(Long couponTemplateId) { this.couponTemplateId = couponTemplateId; }
        public BigDecimal getCouponDiscount() { return couponDiscount; }
        public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }
        public Long getPromotionId() { return promotionId; }
        public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }
        public BigDecimal getPromotionDiscount() { return promotionDiscount; }
        public void setPromotionDiscount(BigDecimal promotionDiscount) { this.promotionDiscount = promotionDiscount; }
        public Integer getPointsUsed() { return pointsUsed; }
        public void setPointsUsed(Integer pointsUsed) { this.pointsUsed = pointsUsed; }
        public BigDecimal getPointsDiscount() { return pointsDiscount; }
        public void setPointsDiscount(BigDecimal pointsDiscount) { this.pointsDiscount = pointsDiscount; }
        public BigDecimal getTotalDiscount() { return totalDiscount; }
        public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getAppliedActivities() { return appliedActivities; }
        public void setAppliedActivities(List<String> appliedActivities) { this.appliedActivities = appliedActivities; }
    }

    /**
     * 订单项 DTO（用于匹配）
     */
    class OrderItemInput {
        private Long productId;
        private Long skuId;
        private Long shopId;
        private BigDecimal price;
        private Integer quantity;
        /** 命中活动类型：2=拼团 3=秒杀 0=无 */
        private Integer activePromotionType;
        private Long activePromotionId;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Long getSkuId() { return skuId; }
        public void setSkuId(Long skuId) { this.skuId = skuId; }
        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Integer getActivePromotionType() { return activePromotionType; }
        public void setActivePromotionType(Integer activePromotionType) { this.activePromotionType = activePromotionType; }
        public Long getActivePromotionId() { return activePromotionId; }
        public void setActivePromotionId(Long activePromotionId) { this.activePromotionId = activePromotionId; }

        public BigDecimal getSubtotal() {
            if (price == null || quantity == null) return BigDecimal.ZERO;
            return price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /**
     * 计算订单最优优惠方案
     *
     * @param userId       用户ID
     * @param items        订单商品列表
     * @param totalAmount  订单总金额
     * @return 优惠方案（不会为null，但可能 totalDiscount = 0）
     */
    OrderDiscountPlan calculateOptimal(Long userId, List<OrderItemInput> items, BigDecimal totalAmount);

    /**
     * 仅计算优惠券最优（不叠加其他活动）
     */
    OrderDiscountPlan calculateCouponOnly(Long userId, BigDecimal orderAmount);
}
