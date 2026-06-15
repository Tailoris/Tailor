package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.merchant.constant.MerchantConstants;
import com.tailoris.merchant.dto.EmployeeRequest;
import com.tailoris.merchant.entity.MerchantEmployee;
import com.tailoris.merchant.mapper.MerchantEmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("商家员工服务单元测试")
@ExtendWith(MockitoExtension.class)
class MerchantEmployeeServiceImplTest {

    @Mock
    private MerchantEmployeeMapper merchantEmployeeMapper;

    @InjectMocks
    private MerchantEmployeeServiceImpl merchantEmployeeService;

    private MerchantEmployee employee;

    @BeforeEach
    void setUp() {
        employee = new MerchantEmployee();
        employee.setId(1L);
        employee.setMerchantId(100L);
        employee.setUserId(1000L);
        employee.setEmployeeName("张三");
        employee.setEmployeePhone("13800138000");
        employee.setRole(MerchantConstants.EMPLOYEE_ROLE_OPERATOR);
        employee.setPermissions("product:create,product:update");
        employee.setStatus(MerchantConstants.EMPLOYEE_STATUS_NORMAL);
    }

    @Test
    @DisplayName("添加员工：已是员工应抛异常")
    void testAddEmployee_AlreadyExists() {
        EmployeeRequest request = new EmployeeRequest();
        request.setUserId(1000L);
        request.setEmployeeName("张三");

        when(merchantEmployeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);

        assertThrows(BusinessException.class, () -> merchantEmployeeService.addEmployee(100L, request));
    }

    @Test
    @DisplayName("添加员工：成功添加")
    void testAddEmployee_Success() {
        EmployeeRequest request = new EmployeeRequest();
        request.setUserId(1001L);
        request.setEmployeeName("李四");
        request.setEmployeePhone("13800138001");
        request.setRole(MerchantConstants.EMPLOYEE_ROLE_OPERATOR);

        when(merchantEmployeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(merchantEmployeeMapper.insert(any(MerchantEmployee.class))).thenReturn(1);

        MerchantEmployee result = merchantEmployeeService.addEmployee(100L, request);

        assertNotNull(result);
        assertEquals(100L, result.getMerchantId());
        assertEquals(1001L, result.getUserId());
        verify(merchantEmployeeMapper).insert(any(MerchantEmployee.class));
    }

    @Test
    @DisplayName("移除员工：员工不存在应抛异常")
    void testRemoveEmployee_NotFound() {
        when(merchantEmployeeMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> merchantEmployeeService.removeEmployee(100L, 999L));
    }

    @Test
    @DisplayName("移除员工：无权操作应抛异常")
    void testRemoveEmployee_Unauthorized() {
        employee.setMerchantId(200L);
        when(merchantEmployeeMapper.selectById(1L)).thenReturn(employee);

        assertThrows(BusinessException.class, () -> merchantEmployeeService.removeEmployee(100L, 1L));
    }

    @Test
    @DisplayName("移除员工：成功移除")
    void testRemoveEmployee_Success() {
        when(merchantEmployeeMapper.selectById(1L)).thenReturn(employee);
        when(merchantEmployeeMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> merchantEmployeeService.removeEmployee(100L, 1L));
        verify(merchantEmployeeMapper).deleteById(1L);
    }

    @Test
    @DisplayName("检查权限：员工不存在返回false")
    void testCheckPermission_EmployeeNotFound() {
        when(merchantEmployeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        boolean result = merchantEmployeeService.checkPermission(100L, 1000L, "product:create");

        assertFalse(result);
    }

    @Test
    @DisplayName("检查权限：管理员拥有所有权限")
    void testCheckPermission_ManagerHasAllPermissions() {
        employee.setRole(MerchantConstants.EMPLOYEE_ROLE_MANAGER);
        when(merchantEmployeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);

        boolean result = merchantEmployeeService.checkPermission(100L, 1000L, "any:permission");

        assertTrue(result);
    }

    @Test
    @DisplayName("检查权限：普通员工有指定权限")
    void testCheckPermission_HasPermission() {
        employee.setRole(MerchantConstants.EMPLOYEE_ROLE_OPERATOR);
        employee.setPermissions("product:create,product:update");
        when(merchantEmployeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);

        boolean result = merchantEmployeeService.checkPermission(100L, 1000L, "product:create");

        assertTrue(result);
    }

    @Test
    @DisplayName("检查权限：普通员工无指定权限")
    void testCheckPermission_NoPermission() {
        employee.setRole(MerchantConstants.EMPLOYEE_ROLE_OPERATOR);
        employee.setPermissions("product:create,product:update");
        when(merchantEmployeeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(employee);

        boolean result = merchantEmployeeService.checkPermission(100L, 1000L, "product:delete");

        assertFalse(result);
    }

    @Test
    @DisplayName("列出员工：按店铺筛选")
    void testListEmployees_WithShopFilter() {
        when(merchantEmployeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(employee));

        List<MerchantEmployee> result = merchantEmployeeService.listEmployees(100L, 1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("列出员工：不按店铺筛选")
    void testListEmployees_WithoutShopFilter() {
        when(merchantEmployeeMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(employee));

        List<MerchantEmployee> result = merchantEmployeeService.listEmployees(100L, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
