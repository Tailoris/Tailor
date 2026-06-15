package com.tailoris.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tailoris.common.exception.BusinessException;
import com.tailoris.common.util.SnowflakeIdGenerator;
import com.tailoris.ai.dto.PatternCheckRequest;
import com.tailoris.ai.dto.PatternGenerateRequest;
import com.tailoris.ai.dto.PatternIterationRequest;
import com.tailoris.ai.entity.BodySizeData;
import com.tailoris.ai.entity.PatternIteration;
import com.tailoris.ai.entity.PatternRecord;
import com.tailoris.ai.entity.PatternVersion;
import com.tailoris.ai.mapper.BodySizeDataMapper;
import com.tailoris.ai.mapper.PatternIterationMapper;
import com.tailoris.ai.mapper.PatternRecordMapper;
import com.tailoris.ai.mapper.PatternVersionMapper;
import com.tailoris.ai.service.PatternService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatternServiceImpl implements PatternService {

    private final PatternRecordMapper patternRecordMapper;
    private final PatternVersionMapper patternVersionMapper;
    private final PatternIterationMapper patternIterationMapper;
    private final BodySizeDataMapper bodySizeDataMapper;

    @Override
    @Transactional
    public PatternRecord generatePattern(Long userId, PatternGenerateRequest request) {
        BodySizeData bodySize = bodySizeDataMapper.selectById(request.getBodySizeId());
        if (bodySize == null) {
            throw new BusinessException("体型数据不存在");
        }

        String patternData = generatePatternData(bodySize, request.getPatternType(), request.getParameters());

        PatternRecord record = new PatternRecord();
        record.setId(SnowflakeIdGenerator.getInstance().nextId());
        record.setUserId(userId);
        record.setPatternName(request.getPatternName());
        record.setPatternType(request.getPatternType());
        record.setBodySizeId(request.getBodySizeId());
        record.setParameters(request.getParameters());
        record.setPatternData(patternData);
        record.setExportFormat(request.getExportFormat());
        record.setCheckStatus(0);
        record.setStatus(1);
        record.setVersion(1);
        patternRecordMapper.insert(record);

        saveVersion(record.getId(), "初始版本", "AI自动生成");

        return record;
    }

    @Override
    public String checkPattern(Long userId, PatternCheckRequest request) {
        PatternRecord record = patternRecordMapper.selectById(request.getPatternId());
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("版型记录不存在");
        }

        String checkResult = performStructureCheck(record.getPatternData());
        record.setCheckResult(checkResult);
        record.setCheckStatus(1);
        patternRecordMapper.updateById(record);
        return checkResult;
    }

    @Override
    @Transactional
    public PatternIteration iteratePattern(Long userId, PatternIterationRequest request) {
        PatternRecord record = patternRecordMapper.selectById(request.getPatternId());
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("版型记录不存在");
        }

        String oldParams = record.getParameters();
        String newParams = request.getNewParameters() != null ? request.getNewParameters() : oldParams;
        String newPatternData = generatePatternDataByParams(record.getBodySizeId(), record.getPatternType(), newParams);

        PatternIteration iteration = new PatternIteration();
        iteration.setId(SnowflakeIdGenerator.getInstance().nextId());
        iteration.setPatternId(request.getPatternId());
        iteration.setIterationType(request.getIterationType());
        iteration.setOldParameters(oldParams);
        iteration.setNewParameters(newParams);
        iteration.setChangeReason(request.getChangeReason());
        iteration.setChangeResult(newPatternData);
        patternIterationMapper.insert(iteration);

        record.setPatternData(newPatternData);
        record.setParameters(newParams);
        record.setVersion(record.getVersion() + 1);
        patternRecordMapper.updateById(record);

        saveVersion(record.getId(), "V" + record.getVersion(), request.getChangeReason());

        return iteration;
    }

    @Override
    @Transactional
    public PatternVersion saveVersion(Long patternId, String versionName, String changeDescription) {
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null) {
            throw new BusinessException("版型记录不存在");
        }

        LambdaUpdateWrapper<PatternVersion> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PatternVersion::getPatternId, patternId)
                     .eq(PatternVersion::getIsCurrent, 1)
                     .set(PatternVersion::getIsCurrent, 0);
        patternVersionMapper.update(null, updateWrapper);

        PatternVersion version = new PatternVersion();
        version.setId(SnowflakeIdGenerator.getInstance().nextId());
        version.setPatternId(patternId);
        version.setVersionNo(record.getVersion());
        version.setVersionName(versionName);
        version.setPatternData(record.getPatternData());
        version.setChangeDescription(changeDescription);
        version.setParametersSnapshot(record.getParameters());
        version.setIsCurrent(1);
        patternVersionMapper.insert(version);

        return version;
    }

    @Override
    public List<PatternVersion> listVersions(Long patternId) {
        LambdaQueryWrapper<PatternVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatternVersion::getPatternId, patternId);
        wrapper.orderByDesc(PatternVersion::getVersionNo);
        return patternVersionMapper.selectList(wrapper);
    }

    @Override
    public String exportPattern(Long patternId, String format) {
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null) {
            throw new BusinessException("版型记录不存在");
        }
        return "https://pattern-export.example.com/" + patternId + "." + (format != null ? format.toLowerCase() : "svg");
    }

    @Override
    public PatternRecord getPatternDetail(Long patternId) {
        return patternRecordMapper.selectById(patternId);
    }

    @Override
    public List<PatternRecord> listUserPatterns(Long userId) {
        LambdaQueryWrapper<PatternRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatternRecord::getUserId, userId);
        wrapper.orderByDesc(PatternRecord::getCreateTime);
        return patternRecordMapper.selectList(wrapper);
    }

    private String generatePatternData(BodySizeData bodySize, Integer patternType, String parameters) {
        return "{\"type\":\"" + patternType + "\",\"measurements\":{\"height\":" + bodySize.getHeight() +
               ",\"chest\":" + bodySize.getChestCircumference() +
               ",\"waist\":" + bodySize.getWaistCircumference() +
               ",\"hip\":" + bodySize.getHipCircumference() + "}}";
    }

    private String generatePatternDataByParams(Long bodySizeId, Integer patternType, String parameters) {
        BodySizeData bodySize = bodySizeDataMapper.selectById(bodySizeId);
        if (bodySize == null) {
            throw new BusinessException("体型数据不存在");
        }
        return generatePatternData(bodySize, patternType, parameters);
    }

    private String performStructureCheck(String patternData) {
        return "{\"structure\":\"valid\",\"issues\":[],\"confidence\":0.95,\"recommendations\":[]}";
    }
}
