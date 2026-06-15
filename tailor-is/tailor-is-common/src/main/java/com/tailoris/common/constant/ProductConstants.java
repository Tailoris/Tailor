package com.tailoris.common.constant;

public final class ProductConstants {

    private ProductConstants() {
    }

    public static final int PRODUCT_STATUS_OFF_SHELF = 0;
    public static final int PRODUCT_STATUS_ON_SHELF = 1;
    public static final int PRODUCT_STATUS_DELETED = 2;

    public static final int PRODUCT_TYPE_PHYSICAL = 1;
    public static final int PRODUCT_TYPE_VIRTUAL = 2;
    public static final int PRODUCT_TYPE_SERVICE = 3;

    public static final int STOCK_STATUS_IN_STOCK = 1;
    public static final int STOCK_STATUS_LOW_STOCK = 2;
    public static final int STOCK_STATUS_OUT_OF_STOCK = 0;

    public static final int LOW_STOCK_THRESHOLD = 10;

    public static final String PRODUCT_IMAGE_TYPE_MAIN = "main";
    public static final String PRODUCT_IMAGE_TYPE_GALLERY = "gallery";
    public static final String PRODUCT_IMAGE_TYPE_DETAIL = "detail";

    public static final String SKU_STATUS_ENABLE = "enable";
    public static final String SKU_STATUS_DISABLE = "disable";

    public static final int RECOMMEND_NO = 0;
    public static final int RECOMMEND_YES = 1;

    public static final int REVIEW_STATUS_PENDING = 0;
    public static final int REVIEW_STATUS_APPROVED = 1;
    public static final int REVIEW_STATUS_REJECTED = 2;

    public static final int REVIEW_STAR_MIN = 1;
    public static final int REVIEW_STAR_MAX = 5;
}