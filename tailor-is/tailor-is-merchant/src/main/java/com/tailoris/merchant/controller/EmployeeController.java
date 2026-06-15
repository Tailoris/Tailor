package com.tailoris.merchant.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.tailoris.common.result.Result;
import com.tailoris.merchant.dto.EmployeeRequest;
import com.tailoris.merchant.entity.MerchantEmployee;
import com.tailoris.merchant.service.MerchantEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SaCheckLogin
@Tag(name = "员工管理")
@RestController
@RequestMapping("/api/merchant/employee")
@RequiredArgsConstructor
public class EmployeeController {

    private final MerchantEmployeeService merchantEmployeeService;

    @Operation(summary = "添加员工")
    @PostMapping
    public Result<MerchantEmployee> addEmployee(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @Valid @RequestBody EmployeeRequest request) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        MerchantEmployee employee = merchantEmployeeService.addEmployee(merchantId, request);
        return Result.success(employee);
    }

    @Operation(summary = "移除员工")
    @DeleteMapping("/{id}")
    public Result<Void> removeEmployee(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @PathVariable Long id) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        merchantEmployeeService.removeEmployee(merchantId, id);
        return Result.success();
    }

    @Operation(summary = "查询员工列表")
    @GetMapping("/list/{shopId}")
    public Result<List<MerchantEmployee>> listEmployees(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @PathVariable Long shopId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        List<MerchantEmployee> employees = merchantEmployeeService.listEmployees(merchantId, shopId);
        return Result.success(employees);
    }

    @Operation(summary = "查询商家员工列表（所有店铺）")
    @GetMapping("/list")
    public Result<List<MerchantEmployee>> listAllEmployees(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        List<MerchantEmployee> employees = merchantEmployeeService.listEmployees(merchantId, null);
        return Result.success(employees);
    }

    @Operation(summary = "检查员工权限")
    @GetMapping("/permission")
    public Result<Boolean> checkPermission(
            @RequestHeader(value = "X-Merchant-Id", required = false) Long merchantId,
            @RequestParam Long userId,
            @RequestParam String permission) {
        if (merchantId == null) {
            return Result.fail("商家未登录");
        }
        boolean hasPermission = merchantEmployeeService.checkPermission(merchantId, userId, permission);
        return Result.success(hasPermission);
    }
}
