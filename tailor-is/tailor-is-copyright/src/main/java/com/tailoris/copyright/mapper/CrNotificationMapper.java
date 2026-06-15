package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrNotification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CrNotificationMapper extends BaseMapper<CrNotification> {

    @Select("SELECT * FROM cr_notification WHERE user_id = #{userId} AND deleted = 0 ORDER BY create_time DESC LIMIT 200")
    List<CrNotification> selectByUser(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM cr_notification WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    long countUnread(@Param("userId") Long userId);

    @Update("UPDATE cr_notification SET is_read = 1, read_time = NOW() WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    int markAllRead(@Param("userId") Long userId);
}
