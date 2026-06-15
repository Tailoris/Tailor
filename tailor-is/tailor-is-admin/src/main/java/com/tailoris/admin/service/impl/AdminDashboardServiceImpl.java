package com.tailoris.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailoris.admin.constant.AdminConstants;
import com.tailoris.api.admin.dto.DashboardStatsResponse;
import com.tailoris.admin.service.AdminDashboardService;
import com.tailoris.api.merchant.entity.Merchant;
import com.tailoris.api.merchant.mapper.MerchantMapper;
import com.tailoris.api.order.entity.OrderInfo;
import com.tailoris.api.order.mapper.OrderInfoMapper;
import com.tailoris.api.product.entity.Product;
import com.tailoris.api.product.mapper.ProductMapper;
import com.tailoris.api.user.entity.SysUser;
import com.tailoris.api.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final SysUserMapper sysUserMapper;
    private final MerchantMapper merchantMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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

        LambdaQueryWrapper<SysUser> userQuery = new LambdaQueryWrapper<>();
        Long userCount = sysUserMapper.selectCount(userQuery);

        LambdaQueryWrapper<Merchant> merchantQuery = new LambdaQueryWrapper<>();
        Long merchantCount = merchantMapper.selectCount(merchantQuery);

        LambdaQueryWrapper<OrderInfo> orderQuery = new LambdaQueryWrapper<>();
        Long orderCount = orderInfoMapper.selectCount(orderQuery);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        LambdaQueryWrapper<OrderInfo> todayPayQuery = new LambdaQueryWrapper<>();
        todayPayQuery.ge(OrderInfo::getCreateTime, todayStart)
                .lt(OrderInfo::getCreateTime, todayEnd)
                .in(OrderInfo::getStatus, AdminConstants.ORDER_STATUS_PAID,
                        AdminConstants.ORDER_STATUS_DELIVERED,
                        AdminConstants.ORDER_STATUS_COMPLETED);
        Long todayOrders = orderInfoMapper.selectCount(todayPayQuery);

        LambdaQueryWrapper<OrderInfo> revenueQuery = new LambdaQueryWrapper<>();
        revenueQuery.ge(OrderInfo::getCreateTime, todayStart)
                .lt(OrderInfo::getCreateTime, todayEnd)
                .in(OrderInfo::getStatus, AdminConstants.ORDER_STATUS_PAID,
                        AdminConstants.ORDER_STATUS_DELIVERED,
                        AdminConstants.ORDER_STATUS_COMPLETED);
        BigDecimal todayRevenue = BigDecimal.ZERO;
        var paidOrders = orderInfoMapper.selectList(revenueQuery);
        for (OrderInfo order : paidOrders) {
            if (order.getPayAmount() != null) {
                todayRevenue = todayRevenue.add(order.getPayAmount());
            }
        }

        LambdaQueryWrapper<Merchant> pendingMerchantQuery = new LambdaQueryWrapper<>();
        pendingMerchantQuery.eq(Merchant::getAuditStatus, AdminConstants.AUDIT_STATUS_PENDING);
        Long pendingMerchantCount = merchantMapper.selectCount(pendingMerchantQuery);

        LambdaQueryWrapper<Product> pendingProductQuery = new LambdaQueryWrapper<>();
        pendingProductQuery.eq(Product::getAuditStatus, AdminConstants.AUDIT_STATUS_PENDING);
        Long pendingProductCount = productMapper.selectCount(pendingProductQuery);

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
}
