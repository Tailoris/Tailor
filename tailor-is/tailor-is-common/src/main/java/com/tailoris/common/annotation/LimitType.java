package com.tailoris.common.annotation;

/**
 * 限流维度
 */
public enum LimitType {
    /** 全局限流（按接口维度） */
    GLOBAL,
    /** IP级别限流 */
    IP,
    /** 用户级别限流（已登录） */
    USER
}
