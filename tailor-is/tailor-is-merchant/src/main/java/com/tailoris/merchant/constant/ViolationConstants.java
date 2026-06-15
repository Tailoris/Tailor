package com.tailoris.merchant.constant;

/**
 * 商家违规处罚相关常量 - MER-007.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
public final class ViolationConstants {

    private ViolationConstants() {
    }

    // ========== 违规类型 ==========
    public static final int TYPE_PRODUCT = 1;       // 商品违规
    public static final int TYPE_PRICE = 2;          // 价格违规
    public static final int TYPE_ADVERTISE = 3;      // 虚假宣传
    public static final int TYPE_AFTER_SALE = 4;     // 售后违规
    public static final int TYPE_QUALIFICATION = 5;  // 资质过期
    public static final int TYPE_OTHER = 6;          // 其他

    // ========== 违规级别 ==========
    public static final int LEVEL_MINOR = 1;          // 轻微
    public static final int LEVEL_GENERAL = 2;        // 一般
    public static final int LEVEL_SERIOUS = 3;        // 严重
    public static final int LEVEL_VERY_SERIOUS = 4;   // 特别严重

    // ========== 处罚类型 ==========
    public static final int PUNISH_PENDING = 0;       // 待定
    public static final int PUNISH_WARN = 1;          // 警告
    public static final int PUNISH_LIMIT = 2;         // 限流
    public static final int PUNISH_OFFLINE = 3;       // 下架
    public static final int PUNISH_BAN = 4;           // 封禁
    public static final int PUNISH_EVICT = 5;         // 清退

    // ========== 状态 ==========
    public static final int STATUS_PENDING = 0;        // 待处理
    public static final int STATUS_PUNISHED = 1;      // 已处罚
    public static final int STATUS_APPEALED = 2;       // 已申诉
    public static final int STATUS_REVOKED = 3;        // 已撤销
    public static final int STATUS_RELEASED = 4;       // 已解除

    // ========== 申诉状态 ==========
    public static final int APPEAL_NO = 0;
    public static final int APPEAL_YES = 1;

    // ========== 默认处罚天数 ==========
    public static final int DEFAULT_WARN_DAYS = 0;          // 警告无天数限制
    public static final int DEFAULT_LIMIT_DAYS = 7;         // 限流7天
    public static final int DEFAULT_OFFLINE_DAYS = 15;      // 下架15天
    public static final int DEFAULT_BAN_DAYS = 30;          // 封禁30天
    public static final int DEFAULT_EVICT_DAYS = 0;         // 清退永久

    // ========== 扣分规则 ==========
    public static final int DEDUCT_MINOR = 5;               // 轻微违规扣5分
    public static final int DEDUCT_GENERAL = 15;            // 一般违规扣15分
    public static final int DEDUCT_SERIOUS = 30;            // 严重违规扣30分
    public static final int DEDUCT_VERY_SERIOUS = 60;       // 特别严重扣60分

    /** 违规扣分最高分（满分） */
    public static final int VIOLATION_MAX_SCORE = 100;
}
