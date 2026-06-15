package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.DigitalPattern;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DigitalPatternMapper extends BaseMapper<DigitalPattern> {

    /**
     * 删除商品对应的数字纸样.
     */
    @Update("UPDATE digital_pattern SET deleted = 1, update_time = NOW() WHERE product_id = #{productId} AND deleted = 0")
    int deleteByProductId(@Param("productId") Long productId);

    /**
     * 增加下载次数.
     */
    @Update("UPDATE digital_pattern SET download_count = download_count + 1, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int incrementDownloadCount(@Param("id") Long id);

    /**
     * 查询用户拥有的纸样列表.
     */
    List<DigitalPattern> listOwnedByUser(@Param("userId") Long userId);

    /**
     * 根据商品ID查询.
     */
    DigitalPattern selectByProductId(@Param("productId") Long productId);
}
