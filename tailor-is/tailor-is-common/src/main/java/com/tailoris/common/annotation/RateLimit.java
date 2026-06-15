package com.tailoris.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解 - 修复 B-C09
 *
 * <p>使用方式：在Controller方法上添加 @RateLimit 注解</p>
 *
 * <pre>{@code
 * @RateLimit(key = "login", permitsPerSecond = 10, capacity = 60)
 * @PostMapping("/login")
 * public Result<?> login(...) { ... }
 * }</pre>
 *
 * <p>需要配合 RateLimitInterceptor 拦截器生效。</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流Key前缀，区分不同接口 */
    String key() default "";

    /** 每秒允许的请求数 */
    int permitsPerSecond() default 10;

    /** 令牌桶容量（最大突发请求数） */
    int capacity() default 60;

    /** 限流提示信息 */
    String message() default "请求过于频繁，请稍后再试";

    /** 限流维度：IP / USER / GLOBAL */
    LimitType limitType() default LimitType.IP;
}
