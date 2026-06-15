package com.tailoris.supply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.supply.dto.MatchQueryRequest;
import com.tailoris.supply.entity.SupplyDemandPost;
import com.tailoris.supply.entity.SupplyMatchRecord;
import com.tailoris.supply.mapper.SupplyDemandPostMapper;
import com.tailoris.supply.mapper.SupplyMatchRecordMapper;
import com.tailoris.supply.service.SupplyMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyMatchServiceImpl implements SupplyMatchService {

    private final SupplyDemandPostMapper supplyDemandPostMapper;
    private final SupplyMatchRecordMapper supplyMatchRecordMapper;

    @Override
    public PageResponse<SupplyDemandPost> findMatches(Long userId, MatchQueryRequest request, PageRequest pageRequest) {
        Page<SupplyDemandPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<SupplyDemandPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplyDemandPost::getStatus, 1);
        if (request.getPostType() != null) {
            wrapper.eq(SupplyDemandPost::getPostType, request.getPostType() == 1 ? 2 : 1);
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(SupplyDemandPost::getCategoryId, request.getCategoryId());
        }
        if (request.getCity() != null) {
            wrapper.eq(SupplyDemandPost::getCity, request.getCity());
        }
        if (request.getMaterialType() != null) {
            wrapper.like(SupplyDemandPost::getMaterialType, request.getMaterialType());
        }
        wrapper.orderByDesc(SupplyDemandPost::getIsTop);
        wrapper.orderByDesc(SupplyDemandPost::getCreateTime);
        Page<SupplyDemandPost> result = supplyDemandPostMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    public PageResponse<SupplyDemandPost> recommendSuppliers(Long userId, String city, PageRequest pageRequest) {
        Page<SupplyDemandPost> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<SupplyDemandPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupplyDemandPost::getStatus, 1);
        wrapper.eq(SupplyDemandPost::getPostType, 1);
        if (city != null) {
            wrapper.eq(SupplyDemandPost::getCity, city);
        }
        wrapper.orderByDesc(SupplyDemandPost::getIsTop);
        wrapper.orderByDesc(SupplyDemandPost::getCreateTime);
        Page<SupplyDemandPost> result = supplyDemandPostMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    @Transactional
    public SupplyMatchRecord saveMatchRecord(Long demandPostId, Long supplyPostId, Integer matchScore, String matchReason, Long initiatorId) {
        SupplyMatchRecord record = new SupplyMatchRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setDemandPostId(demandPostId);
        record.setSupplyPostId(supplyPostId);
        record.setMatchScore(matchScore);
        record.setMatchReason(matchReason);
        record.setStatus(0);
        record.setInitiatorId(initiatorId);
        supplyMatchRecordMapper.insert(record);
        return record;
    }
}
