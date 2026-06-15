package com.tailoris.merchant.constant;

public class MerchantConstants {

    public static final int MERCHANT_TYPE_INDIVIDUAL = 1;
    public static final int MERCHANT_TYPE_ENTERPRISE = 2;
    public static final int MERCHANT_TYPE_BUSINESS = 3;

    public static final int MERCHANT_STATUS_PENDING = 0;
    public static final int MERCHANT_STATUS_NORMAL = 1;
    public static final int MERCHANT_STATUS_FROZEN = 2;
    public static final int MERCHANT_STATUS_CANCELLED = 3;

    public static final int MERCHANT_AUDIT_STATUS_PENDING = 0;
    public static final int MERCHANT_AUDIT_STATUS_REVIEWING = 1;
    public static final int MERCHANT_AUDIT_STATUS_APPROVED = 2;
    public static final int MERCHANT_AUDIT_STATUS_REJECTED = 3;

    public static final int SHOP_STATUS_DECORATING = 0;
    public static final int SHOP_STATUS_OPEN = 1;
    public static final int SHOP_STATUS_SUSPENDED = 2;
    public static final int SHOP_STATUS_CLOSED = 3;

    public static final int EMPLOYEE_ROLE_MANAGER = 1;
    public static final int EMPLOYEE_ROLE_OPERATOR = 2;
    public static final int EMPLOYEE_ROLE_CUSTOMER_SERVICE = 3;
    public static final int EMPLOYEE_ROLE_WAREHOUSE = 4;
    public static final int EMPLOYEE_ROLE_FINANCE = 5;

    public static final int EMPLOYEE_STATUS_DISABLED = 0;
    public static final int EMPLOYEE_STATUS_NORMAL = 1;

    public static final int CERT_TYPE_BUSINESS_LICENSE = 1;
    public static final int CERT_TYPE_TAX_REGISTRATION = 2;
    public static final int CERT_TYPE_ORGANIZATION_CODE = 3;
    public static final int CERT_TYPE_FOOD_LICENSE = 4;
    public static final int CERT_TYPE_OTHER = 5;

    public static final int QUALIFICATION_AUDIT_STATUS_PENDING = 0;
    public static final int QUALIFICATION_AUDIT_STATUS_REVIEWING = 1;
    public static final int QUALIFICATION_AUDIT_STATUS_APPROVED = 2;
    public static final int QUALIFICATION_AUDIT_STATUS_REJECTED = 3;

    public static final String REDIS_KEY_MERCHANT_INFO = "merchant:info:";
    public static final String REDIS_KEY_SHOP_INFO = "merchant:shop:";
    public static final long REDIS_EXPIRE_MINUTES = 30;
}
