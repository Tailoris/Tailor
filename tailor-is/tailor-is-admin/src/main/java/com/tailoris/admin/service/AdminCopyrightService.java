package com.tailoris.admin.service;

import com.tailoris.admin.dto.CopyrightAuditRequest;
import com.tailoris.admin.dto.CopyrightQueryRequest;
import com.tailoris.common.dto.PageResponse;

public interface AdminCopyrightService {

    PageResponse<Object> listCopyrights(CopyrightQueryRequest request);

    Object getCopyright(Long id);

    void auditCopyright(Long id, CopyrightAuditRequest request);

    void detectInfringement(Long id);

    PageResponse<Object> getEvidenceRecords(Long id);
}