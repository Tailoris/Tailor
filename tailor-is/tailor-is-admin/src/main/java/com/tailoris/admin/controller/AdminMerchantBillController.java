package com.tailoris.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.tailoris.common.constant.RoutePortConstant;
import com.tailoris.admin.service.AdminMerchantBillService;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SaCheckRole({"SUPER_ADMIN", "PLATFORM_ADMIN"})
@Tag(name = "平台商家管理-账单记录")
@RestController
@RequestMapping(RoutePortConstant.ROUTE_ADMIN_MERCHANT_BILL)
@RequiredArgsConstructor
public class AdminMerchantBillController {

    private final AdminMerchantBillService adminMerchantBillService;

    @SaCheckPermission("merchant:bill:view")
    @Operation(summary = "分页查询账单记录（billType: settlement/withdraw/deposit_flow/transaction）")
    @GetMapping("/page")
    public Result<PageResponse<Map<String, Object>>> page(
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String billType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(adminMerchantBillService.page(merchantId, billType, startDate, endDate, current, size));
    }
}
