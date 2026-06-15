package com.tailoris.product.constant;

/**
 * 商品类型常量 - PRD-008.
 *
 * <p>三种商品类型的差异化处理：</p>
 * <ul>
 *   <li>PHYSICAL (1) 实物商品 - 传统电商模式，需物流发货、库存管理、SKU 规格</li>
 *   <li>DIGITAL_PATTERN (2) 数字纸样 - 服装样板文件，下载即用，无物流</li>
 *   <li>CUSTOM (3) 定制商品 - 用户可定制尺寸/款式，下单后生产</li>
 * </ul>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public final class ProductTypeConstants {

    private ProductTypeConstants() {}

    /** 实物商品 */
    public static final int PHYSICAL = 1;
    /** 数字纸样（虚拟商品） */
    public static final int DIGITAL_PATTERN = 2;
    /** 定制商品 */
    public static final int CUSTOM = 3;

    /**
     * 校验商品类型.
     */
    public static boolean isValid(Integer type) {
        return type != null && type >= PHYSICAL && type <= CUSTOM;
    }

    /**
     * 是否需要物流配送.
     */
    public static boolean needsShipping(Integer type) {
        return type == null || type == PHYSICAL || type == CUSTOM;
    }

    /**
     * 是否需要库存管理.
     */
    public static boolean needsStock(Integer type) {
        return type == null || type == PHYSICAL;
    }

    /**
     * 是否可生成下载文件.
     */
    public static boolean isDownloadable(Integer type) {
        return type != null && type == DIGITAL_PATTERN;
    }

    /**
     * 是否需要定制参数采集.
     */
    public static boolean needsCustomization(Integer type) {
        return type != null && type == CUSTOM;
    }
}
