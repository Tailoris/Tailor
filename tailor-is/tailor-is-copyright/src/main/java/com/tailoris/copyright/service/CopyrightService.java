package com.tailoris.copyright.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.copyright.dto.AuthorizationRequest;
import com.tailoris.copyright.dto.CopyrightRegisterRequest;
import com.tailoris.copyright.dto.CopyrightRegisterResponse;
import com.tailoris.copyright.dto.CopyrightVerifyRequest;
import com.tailoris.copyright.dto.CopyrightVerifyResponse;
import com.tailoris.copyright.entity.CrCertificateFile;
import com.tailoris.copyright.entity.CopyrightAuthorization;
import com.tailoris.copyright.entity.CopyrightRecord;

public interface CopyrightService {

    CopyrightRecord registerCopyright(Long userId, CopyrightRegisterRequest request);

    CopyrightRegisterResponse register(CopyrightRegisterRequest request);

    String generateCertificate(Long copyrightId);

    String verifyCopyright(Long copyrightId);

    CopyrightVerifyResponse verify(CopyrightVerifyRequest request);

    PageResponse<CopyrightRecord> listCopyrights(Long userId, PageRequest pageRequest, Integer status);

    CopyrightRecord getCopyrightDetail(Long copyrightId);

    CopyrightAuthorization authorize(Long userId, AuthorizationRequest request);

    CrCertificateFile getCertificateFile(Long recordId);

    CrCertificateFile getCertificateByCertNo(String certNo);
}
