package com.tailoris.admin.feign;

import com.tailoris.admin.dto.PatternAuditRequest;
import com.tailoris.admin.dto.PatternQueryRequest;
import com.tailoris.admin.dto.PatternScoreRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "tailor-is-pattern")
public interface PatternFeignClient {

    @GetMapping("/api/v1/pattern")
    Result<PageResponse<Object>> getPatternList(@RequestBody PatternQueryRequest request);

    @GetMapping("/api/v1/pattern/{id}")
    Result<Object> getPatternById(@PathVariable Long id);

    @PutMapping("/api/v1/pattern/{id}/audit")
    Result<Void> auditPattern(@PathVariable Long id, @RequestBody PatternAuditRequest request);

    @PutMapping("/api/v1/pattern/{id}/score")
    Result<Void> updatePatternScore(@PathVariable Long id, @RequestBody PatternScoreRequest request);

    @DeleteMapping("/api/v1/pattern/{id}")
    Result<Void> deletePattern(@PathVariable Long id);
}