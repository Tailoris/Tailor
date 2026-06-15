package com.tailoris.marketing.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.entity.MktPromotionRule;
import com.tailoris.marketing.entity.MktPromotionStep;

import java.math.BigDecimal;
import java.util.List;

/**
 * 阶梯满减满赠 Service
 * 任务编号: MKT-004
 */
public interface MktPromotionService {

    /**
     * 创建促销规则（含阶梯）
     */
    MktPromotionRule createPromotion(MktPromotionRule rule, List<MktPromotionStep> steps);

    /**
     * 更新促销
     */
    MktPromotionRule updatePromotion(MktPromotionRule rule, List<MktPromotionStep> steps);

    /**
     * 取消促销
     */
    void cancelPromotion(Long promotionId);

    /**
     * 促销详情
     */
    MktPromotionRule getPromotionDetail(Long promotionId);

    /**
     * 促销列表
     */
    PageResponse<MktPromotionRule> listPromotions(PageRequest pageRequest, Long shopId, Integer status);

    /**
     * 查询生效中的规则
     */
    List<MktPromotionRule> listActiveRules(Long shopId);

    /**
     * 计算订单最优优惠
     * @param shopId 店铺ID
     * @param totalAmount 订单总金额
     * @param itemCount 商品件数
     * @return 最优优惠方案
     */
    PromotionDiscountResult calculateOptimalDiscount(Long shopId, BigDecimal totalAmount, int itemCount);

    /**
     * 优惠结果封装
     */
    class PromotionDiscountResult {
        private Long promotionId;
        private String promotionName;
        private Integer promotionType;
        private BigDecimal discountAmount;
        private Long giftProductId;
        private Integer giftQuantity;
        private String description;

        public Long getPromotionId() { return promotionId; }
        public void setPromotionId(Long promotionId) { this.promotionId = promotionId; }
        public String getPromotionName() { return promotionName; }
        public void setPromotionName(String promotionName) { this.promotionName = promotionName; }
        public Integer getPromotionType() { return promotionType; }
        public void setPromotionType(Integer promotionType) { this.promotionType = promotionType; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public Long getGiftProductId() { return giftProductId; }
        public void setGiftProductId(Long giftProductId) { this.giftProductId = giftProductId; }
        public Integer getGiftQuantity() { return giftQuantity; }
        public void setGiftQuantity(Integer giftQuantity) { this.giftQuantity = giftQuantity; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
