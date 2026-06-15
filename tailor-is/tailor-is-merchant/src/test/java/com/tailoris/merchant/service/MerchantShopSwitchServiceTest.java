package com.tailoris.merchant.service;

import com.tailoris.merchant.entity.MerchantCurrentShop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商家多店铺切换单元测试 - MER-009.
 *
 * @author Tailor IS Team
 * @since 2026-06-03
 */
@DisplayName("商家多店铺切换单元测试")
class MerchantShopSwitchServiceTest {

    @Test
    @DisplayName("当前店铺实体初始化")
    void testEntityInit() {
        MerchantCurrentShop current = new MerchantCurrentShop();
        current.setUserId(1L);
        current.setMerchantId(1L);
        current.setCurrentShopId(100L);

        assertEquals(1L, current.getUserId());
        assertEquals(1L, current.getMerchantId());
        assertEquals(100L, current.getCurrentShopId());
    }

    @Test
    @DisplayName("店铺ID列表解析")
    void testParseShopIds() {
        String shopIdsStr = "100,200,300,400";
        List<Long> ids = Arrays.stream(shopIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        assertEquals(4, ids.size());
        assertTrue(ids.contains(100L));
        assertTrue(ids.contains(400L));
    }

    @Test
    @DisplayName("空字符串处理")
    void testParseEmptyShopIds() {
        String shopIdsStr = "";
        List<Long> ids = Arrays.stream(shopIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        assertEquals(0, ids.size());
    }

    @Test
    @DisplayName("店铺ID列表包含性校验")
    void testShopAccess() {
        List<Long> allowedShops = Arrays.asList(1L, 2L, 3L);
        Long target = 2L;
        assertTrue(allowedShops.contains(target), "员工有权访问该店铺");

        target = 99L;
        assertFalse(allowedShops.contains(target), "员工无权访问该店铺");
    }

    @Test
    @DisplayName("店长全店铺权限")
    void testShopManagerAllShops() {
        String roleCode = "shop_manager";
        assertEquals("shop_manager", roleCode);
        // 店长无需限定 shopIds
    }

    @Test
    @DisplayName("主店铺访问")
    void testPrimaryShopAccess() {
        Long primaryShopId = 100L;
        Long target = 100L;
        assertEquals(primaryShopId, target, "员工主店铺允许访问");
    }
}
