package com.tailoris.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.product.entity.PatternDownloadToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PatternDownloadTokenMapper extends BaseMapper<PatternDownloadToken> {

    /**
     * 通过 token 查询.
     */
    PatternDownloadToken selectByToken(@Param("token") String token);

    /**
     * 原子消费（CAS 乐观锁，used_count 必须在 max_download_count 范围内）.
     *
     * @return 影响行数，0表示已被消费
     */
    @Update("UPDATE pattern_download_token SET used_count = used_count + 1, update_time = NOW() " +
            "WHERE id = #{id} AND deleted = 0 AND used_count < max_download_count " +
            "AND (expire_time IS NULL OR expire_time > NOW())")
    int atomicConsume(@Param("id") Long id);
}
