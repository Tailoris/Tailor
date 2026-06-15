package com.tailoris.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 商品状态枚举 - 修复 B-M38
 *
 * <p>替换魔法数字，提供类型安全的商品状态管理。</p>
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Getter
@AllArgsConstructor
public enum ProductStatusEnum {

    /** 下架 */
    OFF_SHELF(0, "下架"),

    /** 上架 */
    ON_SHELF(1, "上架"),

    /** 草稿 */
    DRAFT(2, "草稿"),

    /** 违规下架 */
    VIOLATED_OFF_SHELF(3, "违规下架");

    private final int code;
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static ProductStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.code, code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否可销售
     */
    public boolean isSellable() {
        return this == ON_SHELF;
    }
}
