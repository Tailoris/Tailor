package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.merchant.entity.MerchantCurrentShop;
import com.tailoris.merchant.entity.MerchantEmployee;
import com.tailoris.merchant.entity.MerchantShop;
import com.tailoris.merchant.exception.MerchantBusinessException;
import com.tailoris.merchant.mapper.MerchantCurrentShopMapper;
import com.tailoris.merchant.mapper.MerchantEmployeeMapper;
import com.tailoris.merchant.mapper.MerchantShopMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("商家当前店铺服务测试")
class MerchantCurrentShopServiceImplTest {

    @Mock
    private MerchantCurrentShopMapper currentShopMapper;

    @Mock
    private MerchantEmployeeMapper employeeMapper;

    @Mock
    private MerchantShopMapper shopMapper;

    @InjectMocks
    private MerchantCurrentShopServiceImpl currentShopService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(currentShopService, "baseMapper", currentShopMapper);
    }

    @Test
    @DisplayName("获取当前店铺ID：存在记录")
    void testGetCurrentShopId_Exists() {
        MerchantCurrentShop currentShop = new MerchantCurrentShop();
        currentShop.setCurrentShopId(100L);
        when(currentShopMapper.selectByUserAndMerchant(1L, 10L)).thenReturn(currentShop);

        Long shopId = currentShopService.getCurrentShopId(1L, 10L);

        assertEquals(100L, shopId);
    }

    @Test
    @DisplayName("获取当前店铺ID：不存在记录")
    void testGetCurrentShopId_NotExists() {
        when(currentShopMapper.selectByUserAndMerchant(1L, 10L)).thenReturn(null);

        Long shopId = currentShopService.getCurrentShopId(1L, 10L);

        assertNull(shopId);
    }

    @Test
    @DisplayName("切换店铺：成功-新建记录")
    void testSwitchTo_Success_NewRecord() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setShopIds(null);

        MerchantShop shop = new MerchantShop();
        shop.setId(100L);
        shop.setMerchantId(10L);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectById(100L)).thenReturn(shop);
        when(currentShopMapper.selectByUserAndMerchant(1L, 10L)).thenReturn(null);
        when(currentShopMapper.insert(any(MerchantCurrentShop.class))).thenReturn(1);

        boolean result = currentShopService.switchTo(1L, 10L, 100L);

        assertTrue(result);
        verify(currentShopMapper).insert(any(MerchantCurrentShop.class));
    }

    @Test
    @DisplayName("切换店铺：成功-更新记录")
    void testSwitchTo_Success_UpdateRecord() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setShopIds(null);

        MerchantShop shop = new MerchantShop();
        shop.setId(100L);
        shop.setMerchantId(10L);

        MerchantCurrentShop existing = new MerchantCurrentShop();
        existing.setId(1L);
        existing.setCurrentShopId(50L);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectById(100L)).thenReturn(shop);
        when(currentShopMapper.selectByUserAndMerchant(1L, 10L)).thenReturn(existing);
        when(currentShopMapper.updateById(any(MerchantCurrentShop.class))).thenReturn(1);

        boolean result = currentShopService.switchTo(1L, 10L, 100L);

        assertTrue(result);
        assertEquals(100L, existing.getCurrentShopId());
        verify(currentShopMapper).updateById(any(MerchantCurrentShop.class));
    }

    @Test
    @DisplayName("切换店铺：用户不属于商家")
    void testSwitchTo_UserNotInMerchant() {
        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> currentShopService.switchTo(1L, 10L, 100L));
    }

    @Test
    @DisplayName("切换店铺：店铺不存在")
    void testSwitchTo_ShopNotExists() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectById(100L)).thenReturn(null);

        assertThrows(MerchantBusinessException.class, () -> currentShopService.switchTo(1L, 10L, 100L));
    }

    @Test
    @DisplayName("切换店铺：店铺不属于商家")
    void testSwitchTo_ShopNotInMerchant() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);

        MerchantShop shop = new MerchantShop();
        shop.setId(100L);
        shop.setMerchantId(20L);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectById(100L)).thenReturn(shop);

        assertThrows(MerchantBusinessException.class, () -> currentShopService.switchTo(1L, 10L, 100L));
    }

    @Test
    @DisplayName("切换店铺：无店铺访问权限")
    void testSwitchTo_NoPermission() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setShopIds("50,60");

        MerchantShop shop = new MerchantShop();
        shop.setId(100L);
        shop.setMerchantId(10L);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectById(100L)).thenReturn(shop);

        assertThrows(MerchantBusinessException.class, () -> currentShopService.switchTo(1L, 10L, 100L));
    }

    @Test
    @DisplayName("切换店铺：有店铺访问权限")
    void testSwitchTo_WithPermission() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setShopIds("100,200");

        MerchantShop shop = new MerchantShop();
        shop.setId(100L);
        shop.setMerchantId(10L);

        MerchantCurrentShop existing = new MerchantCurrentShop();
        existing.setId(1L);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectById(100L)).thenReturn(shop);
        when(currentShopMapper.selectByUserAndMerchant(1L, 10L)).thenReturn(existing);
        when(currentShopMapper.updateById(any(MerchantCurrentShop.class))).thenReturn(1);

        boolean result = currentShopService.switchTo(1L, 10L, 100L);

        assertTrue(result);
    }

    @Test
    @DisplayName("获取用户店铺列表：用户不存在")
    void testListUserShops_UserNotExists() {
        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        List<MerchantShop> result = currentShopService.listUserShops(1L, 10L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取用户店铺列表：店长可见全部")
    void testListUserShops_ShopManager() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setRoleCode("shop_manager");

        MerchantShop shop1 = new MerchantShop();
        shop1.setId(100L);
        MerchantShop shop2 = new MerchantShop();
        shop2.setId(200L);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(shop1, shop2));

        List<MerchantShop> result = currentShopService.listUserShops(1L, 10L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("获取用户店铺列表：商家所有者可见全部")
    void testListUserShops_MerchantOwner() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setRoleCode("merchant_owner");

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<MerchantShop> result = currentShopService.listUserShops(1L, 10L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取用户店铺列表：限定店铺")
    void testListUserShops_LimitedShops() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setRoleCode("staff");
        employee.setShopIds("100,200");

        MerchantShop shop1 = new MerchantShop();
        shop1.setId(100L);

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(shop1));

        List<MerchantShop> result = currentShopService.listUserShops(1L, 10L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取用户店铺列表：限定店铺为空")
    void testListUserShops_EmptyShopIds() {
        MerchantEmployee employee = new MerchantEmployee();
        employee.setUserId(1L);
        employee.setMerchantId(10L);
        employee.setStatus(1);
        employee.setRoleCode("staff");
        employee.setShopIds("");

        when(employeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);
        when(shopMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<MerchantShop> result = currentShopService.listUserShops(1L, 10L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("清除当前店铺：存在记录")
    void testClear_Exists() {
        MerchantCurrentShop currentShop = new MerchantCurrentShop();
        currentShop.setId(1L);

        when(currentShopMapper.selectByUserAndMerchant(1L, 10L)).thenReturn(currentShop);
        when(currentShopMapper.deleteById(1L)).thenReturn(1);

        boolean result = currentShopService.clear(1L, 10L);

        assertTrue(result);
    }

    @Test
    @DisplayName("清除当前店铺：不存在记录")
    void testClear_NotExists() {
        when(currentShopMapper.selectByUserAndMerchant(1L, 10L)).thenReturn(null);

        boolean result = currentShopService.clear(1L, 10L);

        assertTrue(result);
    }
}
