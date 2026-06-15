package com.tailoris.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 审核状态枚举.
 *
 * @author Tailor IS Team
 * @since 1.1.0
 */
@Getter
@AllArgsConstructor
public enum AuditStatusEnum {

    /** 待审核 */
    PENDING(0, "待审核"),

    /** 审核通过 */
    APPROVED(1, "审核通过"),

    /** 审核拒绝 */
    REJECTED(2, "审核拒绝");

    private final int code;
    private final String description;

    public static AuditStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.code, code))
                .findFirst()
                .orElse(null);
    }
}
