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

    // ===== 售后相关常量 =====
    /** 售后申请时效（天），超过此时效不可申请售后 */
    public static final int AFTER_SALE_DEADLINE_DAYS = 15;
    /** 售后状态：待处理 */
    public static final int AFTER_SALE_STATUS_PENDING = 0;
    /** 售后状态：已通过 */
    public static final int AFTER_SALE_STATUS_APPROVED = 1;
    /** 售后状态：已拒绝 */
    public static final int AFTER_SALE_STATUS_REJECTED = 2;
    /** 售后状态：已完成 */
    public static final int AFTER_SALE_STATUS_COMPLETED = 3;
    /** 售后类型：退款 */
    public static final int AFTER_SALE_TYPE_REFUND = 1;
    /** 售后类型：退货退款 */
    public static final int AFTER_SALE_TYPE_RETURN_REFUND = 2;
    /** 售后类型：换货 */
    public static final int AFTER_SALE_TYPE_EXCHANGE = 3;

    // ===== 加急订单相关常量 =====
    /** 加急订单标识 */
    public static final int ORDER_URGENT_NO = 0;
    public static final int ORDER_URGENT_YES = 1;
    /** 加急订单费用倍率（1.5倍计费） */
    public static final double URGENT_ORDER_PRICE_MULTIPLIER = 1.5;
    /** 加急订单处理时限（小时） */
    public static final int URGENT_ORDER_PROCESSING_HOURS = 24;

    // ===== 分期付款相关常量 =====
    /** 分期付款比例：首期 33% */
    public static final double INSTALLMENT_RATIO_FIRST = 0.33;
    /** 分期付款比例：二期 33% */
    public static final double INSTALLMENT_RATIO_SECOND = 0.33;
    /** 分期付款比例：三期 34% */
    public static final double INSTALLMENT_RATIO_THIRD = 0.34;
    /** 分期付款总期数 */
    public static final int INSTALLMENT_TOTAL_PERIODS = 3;
}