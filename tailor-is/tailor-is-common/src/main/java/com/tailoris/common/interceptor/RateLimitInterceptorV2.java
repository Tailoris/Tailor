package com.tailoris.common.interceptor;

import com.tailoris.common.annotation.LimitType;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.config.RateLimitDynamicConfig;
import com.tailoris.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流拦截器 - 增强版 (修复 B-H10)
 *
 * <p>基于动态配置 + 本地降级的限流拦截器：</p>
 * <ul>
 *   <li>支持Nacos动态配置调整阈值</li>
 *   <li>Redis不可用时降级到本地限流</li>
 *   <li>支持多种限流维度</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptorV2 implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitDynamicConfig dynamicConfig;

    /** 本地限流计数（降级使用） */
    private final java.util.concurrent.ConcurrentHashMap<String, AtomicLong> localCounters =
            new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> localResetTimes =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        if (!dynamicConfig.isEnabled()) {
            return true;
        }

        // 优先使用动态配置
        RateLimitDynamicConfig.RateLimitConfigEntry config = dynamicConfig.getConfig(rateLimit.key());
        int permitsPerSecond = config.getPermitsPerSecond() > 0
                ? config.getPermitsPerSecond() : rateLimit.permitsPerSecond();
        int capacity = config.getCapacity() > 0
                ? config.getCapacity() : rateLimit.capacity();
        String message = config.getMessage() != null
                ? config.getMessage() : rateLimit.message();

        String limitKey = buildLimitKey(request, rateLimit);

        // 尝试Redis限流
        try {
            return doRedisLimit(limitKey, capacity, message, response);
        } catch (RedisConnectionFailureException e) {
            // Redis故障，降级到本地限流
            log.warn("Redis不可用，降级到本地限流: key={}", limitKey);
            return doLocalLimit(limitKey, capacity, message, response);
        }
    }

    private boolean doRedisLimit(String key, int capacity, String message, HttpServletResponse response) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, capacity, TimeUnit.SECONDS);
        }

        long current = count == null ? 0 : count;
        if (current > capacity) {
            log.warn("触发限流: key={}, count={}/{}", key, current, capacity);
            throw new BusinessException(message);
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, capacity - current)));
        return true;
    }

    private boolean doLocalLimit(String key, int capacity, String message, HttpServletResponse response) {
        long now = System.currentTimeMillis();
        Long resetTime = localResetTimes.get(key);

        if (resetTime == null || now > resetTime) {
            localCounters.computeIfAbsent(key, k -> new AtomicLong(0));
            localResetTimes.put(key, now + capacity * 1000L);
        }

        AtomicLong counter = localCounters.computeIfAbsent(key, k -> new AtomicLong(0));
        long current = counter.incrementAndGet();

        if (current > capacity) {
            throw new BusinessException(message + " (本地限流)");
        }

        response.setHeader("X-RateLimit-Local", "true");
        return true;
    }

    private String buildLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder("ratelimit:");
        key.append(rateLimit.key()).append(":");

        LimitType type;
        try {
            type = rateLimit.limitType();
        } catch (Exception e) {
            type = LimitType.IP;
        }

        switch (type) {
            case IP:
                key.append(getClientIp(request));
                break;
            case USER:
                try {
                    String auth = request.getHeader("Authorization");
                    if (auth != null) {
                        key.append(auth.hashCode());
                    } else {
                        key.append(getClientIp(request));
                    }
                } catch (Exception e) {
                    key.append(getClientIp(request));
                }
                break;
            case GLOBAL:
            default:
                key.append("global");
                break;
        }
        return key.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "unknown" : ip;
    }
}
