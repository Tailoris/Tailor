package com.tailoris.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在类或方法上，表示使用核心缓存（Redis Cluster）。
 * 适用于 order, payment, ai, copyright, merchant, product 等核心服务。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CoreCache {

    /**
     * 缓存 key 前缀，支持 Spring EL 表达式。
     */
    String prefix() default "";

    /**
     * TTL（秒），0 表示使用默认值。
     */
    long ttlSeconds() default 0;
}
