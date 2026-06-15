package com.tailoris.marketing.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.dto.PointsExchangeRequest;
import com.tailoris.marketing.entity.PointsMallProduct;
import com.tailoris.marketing.entity.PointsRecord;

/**
 * 积分服务接口.
 *
 * <p>提供积分查询、兑换、记录等核心功能。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface PointsService {

    /**
     * 积分兑换.
     *
     * @param userId 用户ID
     * @param request 兑换请求
     */
    void exchangePoints(Long userId, PointsExchangeRequest request);

    /**
     * 记录积分变动.
     *
     * @param userId 用户ID
     * @param pointsChange 积分变动值（正数增加，负数减少）
     * @param changeType 变动类型
     * @param relatedType 关联业务类型
     * @param relatedId 关联业务ID
     * @param description 变动描述
     */
    void recordPoints(Long userId, Integer pointsChange, Integer changeType, String relatedType, Long relatedId, String description);

    /**
     * 查询积分余额.
     *
     * @param userId 用户ID
     * @return 当前积分余额
     */
    Integer getPointsBalance(Long userId);

    /**
     * 分页查询积分历史记录.
     *
     * @param userId 用户ID
     * @param pageRequest 分页参数
     * @return 积分变动记录分页列表
     */
    PageResponse<PointsRecord> getPointsHistory(Long userId, PageRequest pageRequest);

    /**
     * 分页查询积分商城商品.
     *
     * @param pageRequest 分页参数
     * @return 积分商城商品分页列表
     */
    PageResponse<PointsMallProduct> listPointsMallProducts(PageRequest pageRequest);
}
