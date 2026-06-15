package com.tailoris.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.merchant.dto.EmployeeRequest;
import com.tailoris.merchant.entity.MerchantEmployee;
import com.tailoris.merchant.mapper.MerchantEmployeeMapper;
import com.tailoris.merchant.service.MerchantEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantEmployeeServiceImpl implements MerchantEmployeeService {

    private final MerchantEmployeeMapper merchantEmployeeMapper;

    @Override
    public MerchantEmployee addEmployee(Long merchantId, EmployeeRequest request) {
        LambdaQueryWrapper<MerchantEmployee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MerchantEmployee::getMerchantId, merchantId)
                .eq(MerchantEmployee::getUserId, request.getUserId());
        MerchantEmployee existing = merchantEmployeeMapper.selectOne(queryWrapper);
        if (existing != null) {
            throw new BusinessException("该用户已是商家员工");
        }

        MerchantEmployee employee = new MerchantEmployee();
        employee.setMerchantId(merchantId);
        employee.setShopId(request.getShopId());
        employee.setUserId(request.getUserId());
        employee.setEmployeeName(request.getEmployeeName());
        employee.setEmployeePhone(request.getEmployeePhone());
        employee.setRole(request.getRole());
        employee.setPermissions(request.getPermissions());
        employee.setStatus(1);

        merchantEmployeeMapper.insert(employee);
        log.info("员工添加成功, merchantId: {}, employeeId: {}, userId: {}", merchantId, employee.getId(), request.getUserId());
        return employee;
    }

    @Override
    public void removeEmployee(Long merchantId, Long employeeId) {
        MerchantEmployee employee = merchantEmployeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        if (!employee.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作该员工");
        }

        merchantEmployeeMapper.deleteById(employeeId);
        log.info("员工移除成功, employeeId: {}", employeeId);
    }

    @Override
    public List<MerchantEmployee> listEmployees(Long merchantId, Long shopId) {
        LambdaQueryWrapper<MerchantEmployee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MerchantEmployee::getMerchantId, merchantId);
        if (shopId != null) {
            queryWrapper.eq(MerchantEmployee::getShopId, shopId);
        }
        queryWrapper.orderByDesc(MerchantEmployee::getCreateTime);
        return merchantEmployeeMapper.selectList(queryWrapper);
    }

    @Override
    public boolean checkPermission(Long merchantId, Long userId, String permission) {
        LambdaQueryWrapper<MerchantEmployee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MerchantEmployee::getMerchantId, merchantId)
                .eq(MerchantEmployee::getUserId, userId)
                .eq(MerchantEmployee::getStatus, 1);
        MerchantEmployee employee = merchantEmployeeMapper.selectOne(queryWrapper);

        if (employee == null) {
            return false;
        }

        if (employee.getRole() != null && employee.getRole() == 1) {
            return true;
        }

        if (StringUtils.hasText(employee.getPermissions())) {
            return employee.getPermissions().contains(permission);
        }

        return false;
    }
}
