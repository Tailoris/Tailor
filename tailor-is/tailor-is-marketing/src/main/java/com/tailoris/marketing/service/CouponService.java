package com.tailoris.marketing.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.dto.CouponCreateRequest;
import com.tailoris.marketing.entity.CouponTemplate;
import com.tailoris.marketing.entity.UserCoupon;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务接口.
 *
 * <p>提供优惠券模板创建、用户领取、使用、查询等核心功能。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface CouponService {

    /**
     * 创建优惠券模板.
     *
     * @param request 优惠券创建请求
     * @return 创建成功的优惠券模板
     */
    CouponTemplate createCoupon(CouponCreateRequest request);

    /**
     * 用户领取优惠券.
     *
     * @param userId 用户ID
     * @param couponId 优惠券模板ID
     */
    void receiveCoupon(Long userId, Long couponId);

    /**
     * 用户使用优惠券.
     *
     * @param userId 用户ID
     * @param userCouponId 用户优惠券ID
     * @param orderId 订单ID
     */
    void useCoupon(Long userId, Long userCouponId, Long orderId);

    /**
     * 分页查询用户优惠券列表.
     *
     * @param userId 用户ID
     * @param pageRequest 分页参数
     * @param status 优惠券状态（可选）
     * @return 用户优惠券分页列表
     */
    PageResponse<UserCoupon> listCoupons(Long userId, PageRequest pageRequest, Integer status);

    /**
     * 查询可用优惠券.
     *
     * @param userId 用户ID
     * @param orderAmount 订单金额
     * @param shopId 店铺ID（可选）
     * @return 可用优惠券列表
     */
    List<UserCoupon> getAvailableCoupons(Long userId, BigDecimal orderAmount, Long shopId);

    /**
     * 查询优惠券模板详情.
     *
     * @param couponId 优惠券模板ID
     * @return 优惠券模板信息
     */
    CouponTemplate getCouponTemplate(Long couponId);
}
