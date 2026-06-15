package com.tailoris.analytics.service;

import com.tailoris.analytics.entity.MetricsSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AnalyticsService {

    void recordMetric(String metricType, String metricKey, BigDecimal metricValue, LocalDate snapshotDate, String dimension);

    BigDecimal getMetricValue(String metricType, String metricKey, LocalDate snapshotDate);

    List<MetricsSnapshot> getMetricsByTypeAndDateRange(String metricType, LocalDate startDate, LocalDate endDate);

    List<MetricsSnapshot> getMetricsByKeyAndDimension(String metricKey, String dimension);
}