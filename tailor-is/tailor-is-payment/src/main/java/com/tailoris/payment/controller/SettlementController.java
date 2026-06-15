package com.tailoris.payment.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.result.Result;
import com.tailoris.payment.dto.SettlementQueryRequest;
import com.tailoris.payment.entity.SettlementRecord;
import com.tailoris.payment.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@SaCheckLogin
@Tag(name = "商家结算管理", description = "商家结算查询、批量结算等接口")
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @Operation(summary = "查询商家结算列表", description = "分页查询商家的结算记录")
    @GetMapping("/list")
    public Result<PageResponse<SettlementRecord>> listSettlements(SettlementQueryRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long merchantId = StpUtil.getLoginIdAsLong();
        request.setMerchantId(merchantId);
        PageResponse<SettlementRecord> response = settlementService.getMerchantSettlement(merchantId, request);
        return Result.success(response);
    }

    @Operation(summary = "查询结算详情", description = "根据结算单号查询结算详情")
    @GetMapping("/detail")
    public Result<SettlementRecord> getSettlementDetail(@RequestParam String settlementNo) {
        SettlementRecord record = settlementService.getSettlementByNo(settlementNo);
        return Result.success(record);
    }

    @Operation(summary = "批量结算", description = "对指定订单进行批量结算")
    @PostMapping("/batch")
    public Result<Void> batchSettle(@RequestParam java.util.List<Long> orderIds) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException("用户未登录");
        }
        Long merchantId = StpUtil.getLoginIdAsLong();
        settlementService.batchSettle(merchantId, orderIds);
        return Result.success();
    }

    @Operation(summary = "订单结算", description = "订单确认收货后进行结算")
    @PostMapping("/order")
    public Result<Boolean> settleOrder(@RequestParam Long orderId,
                                       @RequestParam Long merchantId,
                                       @RequestParam Long shopId,
                                       @RequestParam BigDecimal orderAmount,
                                       @RequestParam BigDecimal platformFeeRate) {
        settlementService.settleOrder(orderId, merchantId, shopId, orderAmount, platformFeeRate);
        return Result.success(true);
    }
}
