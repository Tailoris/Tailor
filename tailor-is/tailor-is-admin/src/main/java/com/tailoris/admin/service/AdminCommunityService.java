package com.tailoris.admin.service;

import com.tailoris.api.admin.dto.ReportProcessRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.api.community.entity.CommunityReport;

public interface AdminCommunityService {

    PageResponse<CommunityReport> listReports(PageRequest request, Integer status);

    void processReport(ReportProcessRequest request, Long adminId);

    void deletePost(Long postId, String reason);

    void auditPost(Long postId, Integer status, String remark, Long adminId);
}
