package com.tailoris.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.merchant.entity.MerchantNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Merchant notice data access layer
 * <p>商家公告数据访问层</p>
 *
 * @author Tailor IS Team
 * @since 2026-05-31
 */
@Mapper
public interface MerchantNoticeMapper extends BaseMapper<MerchantNotice> {

    /**
     * Mark notice as read
     *
     * @param noticeId notice ID
     * @param userId user ID
     * @return affected rows
     */
    @Update("UPDATE merchant_notice SET read_status = 1, read_time = NOW() " +
            "WHERE id = #{noticeId} AND (read_status IS NULL OR read_status = 0)")
    int markAsRead(@Param("noticeId") Long noticeId, @Param("userId") Long userId);
}
