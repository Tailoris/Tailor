package com.tailoris.common.interceptor;

import com.tailoris.common.annotation.Idempotent;
import com.tailoris.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 幂等性拦截器 - L4 幂等性机制
 *
 * <p>基于 Redis SETNX 实现分布式幂等控制，防止重复提交。</p>
 *
 * <h3>工作流程</h3>
 * <ul>
 *   <li>拦截标注了 @Idempotent 的请求</li>
 *   <li>从请求头 X-Idempotent-Key 获取幂等键，不存在则从请求参数 idempotentKey 获取</li>
 *   <li>使用 Redis SETNX 尝试设置键，设置成功则放行，失败则视为重复提交</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String IDEMPOTENT_KEY_HEADER = "X-Idempotent-Key";
    private static final String IDEMPOTENT_KEY_PARAM = "idempotentKey";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            return true;
        }

        String idempotentKey = resolveIdempotentKey(request);
        if (idempotentKey == null || idempotentKey.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "缺少幂等键");
        }

        String redisKey = idempotent.key() + idempotentKey;

        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", idempotent.expireSeconds(), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(success)) {
            log.warn("幂等拦截: key={}, uri={}", redisKey, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS.value(), idempotent.message());
        }

        log.debug("幂等放行: key={}, uri={}", redisKey, request.getRequestURI());
        return true;
    }

    /**
     * 解析幂等键：优先从请求头获取，其次从请求参数获取
     */
    private String resolveIdempotentKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENT_KEY_HEADER);
        if (key != null && !key.isBlank()) {
            return key;
        }
        return request.getParameter(IDEMPOTENT_KEY_PARAM);
    }
}
