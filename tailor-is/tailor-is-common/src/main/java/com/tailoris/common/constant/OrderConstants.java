package com.tailoris.common.constant;

public final class OrderConstants {

    private OrderConstants() {
    }

    public static final int ORDER_STATUS_PENDING = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_SHIPPED = 2;
    public static final int ORDER_STATUS_RECEIVED = 3;
    public static final int ORDER_STATUS_COMPLETED = 4;
    public static final int ORDER_STATUS_CANCELLED = 5;
    public static final int ORDER_STATUS_REFUNDING = 6;
    public static final int ORDER_STATUS_REFUNDED = 7;

    public static final int PAY_TYPE_ALIPAY = 1;
    public static final int PAY_TYPE_WECHAT = 2;
    public static final int PAY_TYPE_BANK_CARD = 3;
    public static final int PAY_TYPE_CASH = 4;

    public static final int PAY_STATUS_UNPAID = 0;
    public static final int PAY_STATUS_PAYING = 1;
    public static final int PAY_STATUS_PAID = 2;
    public static final int PAY_STATUS_FAILED = 3;
    public static final int PAY_STATUS_REFUNDED = 4;

    public static final int ORDER_TYPE_NORMAL = 0;
    public static final int ORDER_TYPE_GROUP = 1;
    public static final int ORDER_TYPE_SECKILL = 2;
    public static final int ORDER_TYPE_PRESELL = 3;

    public static final String ORDER_NO_PREFIX = "ORD";
    public static final String REFUND_NO_PREFIX = "REF";

    public static final int CANCEL_REASON_USER_REQUEST = 1;
    public static final int CANCEL_REASON_TIMEOUT = 2;
    public static final int CANCEL_REASON_OUT_OF_STOCK = 3;
    public static final int CANCEL_REASON_SYSTEM_ERROR = 4;
}