package com.tailoris.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等性注解 - L4 幂等性机制
 *
 * <p>使用方式：在 Controller 方法上添加 @Idempotent 注解</p>
 *
 * <pre>{@code
 * @Idempotent(key = "order:create", expireSeconds = 600)
 * @PostMapping("/create")
 * public Result<?> createOrder(...) { ... }
 * }</pre>
 *
 * <p>客户端需在请求头 X-Idempotent-Key 中传递唯一幂等键，
 * 否则将从请求参数中尝试获取。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /** 幂等键前缀 */
    String key() default "idempotent:";

    /** 幂等键过期时间（秒） */
    int expireSeconds() default 300;

    /** 重复提交提示信息 */
    String message() default "请勿重复提交";
}
