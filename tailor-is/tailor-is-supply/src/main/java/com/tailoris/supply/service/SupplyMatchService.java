package com.tailoris.supply.service;

import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.supply.dto.MatchQueryRequest;
import com.tailoris.supply.entity.SupplyDemandPost;
import com.tailoris.supply.entity.SupplyMatchRecord;

public interface SupplyMatchService {

    PageResponse<SupplyDemandPost> findMatches(Long userId, MatchQueryRequest request, PageRequest pageRequest);

    PageResponse<SupplyDemandPost> recommendSuppliers(Long userId, String city, PageRequest pageRequest);

    SupplyMatchRecord saveMatchRecord(Long demandPostId, Long supplyPostId, Integer matchScore, String matchReason, Long initiatorId);
}
