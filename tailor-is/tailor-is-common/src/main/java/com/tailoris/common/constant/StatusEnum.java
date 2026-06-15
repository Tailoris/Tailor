package com.tailoris.common.constant;

public enum StatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用"),

    PENDING_AUDIT(0, "待审核"),
    AUDIT_APPROVED(2, "审核通过"),
    AUDIT_REJECTED(3, "审核驳回"),

    PRODUCT_DRAFT(0, "草稿"),
    PRODUCT_PENDING_AUDIT(1, "待审核"),
    PRODUCT_PUBLISHED(2, "已上架"),
    PRODUCT_OFF_SHELF(3, "已下架"),
    PRODUCT_FORBIDDEN(4, "已封禁"),

    PAYMENT_PENDING(0, "待支付"),
    PAYMENT_SUCCESS(1, "已支付"),
    PAYMENT_FAILED(2, "支付失败"),

    REFUND_PENDING(0, "待退款"),
    REFUND_SUCCESS(1, "退款成功"),
    REFUND_FAILED(2, "退款失败");

    private final int code;
    private final String description;

    StatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static String getDescription(int code) {
        for (StatusEnum status : values()) {
            if (status.code == code) {
                return status.description;
            }
        }
        return "未知状态(" + code + ")";
    }
}