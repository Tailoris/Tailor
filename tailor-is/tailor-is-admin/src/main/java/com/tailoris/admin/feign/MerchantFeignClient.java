package com.tailoris.admin.feign;

import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import com.tailoris.api.merchant.entity.Merchant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "tailor-is-merchant")
public interface MerchantFeignClient {

    @GetMapping("/api/v1/merchant/list")
    Result<PageResponse<Merchant>> listMerchants(@RequestParam Map<String, Object> params);

    @GetMapping("/api/v1/merchant/info/{id}")
    Result<Merchant> getMerchantById(@PathVariable Long id);

    @PutMapping("/api/v1/merchant/audit")
    Result<Void> auditMerchant(@RequestBody Map<String, Object> request);
}