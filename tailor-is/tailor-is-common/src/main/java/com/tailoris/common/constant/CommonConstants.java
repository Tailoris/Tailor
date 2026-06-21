package com.tailoris.common.constant;

public final class CommonConstants {

    private CommonConstants() {
    }

    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    public static final int DELETED_NO = 0;
    public static final int DELETED_YES = 1;

    public static final String USER_TYPE_ADMIN = "admin";
    public static final String USER_TYPE_USER = "user";
    public static final String USER_TYPE_MERCHANT = "merchant";

    public static final String GENDER_MALE = "M";
    public static final String GENDER_FEMALE = "F";
    public static final String GENDER_UNKNOWN = "U";

    public static final String YES = "Y";
    public static final String NO = "N";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_TOKEN = "X-Token";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";

    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String CACHE_PREFIX = "tailoris:";
    public static final String CACHE_TOKEN = CACHE_PREFIX + "token:";
    public static final String CACHE_USER = CACHE_PREFIX + "user:";
    public static final String CACHE_CAPTCHA = CACHE_PREFIX + "captcha:";
    public static final String CACHE_RATE_LIMIT = CACHE_PREFIX + "ratelimit:";

    public static final String SESSION_MERCHANT_ID = "merchantId";

    public static final int CAPTCHA_EXPIRE_SECONDS = 300;
    public static final int TOKEN_EXPIRE_SECONDS = 1800;

    public static final int SORT_ASC = 1;
    public static final int SORT_DESC = 0;
}