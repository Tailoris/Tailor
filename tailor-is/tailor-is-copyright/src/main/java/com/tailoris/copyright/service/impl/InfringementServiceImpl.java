package com.tailoris.copyright.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tailoris.common.dto.PageRequest;
import com.tailoris.common.dto.PageResponse;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.copyright.dto.ArbitrationRequest;
import com.tailoris.copyright.dto.InfringementReportRequest;
import com.tailoris.copyright.entity.ArbitrationRecord;
import com.tailoris.copyright.entity.InfringementRecord;
import com.tailoris.copyright.mapper.ArbitrationRecordMapper;
import com.tailoris.copyright.mapper.InfringementRecordMapper;
import com.tailoris.copyright.service.InfringementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InfringementServiceImpl implements InfringementService {

    private final InfringementRecordMapper infringementRecordMapper;
    private final ArbitrationRecordMapper arbitrationRecordMapper;

    @Override
    @Transactional
    public InfringementRecord reportInfringement(Long reporterId, InfringementReportRequest request) {
        InfringementRecord record = new InfringementRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setReportNo("RPT" + SnowflakeIdGenerator.getInstance().nextId());
        record.setReporterId(reporterId);
        record.setCopyrightId(request.getCopyrightId());
        record.setReportedProductId(request.getReportedProductId());
        record.setReportedUserId(request.getReportedUserId());
        record.setReportedShopId(request.getReportedShopId());
        record.setReportedMerchantId(request.getReportedMerchantId());
        record.setInfringementType(request.getInfringementType());
        record.setReason(request.getReason());
        record.setDescription(request.getDescription());
        record.setEvidenceImages(request.getEvidenceImages());
        record.setEvidenceUrls(request.getEvidenceUrls());
        record.setComparisonDescription(request.getComparisonDescription());
        record.setStatus(0);
        record.setUrgency(request.getUrgency() != null ? request.getUrgency() : 1);
        infringementRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public void processReport(Long infringementId, Long handlerId, Integer status, String handlerRemark, Integer punishmentType, String punishmentDetail) {
        InfringementRecord record = infringementRecordMapper.selectById(infringementId);
        if (record == null) {
            throw new BusinessException("举报记录不存在");
        }
        record.setStatus(status);
        record.setHandlerId(handlerId);
        record.setHandlerRemark(handlerRemark);
        record.setHandledAt(LocalDateTime.now());
        if (punishmentType != null) {
            record.setPunishmentType(punishmentType);
            record.setPunishmentDetail(punishmentDetail);
        }
        infringementRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public ArbitrationRecord createArbitration(Long infringementId, ArbitrationRequest request) {
        InfringementRecord report = infringementRecordMapper.selectById(infringementId);
        if (report == null) {
            throw new BusinessException("举报记录不存在");
        }

        ArbitrationRecord arbitration = new ArbitrationRecord();
        arbitration.setId(SnowflakeIdGenerator.getInstance().nextId());
        arbitration.setArbitrationNo("ARB" + SnowflakeIdGenerator.getInstance().nextId());
        arbitration.setInfringementId(infringementId);
        arbitration.setReportNo(report.getReportNo());
        arbitration.setArbitratorId(request.getArbitratorId());
        arbitration.setArbitratorName(request.getArbitratorName());
        arbitration.setArbitratorType(request.getArbitratorType());
        arbitration.setResult(0);
        arbitrationRecordMapper.insert(arbitration);

        report.setStatus(6);
        infringementRecordMapper.updateById(report);

        return arbitration;
    }

    @Override
    public PageResponse<InfringementRecord> listInfringements(Long reporterId, PageRequest pageRequest, Integer status) {
        Page<InfringementRecord> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<InfringementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InfringementRecord::getReporterId, reporterId);
        if (status != null) {
            wrapper.eq(InfringementRecord::getStatus, status);
        }
        wrapper.orderByDesc(InfringementRecord::getCreateTime);
        Page<InfringementRecord> result = infringementRecordMapper.selectPage(page, wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), pageRequest.getPageNum(), pageRequest.getPageSize());
    }

    @Override
    @Transactional
    public void completeArbitration(Long arbitrationId, Integer result, String resultDescription) {
        ArbitrationRecord arbitration = arbitrationRecordMapper.selectById(arbitrationId);
        if (arbitration == null) {
            throw new BusinessException("仲裁记录不存在");
        }
        arbitration.setResult(result);
        arbitration.setResultDescription(resultDescription);
        arbitration.setClosedAt(LocalDateTime.now());
        arbitrationRecordMapper.updateById(arbitration);

        InfringementRecord report = new InfringementRecord();
        report.setId(arbitration.getInfringementId());
        report.setStatus(7);
        report.setHandlerRemark("仲裁完成：" + resultDescription);
        report.setHandledAt(LocalDateTime.now());
        infringementRecordMapper.updateById(report);
    }
}
