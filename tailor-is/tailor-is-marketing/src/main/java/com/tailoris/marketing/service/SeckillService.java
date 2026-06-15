package com.tailoris.marketing.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.marketing.dto.SeckillCreateRequest;
import com.tailoris.marketing.entity.SeckillActivity;
import com.tailoris.marketing.entity.SeckillProduct;

import java.util.List;

/**
 * 秒杀服务接口.
 *
 * <p>提供秒杀活动创建、用户参与、商品查询等核心功能。
 * 秒杀场景需要保证高并发下的库存扣减原子性和防超卖。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public interface SeckillService {

    /**
     * 创建秒杀活动.
     *
     * @param request 活动创建请求，包含活动信息和商品列表
     * @return 创建成功的秒杀活动
     */
    SeckillActivity createActivity(SeckillCreateRequest request);

    /**
     * 用户参与秒杀.
     *
     * <p>使用分布式锁+Redis缓存保证高并发下库存扣减的原子性，防止超卖。</p>
     *
     * @param userId 用户ID
     * @param seckillProductId 秒杀商品ID
     */
    void joinSeckill(Long userId, Long seckillProductId);

    /**
     * 取消秒杀参与.
     *
     * @param userId 用户ID
     * @param seckillProductId 秒杀商品ID
     */
    void cancelSeckill(Long userId, Long seckillProductId);

    /**
     * 分页查询秒杀商品.
     *
     * @param pageRequest 分页参数
     * @param activityId 活动ID（可选）
     * @return 秒杀商品分页列表
     */
    PageResponse<SeckillProduct> listSeckillProducts(PageRequest pageRequest, Long activityId);

    /**
     * 查询进行中的秒杀活动.
     *
     * @return 当前正在进行中的秒杀活动列表
     */
    List<SeckillActivity> listActiveActivities();

    /**
     * 查询秒杀商品详情.
     *
     * @param seckillProductId 秒杀商品ID
     * @return 秒杀商品信息
     */
    SeckillProduct getSeckillProduct(Long seckillProductId);
}
