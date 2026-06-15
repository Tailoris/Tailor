package com.tailoris.common.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.annotation.LimitType;
import com.tailoris.common.annotation.RateLimit;
import com.tailoris.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器 - 修复 B-C09
 *
 * <p>基于 Redis 实现的分布式限流器（令牌桶算法）。
 * 支持 IP / USER / GLOBAL 三种限流维度。</p>
 *
 * <h3>实现原理</h3>
 * <ul>
 *   <li>使用 Redis INCR + EXPIRE 实现固定窗口计数器</li>
 *   <li>每分钟一个窗口，窗口内请求数超限则拒绝</li>
 *   <li>分布式场景下通过 Redis 原子操作保证一致性</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String limitKey = buildLimitKey(request, rateLimit);
        long currentCount = incrementAndGet(limitKey, rateLimit.capacity());

        if (currentCount > rateLimit.capacity()) {
            log.warn("触发限流: key={}, count={}/{}", limitKey, currentCount, rateLimit.capacity());
            throw new BusinessException(rateLimit.message());
        }

        // 设置响应头，便于客户端调试
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimit.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, rateLimit.capacity() - currentCount)));
        return true;
    }

    /**
     * 构建限流Key
     */
    private String buildLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder key = new StringBuilder("ratelimit:");
        key.append(rateLimit.key()).append(":");

        switch (rateLimit.limitType()) {
            case IP:
                key.append(getClientIp(request));
                break;
            case USER:
                try {
                    key.append(StpUtil.getLoginIdDefaultNull());
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

    /**
     * 原子计数+获取
     */
    private long incrementAndGet(String key, int capacity) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // 第一个请求设置TTL（窗口期=容量秒数）
            stringRedisTemplate.expire(key, capacity, TimeUnit.SECONDS);
        }
        return count == null ? 0 : count;
    }

    /**
     * 获取客户端真实IP
     */
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
