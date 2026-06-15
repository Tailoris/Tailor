package com.tailoris.order.constant;

public class OrderConstants {

    public static final int ORDER_STATUS_PENDING_PAY = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_SHIPPED = 2;
    public static final int ORDER_STATUS_COMPLETED = 3;
    public static final int ORDER_STATUS_CANCELLED = 4;
    public static final int ORDER_STATUS_REFUNDING = 5;
    public static final int ORDER_STATUS_REFUNDED = 6;
    public static final int ORDER_STATUS_PENDING_DELIVERY = 7;
    public static final int ORDER_STATUS_PENDING_RECEIVE = 8;

    public static final int PAY_STATUS_UNPAID = 0;
    public static final int PAY_STATUS_PAID = 1;
    public static final int PAY_STATUS_REFUNDED = 2;
    public static final int PAY_STATUS_PARTIAL_REFUNDED = 3;

    public static final int TICKET_TYPE_REFUND_ONLY = 1;
    public static final int TICKET_TYPE_RETURN_REFUND = 2;
    public static final int TICKET_TYPE_CUSTOM_REWORK = 3;

    public static final int TICKET_STATUS_PENDING = 0;
    public static final int TICKET_STATUS_PROCESSING = 1;
    public static final int TICKET_STATUS_COMPLETED = 2;
    public static final int TICKET_STATUS_REJECTED = 3;
    public static final int TICKET_STATUS_CLOSED = 4;

    public static final int PRODUCT_TYPE_DIGITAL_PATTERN = 1;
    public static final int PRODUCT_TYPE_CUSTOM_SERVICE = 2;
    public static final int PRODUCT_TYPE_PHYSICAL = 3;

    public static final int LOGISTICS_STATUS_PENDING = 0;
    public static final int LOGISTICS_STATUS_SHIPPED = 1;
    public static final int LOGISTICS_STATUS_IN_TRANSIT = 2;
    public static final int LOGISTICS_STATUS_DELIVERING = 3;
    public static final int LOGISTICS_STATUS_SIGNED = 4;

    public static final String ORDER_NO_PREFIX = "ORD";
    public static final String TICKET_NO_PREFIX = "AS";
    public static final String CART_CACHE_KEY_PREFIX = "cart:";
    public static final long CART_CACHE_EXPIRE_HOURS = 24;
    public static final long ORDER_EXPIRE_MINUTES = 30;

    public static final String ORDER_TIMEOUT_EXCHANGE = "order.timeout.exchange";
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";
}
