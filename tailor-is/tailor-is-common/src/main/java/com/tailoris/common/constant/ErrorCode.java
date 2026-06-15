package com.tailoris.common.constant;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "操作成功"),
    SYSTEM_ERROR(1000, "系统内部错误"),
    PARAM_INVALID(1001, "参数校验失败"),
    NOT_FOUND(1002, "资源不存在"),

    UNAUTHORIZED(2000, "未登录或Token已过期"),
    FORBIDDEN(2001, "权限不足"),
    TOKEN_EXPIRED(2002, "Token已过期，请重新登录"),
    TOKEN_INVALID(2003, "Token无效"),

    ORDER_NOT_FOUND(3001, "订单不存在"),
    PRODUCT_NOT_FOUND(3002, "商品不存在"),
    STOCK_INSUFFICIENT(3003, "库存不足"),
    PAYMENT_FAILED(3004, "支付失败"),
    BALANCE_INSUFFICIENT(3005, "余额不足"),
    MERCHANT_NOT_FOUND(3006, "商户不存在"),
    COPYRIGHT_EXISTS(3007, "版权已存在"),
    ;

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}