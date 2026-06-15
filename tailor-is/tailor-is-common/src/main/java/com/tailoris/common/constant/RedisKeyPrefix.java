package com.tailoris.common.constant;

public final class RedisKeyPrefix {

    private RedisKeyPrefix() {
    }

    public static final String PROJECT = "tailor:";

    public static final String USER = PROJECT + "user:";

    public static final String PRODUCT = PROJECT + "product:";

    public static final String ORDER = PROJECT + "order:";

    public static final String PAYMENT = PROJECT + "payment:";

    public static final String MERCHANT = PROJECT + "merchant:";

    public static final String MARKETING = PROJECT + "marketing:";

    public static final String CART = PROJECT + "cart:";

    public static final String TOKEN = PROJECT + "token:";

    public static final String CACHE = PROJECT + "cache:";

    public static final String LOCK = PROJECT + "lock:";

    public static final String BLOOM = PROJECT + "bloom:";

    public static String user(Long userId) {
        return USER + userId;
    }

    public static String product(Long productId) {
        return PRODUCT + productId;
    }

    public static String order(String orderNo) {
        return ORDER + orderNo;
    }

    public static String token(String loginId) {
        return TOKEN + loginId;
    }

    public static String cache(String module, String key) {
        return CACHE + module + ":" + key;
    }

    public static String lock(String resource) {
        return LOCK + resource;
    }

    public static String bloom(String filterName) {
        return BLOOM + filterName;
    }
}