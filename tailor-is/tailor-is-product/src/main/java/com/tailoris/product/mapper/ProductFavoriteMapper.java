package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.ProductFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 商品收藏 Mapper - USR-008 / PRD-007.
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Mapper
public interface ProductFavoriteMapper extends BaseMapper<ProductFavorite> {

    /**
     * 🔒 PRD-007: 根据商品ID软删除收藏记录.
     *
     * @param productId 商品ID
     * @return 影响行数
     */
    @Update("UPDATE product_favorite SET deleted = 1, update_time = NOW() WHERE product_id = #{productId} AND deleted = 0")
    int softDeleteByProductId(Long productId);
}
