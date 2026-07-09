package com.tailoris.admin.feign;

import com.tailoris.admin.dto.CopyrightAuditRequest;
import com.tailoris.admin.dto.CopyrightQueryRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "tailor-is-copyright")
public interface CopyrightFeignClient {

    @GetMapping("/api/v1/copyright")
    Result<PageResponse<Object>> getCopyrightList(@RequestBody CopyrightQueryRequest request);

    @GetMapping("/api/v1/copyright/{id}")
    Result<Object> getCopyrightById(@PathVariable Long id);

    @PutMapping("/api/v1/copyright/{id}/audit")
    Result<Void> auditCopyright(@PathVariable Long id, @RequestBody CopyrightAuditRequest request);

    @PostMapping("/api/v1/copyright/{id}/detect")
    Result<Void> detectInfringement(@PathVariable Long id);

    @GetMapping("/api/v1/copyright/{id}/evidence")
    Result<PageResponse<Object>> getEvidenceRecords(@PathVariable Long id);
}