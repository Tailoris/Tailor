package com.tailoris.copyright.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.copyright.entity.CrBlockchainEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CrBlockchainEventMapper extends BaseMapper<CrBlockchainEvent> {

    @Select("SELECT * FROM cr_blockchain_event WHERE processed = 0 AND retry_count < 3 AND deleted = 0 ORDER BY create_time ASC LIMIT 100")
    List<CrBlockchainEvent> selectUnprocessed();

    @Update("UPDATE cr_blockchain_event SET processed = 1, process_time = NOW(), process_result = #{result} WHERE id = #{id}")
    int markProcessed(@Param("id") Long id, @Param("result") String result);

    @Update("UPDATE cr_blockchain_event SET retry_count = retry_count + 1 WHERE id = #{id}")
    int incrRetry(@Param("id") Long id);
}
