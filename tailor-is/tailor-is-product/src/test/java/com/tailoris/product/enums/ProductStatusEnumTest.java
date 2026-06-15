package com.tailoris.product.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品状态枚举测试 - 覆盖 B-M38
 *
 * @author Tailor IS Team
 */
@DisplayName("ProductStatusEnum 测试")
class ProductStatusEnumTest {

    @Test
    @DisplayName("根据code获取枚举")
    void shouldFindByCode() {
        assertEquals(ProductStatusEnum.OFF_SHELF, ProductStatusEnum.of(0));
        assertEquals(ProductStatusEnum.ON_SHELF, ProductStatusEnum.of(1));
        assertEquals(ProductStatusEnum.DRAFT, ProductStatusEnum.of(2));
    }

    @Test
    @DisplayName("null返回null")
    void shouldReturnNullForNull() {
        assertNull(ProductStatusEnum.of(null));
    }

    @Test
    @DisplayName("无效code返回null")
    void shouldReturnNullForInvalidCode() {
        assertNull(ProductStatusEnum.of(999));
    }

    @Test
    @DisplayName("isSellable 仅ON_SHELF可销售")
    void shouldCheckSellable() {
        assertFalse(ProductStatusEnum.OFF_SHELF.isSellable());
        assertTrue(ProductStatusEnum.ON_SHELF.isSellable());
        assertFalse(ProductStatusEnum.DRAFT.isSellable());
        assertFalse(ProductStatusEnum.VIOLATED_OFF_SHELF.isSellable());
    }

    @Test
    @DisplayName("枚举值数量正确")
    void shouldHaveCorrectCount() {
        assertEquals(4, ProductStatusEnum.values().length);
    }
}
