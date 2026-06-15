package com.tailoris.product.exception;

/**
 * 商品类型不匹配异常 - PRD-008.
 *
 * <p>如：对数字纸样调用了定制商品的接口，或反之。</p>
 *
 * @author Tailor IS Team
 * @since 1.0.0
 */
public class ProductTypeMismatchException extends RuntimeException {

    public ProductTypeMismatchException(String message) {
        super(message);
    }

    public static ProductTypeMismatchException of(int actualType, int expectedType) {
        return new ProductTypeMismatchException(
                String.format("商品类型不匹配: 实际=%d, 期望=%d", actualType, expectedType));
    }
}
