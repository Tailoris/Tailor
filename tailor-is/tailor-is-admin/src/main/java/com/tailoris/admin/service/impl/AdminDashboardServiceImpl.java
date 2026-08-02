package com.tailoris.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.DashboardStatsResponse;
import com.tailoris.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${tailoris.datasource.user-db:tailor_is_user}")
    private String userDatabase;

    @Value("${tailoris.datasource.merchant-db:tailor_is_merchant}")
    private String merchantDatabase;

    @Value("${tailoris.datasource.order-db:tailoris_order}")
    private String orderDatabase;

    @Value("${tailoris.datasource.product-db:tailor_is_product}")
    private String productDatabase;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        String cacheKey = AdminConstants.REDIS_KEY_DASHBOARD_STATS;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            try {
                return objectMapper.readValue(cached, DashboardStatsResponse.class);
            } catch (JsonProcessingException e) {
                log.warn("Dashboard stats Redis deserialization failed", e);
            }
        }

        Long userCount = queryUserCount();

        Long merchantCount = queryMerchantCount();

        Long orderCount = queryOrderCount();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        String todayStartStr = todayStart.format(DateTimeFormatter.ofPattern(dateFormat));
        String todayEndStr = todayEnd.format(DateTimeFormatter.ofPattern(dateFormat));

        Long todayOrders = queryTodayOrders(todayStartStr, todayEndStr);

        BigDecimal todayRevenue = queryTodayRevenue(todayStartStr, todayEndStr);

        Long pendingMerchantCount = queryPendingMerchantCount();

        Long pendingProductCount = queryPendingProductCount();

        DashboardStatsResponse response = new DashboardStatsResponse();
        response.setUserCount(userCount);
        response.setMerchantCount(merchantCount);
        response.setOrderCount(orderCount);
        response.setTodayRevenue(todayRevenue);
        response.setTodayOrders(todayOrders);
        response.setPendingAuditCount(pendingMerchantCount + pendingProductCount);

        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(response),
                    AdminConstants.REDIS_DASHBOARD_CACHE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.warn("Dashboard stats Redis serialization failed", e);
        }

        return response;
    }

    private Long queryUserCount() {
        try {
            return jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.sys_user", userDatabase), Long.class);
        } catch (Exception e) {
            log.warn("Failed to query user count", e);
            return 0L;
        }
    }

    private Long queryMerchantCount() {
        try {
            return jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.merchant", merchantDatabase), Long.class);
        } catch (Exception e) {
            log.warn("Failed to query merchant count", e);
            return 0L;
        }
    }

    private Long queryOrderCount() {
        try {
            return jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.order_info", orderDatabase), Long.class);
        } catch (Exception e) {
            log.warn("Failed to query order count", e);
            return 0L;
        }
    }

    private Long queryTodayOrders(String start, String end) {
        try {
            return jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.order_info WHERE created_at >= ? AND created_at < ? AND status IN (2, 3, 4)", orderDatabase),
                    Long.class, start, end);
        } catch (Exception e) {
            log.warn("Failed to query today orders", e);
            return 0L;
        }
    }

    private BigDecimal queryTodayRevenue(String start, String end) {
        try {
            BigDecimal result = jdbcTemplate.queryForObject(
                    String.format("SELECT COALESCE(SUM(pay_amount), 0) FROM %s.order_info WHERE created_at >= ? AND created_at < ? AND status IN (2, 3, 4)", orderDatabase),
                    BigDecimal.class, start, end);
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to query today revenue", e);
            return BigDecimal.ZERO;
        }
    }

    private Long queryPendingMerchantCount() {
        try {
            return jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.merchant WHERE audit_status = 0", merchantDatabase), Long.class);
        } catch (Exception e) {
            log.warn("Failed to query pending merchant count", e);
            return 0L;
        }
    }

    private Long queryPendingProductCount() {
        try {
            return jdbcTemplate.queryForObject(
                    String.format("SELECT COUNT(*) FROM %s.product WHERE audit_status = 0", productDatabase), Long.class);
        } catch (Exception e) {
            log.warn("Failed to query pending product count", e);
            return 0L;
        }
    }
}
