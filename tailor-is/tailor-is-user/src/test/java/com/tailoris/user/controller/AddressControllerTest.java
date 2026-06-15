package com.tailoris.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.result.Result;
import com.tailoris.user.dto.AddressRequest;
import com.tailoris.user.entity.UserAddress;
import com.tailoris.user.service.UserAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("地址管理控制器单元测试")
@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private UserAddressService userAddressService;

    @InjectMocks
    private AddressController addressController;

    private UserAddress address;

    @BeforeEach
    void setUp() {
        address = new UserAddress();
        address.setId(1L);
        address.setUserId(100L);
        address.setName("张三");
        address.setPhone("13800138000");
        address.setProvince("浙江省");
        address.setCity("杭州市");
        address.setDistrict("西湖区");
        address.setDetail("文三路 123 号");
        address.setIsDefault(1);
    }

    @Test
    @DisplayName("获取地址列表：成功返回")
    void testListAddresses_Success() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(userAddressService.listByUserId(100L)).thenReturn(Arrays.asList(address));

            Result<List<UserAddress>> result = addressController.listAddresses();

            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(1, result.getData().size());
        }
    }

    @Test
    @DisplayName("获取默认地址：成功返回")
    void testGetDefaultAddress_Success() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(userAddressService.getDefaultAddress(100L)).thenReturn(address);

            Result<UserAddress> result = addressController.getDefaultAddress();

            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(1L, result.getData().getId());
        }
    }

    @Test
    @DisplayName("创建地址：成功创建")
    void testCreateAddress_Success() {
        AddressRequest request = new AddressRequest();
        request.setName("李四");
        request.setPhone("13800138001");
        request.setDetail("新地址");

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(userAddressService.create(eq(100L), any(AddressRequest.class))).thenReturn(2L);

            Result<Long> result = addressController.createAddress(request);

            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals(2L, result.getData());
        }
    }

    @Test
    @DisplayName("更新地址：成功更新")
    void testUpdateAddress_Success() {
        AddressRequest request = new AddressRequest();
        request.setName("王五");

        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            doNothing().when(userAddressService).update(eq(100L), eq(1L), any(AddressRequest.class));

            Result<Void> result = addressController.updateAddress(1L, request);

            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    @Test
    @DisplayName("删除地址：成功删除")
    void testDeleteAddress_Success() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            doNothing().when(userAddressService).delete(100L, 1L);

            Result<Void> result = addressController.deleteAddress(1L);

            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }

    @Test
    @DisplayName("设为默认地址：成功设置")
    void testSetDefaultAddress_Success() {
        try (MockedStatic<StpUtil> stpUtilMock = mockStatic(StpUtil.class)) {
            stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            doNothing().when(userAddressService).setDefault(100L, 1L);

            Result<Void> result = addressController.setDefaultAddress(1L);

            assertNotNull(result);
            assertEquals(200, result.getCode());
        }
    }
}
