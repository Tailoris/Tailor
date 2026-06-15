package com.tailoris.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tailoris.analytics.entity.MetricsSnapshot;
import com.tailoris.analytics.mapper.MetricsSnapshotMapper;
import com.tailoris.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl extends ServiceImpl<MetricsSnapshotMapper, MetricsSnapshot> implements AnalyticsService {

    private final MetricsSnapshotMapper metricsSnapshotMapper;

    @Override
    public void recordMetric(String metricType, String metricKey, BigDecimal metricValue, LocalDate snapshotDate, String dimension) {
        MetricsSnapshot snapshot = new MetricsSnapshot();
        snapshot.setMetricType(metricType);
        snapshot.setMetricKey(metricKey);
        snapshot.setMetricValue(metricValue);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setDimension(dimension);
        metricsSnapshotMapper.insert(snapshot);
    }

    @Override
    public BigDecimal getMetricValue(String metricType, String metricKey, LocalDate snapshotDate) {
        LambdaQueryWrapper<MetricsSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricsSnapshot::getMetricType, metricType)
                .eq(MetricsSnapshot::getMetricKey, metricKey)
                .eq(MetricsSnapshot::getSnapshotDate, snapshotDate);
        MetricsSnapshot snapshot = metricsSnapshotMapper.selectOne(wrapper);
        return snapshot != null ? snapshot.getMetricValue() : BigDecimal.ZERO;
    }

    @Override
    public List<MetricsSnapshot> getMetricsByTypeAndDateRange(String metricType, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<MetricsSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricsSnapshot::getMetricType, metricType)
                .between(MetricsSnapshot::getSnapshotDate, startDate, endDate)
                .orderByAsc(MetricsSnapshot::getSnapshotDate);
        return metricsSnapshotMapper.selectList(wrapper);
    }

    @Override
    public List<MetricsSnapshot> getMetricsByKeyAndDimension(String metricKey, String dimension) {
        LambdaQueryWrapper<MetricsSnapshot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricsSnapshot::getMetricKey, metricKey)
                .eq(MetricsSnapshot::getDimension, dimension)
                .orderByDesc(MetricsSnapshot::getSnapshotDate);
        return metricsSnapshotMapper.selectList(wrapper);
    }
}