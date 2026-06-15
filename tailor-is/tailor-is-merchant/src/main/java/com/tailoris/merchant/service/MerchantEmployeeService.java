package com.tailoris.merchant.service;

import com.tailoris.merchant.dto.EmployeeRequest;
import com.tailoris.merchant.entity.MerchantEmployee;

import java.util.List;

public interface MerchantEmployeeService {

    MerchantEmployee addEmployee(Long merchantId, EmployeeRequest request);

    void removeEmployee(Long merchantId, Long employeeId);

    List<MerchantEmployee> listEmployees(Long merchantId, Long shopId);

    boolean checkPermission(Long merchantId, Long userId, String permission);
}
