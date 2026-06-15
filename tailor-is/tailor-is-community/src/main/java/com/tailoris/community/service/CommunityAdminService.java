package com.tailoris.community.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.community.dto.ReportRequest;
import com.tailoris.community.entity.CommunityCategory;
import com.tailoris.community.entity.CommunityReport;

public interface CommunityAdminService {

    void auditPost(Long postId, Integer status, Long auditBy, String auditRemark);

    void deletePost(Long postId);

    void createReport(Long reporterId, ReportRequest request);

    void processReport(Long reportId, Long handlerId, Integer status, String handlerRemark, Integer punishmentType);

    PageResponse<CommunityReport> listReports(PageRequest pageRequest, Integer status);

    CommunityReport getReportDetail(Long reportId);
}
