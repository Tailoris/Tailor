package com.tailoris.merchant.service;

import com.tailoris.merchant.dto.QualificationAuditRequest;
import com.tailoris.merchant.entity.MerchantQualification;

import java.util.List;

public interface MerchantQualificationService {

    MerchantQualification uploadQualification(Long merchantId, MerchantQualification qualification);

    void auditQualification(QualificationAuditRequest request);

    List<MerchantQualification> listQualifications(Long merchantId);
}
