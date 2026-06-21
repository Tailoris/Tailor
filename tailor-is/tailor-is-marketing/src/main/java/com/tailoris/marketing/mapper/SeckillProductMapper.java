package com.tailoris.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.marketing.entity.SeckillProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    /** BE-H-13: 原子扣减库存 - 避免并发竞态条件 */
    @Update("UPDATE seckill_product SET available_stock = available_stock - 1, order_count = order_count + 1 WHERE id = #{id} AND available_stock > 0")
    int deductStock(@Param("id") Long id);

    /** BE-H-13: 原子恢复库存 - 取消订单时恢复 */
    @Update("UPDATE seckill_product SET available_stock = available_stock + 1, order_count = GREATEST(order_count - 1, 0) WHERE id = #{id}")
    int restoreStock(@Param("id") Long id);
}
