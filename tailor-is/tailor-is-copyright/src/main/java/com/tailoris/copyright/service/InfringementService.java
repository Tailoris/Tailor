package com.tailoris.copyright.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.copyright.dto.ArbitrationRequest;
import com.tailoris.copyright.dto.InfringementReportRequest;
import com.tailoris.copyright.entity.ArbitrationRecord;
import com.tailoris.copyright.entity.InfringementRecord;

public interface InfringementService {

    InfringementRecord reportInfringement(Long reporterId, InfringementReportRequest request);

    void processReport(Long infringementId, Long handlerId, Integer status, String handlerRemark, Integer punishmentType, String punishmentDetail);

    ArbitrationRecord createArbitration(Long infringementId, ArbitrationRequest request);

    PageResponse<InfringementRecord> listInfringements(Long reporterId, PageRequest pageRequest, Integer status);

    void completeArbitration(Long arbitrationId, Integer result, String resultDescription);
}
