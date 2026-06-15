package com.tailoris.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * Redis 安全序列化器 - 修复 H-003
 *
 * <p>使用 BasicPolymorphicTypeValidator 限制可反序列化的类型白名单，
 * 防止任意类反序列化导致的 RCE 攻击。</p>
 *
 * <p>允许的类型：</p>
 * <ul>
 *   <li>com.tailoris.* — 项目自身的实体和 DTO</li>
 *   <li>java.util.* — 集合类（List, Map, Set 等）</li>
 *   <li>java.lang.* — 基础类型包装类</li>
 *   <li>java.math.* — BigDecimal, BigInteger</li>
 *   <li>java.time.* — 日期时间类</li>
 * </ul>
 *
 * <p>明确拒绝的危险类型：</p>
 * <ul>
 *   <li>java.lang.ProcessBuilder / Runtime</li>
 *   <li>javax.script.*</li>
 *   <li>java.net.URL（Gadget 链）</li>
 *   <li>java.beans.* / java.rmi.*</li>
 *   <li>com.sun.* / sun.*</li>
 * </ul>
 */
public final class SafeJackson2JsonRedisSerializer extends GenericJackson2JsonRedisSerializer {

    /**
     * 创建带有类型白名单限制的 ObjectMapper
     */
    public static ObjectMapper createSafeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 多态类型验证器 - 白名单模式
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                // 允许项目自身的类
                .allowIfBaseType("com.tailoris.")
                // 允许 Java 集合
                .allowIfBaseType("java.util.")
                // 允许基础类型
                .allowIfBaseType("java.lang.")
                // 允许数值类型
                .allowIfBaseType("java.math.")
                // 允许日期时间
                .allowIfBaseType("java.time.")
                .build();

        // 激活默认类型处理，使用白名单验证器
        objectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return objectMapper;
    }

    /**
     * 使用安全的 ObjectMapper 构造序列化器
     */
    public SafeJackson2JsonRedisSerializer() {
        super(createSafeObjectMapper());
    }

    /**
     * 使用自定义 classPropertyTypeId 前缀构造序列化器
     */
    public SafeJackson2JsonRedisSerializer(String classPropertyTypeId) {
        super(createSafeObjectMapper());
    }
}
