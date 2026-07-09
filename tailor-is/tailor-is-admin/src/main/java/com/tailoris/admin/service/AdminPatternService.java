package com.tailoris.admin.service;

import com.tailoris.admin.dto.PatternAuditRequest;
import com.tailoris.admin.dto.PatternQueryRequest;
import com.tailoris.admin.dto.PatternScoreRequest;
import com.tailoris.common.dto.PageResponse;

public interface AdminPatternService {

    PageResponse<Object> listPatterns(PatternQueryRequest request);

    Object getPattern(Long id);

    void auditPattern(Long id, PatternAuditRequest request);

    void updatePatternScore(Long id, PatternScoreRequest request);

    void deletePattern(Long id);
}