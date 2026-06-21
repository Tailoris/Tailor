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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    private final ObjectMapper objectMapper;

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

        saveVersion(userId, record.getId(), "初始版本", "AI自动生成");

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

        saveVersion(userId, record.getId(), "V" + record.getVersion(), request.getChangeReason());

        return iteration;
    }

    @Override
    @Transactional
    public PatternVersion saveVersion(Long userId, Long patternId, String versionName, String changeDescription) {
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null) {
            throw new BusinessException("版型记录不存在");
        }
        // BE-C-7: IDOR越权修复 - 校验版型所属权
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该版型");
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
    public List<PatternVersion> listVersions(Long userId, Long patternId) {
        // BE-C-7: IDOR越权修复 - 校验版型所属权
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("版型记录不存在");
        }
        LambdaQueryWrapper<PatternVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatternVersion::getPatternId, patternId);
        wrapper.orderByDesc(PatternVersion::getVersionNo);
        return patternVersionMapper.selectList(wrapper);
    }

    @Override
    public String exportPattern(Long userId, Long patternId, String format) {
        // BE-C-7: IDOR越权修复 - 校验版型所属权
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("版型记录不存在");
        }
        return "https://pattern-export.example.com/" + patternId + "." + (format != null ? format.toLowerCase() : "svg");
    }

    @Override
    public PatternRecord getPatternDetail(Long userId, Long patternId) {
        // BE-C-7: IDOR越权修复 - 校验版型所属权
        PatternRecord record = patternRecordMapper.selectById(patternId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException("版型记录不存在");
        }
        return record;
    }

    @Override
    public List<PatternRecord> listUserPatterns(Long userId) {
        LambdaQueryWrapper<PatternRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatternRecord::getUserId, userId);
        wrapper.orderByDesc(PatternRecord::getCreateTime);
        return patternRecordMapper.selectList(wrapper);
    }

    private String generatePatternData(BodySizeData bodySize, Integer patternType, String parameters) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", patternType == null ? null : patternType.toString());
        ObjectNode measurements = objectMapper.createObjectNode();
        measurements.put("height", bodySize.getHeight());
        measurements.put("chest", bodySize.getChestCircumference());
        measurements.put("waist", bodySize.getWaistCircumference());
        measurements.put("hip", bodySize.getHipCircumference());
        root.set("measurements", measurements);
        return root.toString();
    }

    private String generatePatternDataByParams(Long bodySizeId, Integer patternType, String parameters) {
        BodySizeData bodySize = bodySizeDataMapper.selectById(bodySizeId);
        if (bodySize == null) {
            throw new BusinessException("体型数据不存在");
        }
        return generatePatternData(bodySize, patternType, parameters);
    }

    /**
     * BE-M-22: 版型结构检查 - 实现真实校验逻辑
     *
     * <p>校验 patternData 的结构完整性与尺寸合理性：</p>
     * <ul>
     *   <li>JSON 格式有效性</li>
     *   <li>必填字段（type、measurements）存在性</li>
     *   <li>尺寸数值合理范围（身高/胸围/腰围/臀围）</li>
     *   <li>尺寸间逻辑关系（如腰围 < 胸围）</li>
     * </ul>
     */
    private String performStructureCheck(String patternData) {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode issues = objectMapper.createArrayNode();
        ArrayNode recommendations = objectMapper.createArrayNode();

        if (patternData == null || patternData.isBlank()) {
            result.put("structure", "invalid");
            issues.add("版型数据为空");
            result.put("confidence", 0.0);
            result.set("issues", issues);
            result.set("recommendations", recommendations);
            return result.toString();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(patternData);
        } catch (Exception e) {
            result.put("structure", "invalid");
            issues.add("版型数据JSON格式无效: " + e.getMessage());
            result.put("confidence", 0.0);
            result.set("issues", issues);
            result.set("recommendations", recommendations);
            return result.toString();
        }

        // 校验 type 字段
        if (!root.has("type") || root.get("type").isNull()) {
            issues.add("缺少版型类型(type)字段");
        }

        // 校验 measurements 字段
        JsonNode measurements = root.get("measurements");
        if (measurements == null || !measurements.isObject()) {
            issues.add("缺少尺寸数据(measurements)字段");
        } else {
            // 校验各项尺寸范围（单位: cm）
            validateMeasurement(measurements, "height", 100, 250, "身高", issues, recommendations);
            validateMeasurement(measurements, "chest", 50, 200, "胸围", issues, recommendations);
            validateMeasurement(measurements, "waist", 40, 200, "腰围", issues, recommendations);
            validateMeasurement(measurements, "hip", 50, 200, "臀围", issues, recommendations);

            // 校验尺寸间逻辑关系
            double chest = measurementValue(measurements, "chest");
            double waist = measurementValue(measurements, "waist");
            double hip = measurementValue(measurements, "hip");
            if (chest > 0 && waist > 0 && waist >= chest) {
                issues.add("腰围(" + waist + ")不应大于等于胸围(" + chest + ")");
            }
            if (hip > 0 && waist > 0 && hip < waist - 30) {
                issues.add("臀围(" + hip + ")与腰围(" + waist + ")差异过大，请核实");
            }
        }

        // 根据问题数量计算置信度与结构判定
        int issueCount = issues.size();
        double confidence;
        String structure;
        if (issueCount == 0) {
            structure = "valid";
            confidence = 0.95;
            recommendations.add("版型结构检查通过，可进入下一步生产");
        } else if (issueCount <= 2) {
            structure = "warning";
            confidence = 0.70;
            recommendations.add("存在少量问题，建议修正后使用");
        } else {
            structure = "invalid";
            confidence = 0.30;
            recommendations.add("存在多处结构问题，请重新生成版型");
        }

        result.put("structure", structure);
        result.put("confidence", confidence);
        result.set("issues", issues);
        result.set("recommendations", recommendations);
        return result.toString();
    }

    /** 校验单个尺寸字段的数值范围 */
    private void validateMeasurement(JsonNode measurements, String field,
                                     double min, double max, String label,
                                     ArrayNode issues, ArrayNode recommendations) {
        JsonNode node = measurements.get(field);
        if (node == null || node.isNull()) {
            issues.add("缺少" + label + "(" + field + ")数据");
            return;
        }
        if (!node.isNumber()) {
            issues.add(label + "(" + field + ")数据类型无效");
            return;
        }
        double value = node.asDouble();
        if (value <= 0) {
            issues.add(label + "(" + value + ")必须为正数");
        } else if (value < min || value > max) {
            issues.add(label + "(" + value + ")超出合理范围[" + min + "-" + max + "cm]");
            recommendations.add("请核实" + label + "数据是否正确");
        }
    }

    /** 安全获取尺寸数值，无效时返回 -1 */
    private double measurementValue(JsonNode measurements, String field) {
        JsonNode node = measurements.get(field);
        return (node != null && node.isNumber()) ? node.asDouble() : -1;
    }
}
