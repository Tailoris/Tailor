package com.tailoris.admin.constant;

public final class AdminConstants {

    private AdminConstants() {
    }

    public static final int AUDIT_STATUS_PENDING = 0;
    public static final int AUDIT_STATUS_REVIEWING = 1;
    public static final int AUDIT_STATUS_APPROVED = 2;
    public static final int AUDIT_STATUS_REJECTED = 3;

    public static final int MERCHANT_STATUS_PENDING = 0;
    public static final int MERCHANT_STATUS_NORMAL = 1;
    public static final int MERCHANT_STATUS_FROZEN = 2;
    public static final int MERCHANT_STATUS_CANCELLED = 3;

    public static final int USER_STATUS_NORMAL = 0;
    public static final int USER_STATUS_FROZEN = 1;

    public static final int ORDER_STATUS_PENDING_PAY = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_DELIVERED = 2;
    public static final int ORDER_STATUS_COMPLETED = 3;
    public static final int ORDER_STATUS_CANCELLED = 4;
    public static final int ORDER_STATUS_REFUNDING = 5;
    public static final int ORDER_STATUS_REFUNDED = 6;

    public static final int AFTER_SALE_STATUS_PENDING = 0;
    public static final int AFTER_SALE_STATUS_PROCESSING = 1;
    public static final int AFTER_SALE_STATUS_COMPLETED = 2;
    public static final int AFTER_SALE_STATUS_REJECTED = 3;

    public static final int REPORT_STATUS_PENDING = 0;
    public static final int REPORT_STATUS_PROCESSING = 1;
    public static final int REPORT_STATUS_HANDLED = 2;
    public static final int REPORT_STATUS_IGNORED = 3;

    public static final int POST_STATUS_NORMAL = 0;
    public static final int POST_STATUS_DELETED = 1;
    public static final int POST_STATUS_AUDITING = 2;

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    public static final String REDIS_KEY_DASHBOARD_STATS = "tailoris:admin:dashboard:stats";
    public static final long REDIS_DASHBOARD_CACHE_MINUTES = 5;

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_SUPER_ADMIN = "super_admin";
}
