package com.tailoris.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tailoris.analytics.entity.MetricsSnapshot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricsSnapshotMapper extends BaseMapper<MetricsSnapshot> {
}