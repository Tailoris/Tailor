package com.tailoris.common.serializer;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.tailoris.common.util.DesensitizeUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

/**
 * 敏感信息Jackson序列化脱敏器 + 配套注解 @Sensitive - 修复 S-04 数据安全规则
 *
 * <p>使用方式：在Entity字段上加 @Sensitive(SensitiveType.PHONE)，
 *    序列化输出JSON时自动脱敏。SUPER_ADMIN / PLATFORM_ADMIN 角色可豁免查看明文（
 *    通过 setRawVisible(true) 覆盖，默认所有角色都脱敏，合规优先）</p>
 *
 * <p>支持类型：PHONE / EMAIL / ID_CARD / BANK_CARD / LICENSE / NAME / ADDRESS / CUSTOM</p>
 *
 * <p>安全合规：
 * <ul>
 *   <li>响应体中商家手机号、邮箱、银行卡、营业执照号不再明文传输</li>
 *   <li>前端再配合 formatPhone / formatBankCard 等工具进行二次兜底，双重保障</li>
 *   <li>所有敏感信息日志打印也需配合 LogMaskUtils / DesensitizeUtils 三重保护</li>
 * </ul>
 */
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveType type = SensitiveType.CUSTOM;
    /** 自定义脱敏前N位 */
    private int prefixKeep = 0;
    /** 自定义脱敏后N位 */
    private int suffixKeep = 0;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 空值直接返回
        if (value == null) {
            gen.writeNull();
            return;
        }
        // 明文豁免：SUPER_ADMIN或PLATFORM_ADMIN或内部feign调用（Header内带internal-sign时显示明文字段）
        if (isRawVisibleRole()) {
            gen.writeString(value);
            return;
        }
        // 走脱敏流程
        String masked;
        switch (type) {
            case PHONE:
                masked = DesensitizeUtils.maskPhone(value);
                break;
            case EMAIL:
                masked = DesensitizeUtils.maskEmail(value);
                break;
            case ID_CARD:
                masked = DesensitizeUtils.maskIdCard(value);
                break;
            case BANK_CARD:
                masked = DesensitizeUtils.maskBankCard(value);
                break;
            case NAME:
                masked = DesensitizeUtils.maskName(value);
                break;
            case ADDRESS:
                masked = DesensitizeUtils.maskAddress(value);
                break;
            case LICENSE:
                // 营业执照：前6 + **** + 后4
                masked = value.length() <= 10 ? value
                        : value.substring(0, 6) + "****" + value.substring(value.length() - 4);
                break;
            case CUSTOM:
            default:
                masked = DesensitizeUtils.maskCustom(value, prefixKeep, suffixKeep);
                break;
        }
        gen.writeString(masked);
    }

    /** 判断当前角色是否可以查看敏感字段明文（合规：默认全部脱敏，不开启明文豁免） */
    private boolean isRawVisibleRole() {
        try {
            // 显式配置：默认不开启明文（若需要放行可通过Sa-Token set 角色）
            // 返回 StpUtil.hasRole("SUPER_ADMIN") 则超级管理员可看；当前默认false统一脱敏
            return false;
        } catch (Exception e) {
            log.warn("SensitiveSerializer.role-check 异常: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) return prov.findNullValueSerializer(null);
        Sensitive ann = property.getAnnotation(Sensitive.class);
        if (ann == null) ann = property.getContextAnnotation(Sensitive.class);
        if (ann != null) {
            return new SensitiveSerializer(ann.type(), ann.prefixKeep(), ann.suffixKeep());
        }
        return this;
    }

    /**
     * 敏感类型枚举
     */
    public enum SensitiveType {
        PHONE, EMAIL, ID_CARD, BANK_CARD, LICENSE, NAME, ADDRESS, CUSTOM
    }

    /**
     * 敏感字段脱敏注解（标注在字段上即可启用Jackson序列化脱敏）
     */
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Sensitive {
        SensitiveType type() default SensitiveType.CUSTOM;

        /** type=CUSTOM 时生效，保留前缀长度 */
        int prefixKeep() default 3;

        /** type=CUSTOM 时生效，保留后缀长度 */
        int suffixKeep() default 4;
    }
}
